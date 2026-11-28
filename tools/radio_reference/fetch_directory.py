#!/usr/bin/env python3
"""Fetch Radio Browser metadata for reference reconciliation, without changing the app.

Network use is intentionally bounded. A directory listing is not proof of identity,
current playback, programme content, or rights. No station is added automatically.
"""
import concurrent.futures
import json
import pathlib
import re
import sys
import time
import urllib.parse
import urllib.request

MIRRORS = ('https://de1.api.radio-browser.info', 'https://nl1.api.radio-browser.info', 'https://de2.api.radio-browser.info')
USER_AGENT = 'RameshRadio/1.0 reference-station-reconciliation (+https://github.com/universalrp2003/RP-Android-Apps)'


def query(params):
    time.sleep(0.5)  # Keep discovery gentle on the community-run directory.
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


def reference_queries(inventory):
    """Choose distinctive ASCII name tokens, avoiding broad searches like radio and tamil.

    These are discovery hints only: every result still requires individual identity review.
    """
    ignored = {'radio', 'tamil', 'song', 'songs', 'hits', 'live', 'city', 'online',
               'music', 'service', 'fm', 'hd', 'old', 'new', 'logo', 'tile', 'cropped',
               'continue', 'three', 'news', 'part', 'stations'}
    terms = set()
    for line in inventory.splitlines():
        if not re.match(r'^\d\d \|', line):
            continue
        for label in line.split(' | ')[1:]:
            label = re.sub(r'\[[^]]*\]', '', label)
            words = [x.lower() for x in re.findall(r'[A-Za-z0-9]+', label)]
            words = [w for w in words if len(w) >= 4 and w not in ignored and not w.isdigit()]
            if words:
                terms.add(max(words, key=len))
    return sorted(terms)


def main(out):
    # Also include named artists: some directory entries have no language metadata.
    requests = [dict(language='tamil', limit='1000', offset=str(offset), order='votes', reverse='true') for offset in (0, 1000, 2000)]
    requests += [dict(tag='tamil', limit='1000', offset=str(offset), order='votes', reverse='true') for offset in (0, 1000)]
    requests += [dict(name=name, limit='100') for name in (
        'ilayaraja', 'ilaiyaraaja', 'rahman', 'spb', 'balasubrahmanyam', 'chitra', 'chithra',
        'susheela', 'suseela', 'yesudas', 'mano', 'harris', 'anirudh', 'yuvan', 'vidyasagar',
        'hariharan', 'janaki', 'swarnalatha', 'kannadasan', 'soundararajan', 'viswanathan',
        'vadivelu', 'goundamani', 'vivek', 'santhanam', 'tamil comedy')]
    inventory = (pathlib.Path(__file__).parent/'screenshot-inventory.txt').read_text(encoding='utf8')
    requests += [dict(name=term, limit='50') for term in reference_queries(inventory)]
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
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
