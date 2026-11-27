#!/usr/bin/env python3
"""Fetch Radio Browser metadata for reference reconciliation, without changing the app.

Network use is intentionally bounded. A directory listing is not proof of identity,
current playback, programme content, or rights. No station is added automatically.
"""
import concurrent.futures
import json
import pathlib
import sys
import urllib.parse
import urllib.request

MIRRORS = ('https://de1.api.radio-browser.info', 'https://nl1.api.radio-browser.info', 'https://de2.api.radio-browser.info')
USER_AGENT = 'RameshRadio/1.0 reference-station-reconciliation (+https://github.com/universalrp2003/RP-Android-Apps)'


def query(params):
    errors = []
    for base in MIRRORS:
        try:
            request = urllib.request.Request(base + '/json/stations/search?' + urllib.parse.urlencode(params), headers={'User-Agent': USER_AGENT})
            with urllib.request.urlopen(request, timeout=20) as response:
                raw = response.read(5_000_001)
            if len(raw) > 5_000_000:
                raise ValueError('response exceeds size limit')
            result = json.loads(raw)
            if not isinstance(result, list):
                raise ValueError('not a list')
            return result, []
        except Exception as exc:
            errors.append(f'{base}: {type(exc).__name__}')
    return [], errors


def main(out):
    # Also include named artists: some directory entries have no language metadata.
    requests = [dict(language='tamil', limit='1000', offset=str(offset), order='votes', reverse='true') for offset in (0, 1000, 2000)]
    requests += [dict(tag='tamil', limit='1000', offset=str(offset), order='votes', reverse='true') for offset in (0, 1000)]
    requests += [dict(name=name, limit='100') for name in (
        'ilayaraja', 'ilaiyaraaja', 'rahman', 'spb', 'balasubrahmanyam', 'chitra', 'chithra',
        'susheela', 'suseela', 'yesudas', 'mano', 'harris', 'anirudh', 'yuvan', 'vidyasagar',
        'hariharan', 'janaki', 'swarnalatha', 'kannadasan', 'soundararajan', 'viswanathan',
        'vadivelu', 'goundamani', 'vivek', 'santhanam', 'tamil comedy')]
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        results = list(pool.map(query, requests))
    rows = {}; failures = []
    for params, (data, errors) in zip(requests, results):
        if errors:
            failures.append({'query': params, 'errors': errors})
        for station in data:
            key = station.get('stationuuid') or station.get('url_resolved') or station.get('url')
            if key:
                rows[key] = station
    target = pathlib.Path(out); target.mkdir(parents=True, exist_ok=True)
    (target/'directory.json').write_text(json.dumps({'queries': requests, 'failures': failures, 'stations': list(rows.values())}, ensure_ascii=False), encoding='utf8')
    print(f'{len(rows)} unique directory entries; {len(failures)} failed queries')
    if not rows:
        raise SystemExit('No directory data was returned')

if __name__ == '__main__':
    main(sys.argv[1])
