#!/usr/bin/env python3
"""Probe manually selected directory candidates; never edit the app or publish.

The probe confirms audio metadata, not the station's content or mobile availability.
Candidates must be reviewed against the reference, not selected by fuzzy matching.
"""
import concurrent.futures
import datetime
import json
import pathlib
import sys
import subprocess
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
from audit_radio_catalogue import probe


def main(candidates, out):
    rows = json.loads(pathlib.Path(candidates).read_text(encoding='utf8'))
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        results = list(pool.map(probe, rows))
    # Look for broadcaster-provided ICY labels on the artist candidates. This is only
    # supporting evidence; a stream can be renamed or mislabelled by its operator.
    for row, result in zip(rows, results):
        if row['name'] not in ('A. R. Rahman Hits HD', 'SPB Hits HD', 'Mano Hits Radio', 'JP Chandrababu Hits', 'Naga FM', 'Osai FM', 'Viduthalai FM') or result['status'] != 'audio_reachable':
            continue
        try:
            proc = subprocess.run(['ffprobe', '-v', 'error', '-rw_timeout', '5000000',
                '-protocol_whitelist', 'http,https,tcp,tls,crypto,httpproxy',
                '-show_entries', 'format_tags', '-of', 'json', row['url']],
                capture_output=True, text=True, timeout=12)
            result['reportedTags'] = json.loads(proc.stdout or '{}').get('format', {}).get('tags', {})
        except Exception as exc:
            result['reportedTagsError'] = type(exc).__name__
    report = {'checkedAtRunnerUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
              'candidates': rows, 'results': results,
              'limits': 'Single CI-location audio metadata probe; no content, identity, phone, region or licence guarantee. Reported-provider streams remain held for review.'}
    pathlib.Path(out).mkdir(parents=True, exist_ok=True)
    (pathlib.Path(out)/'reference-probe.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf8')
    print('Probed', len(results), 'candidates;', sum(x['status'] == 'audio_reachable' for x in results), 'audio reachable')

if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
