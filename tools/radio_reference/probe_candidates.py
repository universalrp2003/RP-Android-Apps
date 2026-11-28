#!/usr/bin/env python3
"""Probe manually selected directory candidates; never edit the app or publish.

The probe confirms audio metadata, not the station's content or mobile availability.
Candidates must be reviewed against the reference, not selected by fuzzy matching.
"""
import concurrent.futures
import datetime
import json
import pathlib
import subprocess
import sys
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
from audit_radio_catalogue import probe


def probe_with_tags(row):
    result = probe(row)
    if result['status'] != 'audio_reachable' or result['providerReview']:
        return result
    # Broadcaster-provided labels are supporting evidence only; the operator can
    # change them and they do not establish station identity or rights.
    try:
        proc = subprocess.run(['ffprobe', '-v', 'error', '-rw_timeout', '5000000',
            '-protocol_whitelist', 'http,https,tcp,tls,crypto,httpproxy',
            '-show_entries', 'format_tags', '-of', 'json', row['url']],
            capture_output=True, text=True, timeout=12)
        result['reportedTags'] = json.loads(proc.stdout or '{}').get('format', {}).get('tags', {})
    except Exception as exc:
        result['reportedTagsError'] = type(exc).__name__
    return result


def main(candidates, out):
    rows = json.loads(pathlib.Path(candidates).read_text(encoding='utf8'))
    # Keep the load bounded, including when many candidates share a host.
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        results = list(pool.map(probe_with_tags, rows))
    report = {'checkedAtRunnerUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
              'candidates': rows, 'results': results,
              'limits': 'Single CI-location audio metadata probe; no content, identity, phone, region or licence guarantee. Reported-provider streams remain held for review.'}
    pathlib.Path(out).mkdir(parents=True, exist_ok=True)
    (pathlib.Path(out)/'reference-probe.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf8')
    print('Probed', len(results), 'candidates;', sum(x['status'] == 'audio_reachable' for x in results), 'audio reachable')

if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
