#!/usr/bin/env python3
"""Bounded, passive collection of name/URL leads from public station pages.

These are directory claims, not verified identities or assurances of availability.
Only fixed pages are fetched; scripts are not run and no stream URLs are fetched.
The receipt is research data, not a seed import.
"""
import html
import json
import pathlib
import re
import sys
import urllib.parse
import urllib.request

PAGES = [
    'https://www.tamilradios.com/aanmeega-sudar',
    'https://www.tamilradios.com/',
    'https://www.tamilradios.com/page/2',
    'https://www.tamilradios.com/page/3',
    'https://www.tamilradios.com/page/4',
]
PATTERN = re.compile(r'"name":"([^"\\]{1,160})"[^{}]{0,600}?"url":"(https?://[^"\\]{1,1000})"')


def extract(body, source):
    text = html.unescape(body).replace('\\/', '/').replace(r'\"', '"')
    if 'Please wait while your request is being verified' in text:
        raise ValueError('verification challenge')
    records = []
    for name, url in PATTERN.findall(text):
        parsed = urllib.parse.urlsplit(url)
        if not parsed.hostname or parsed.username or parsed.password:
            continue
        # Expiring/authenticated query parameters must never become seed URLs.
        query = urllib.parse.parse_qs(parsed.query)
        if any(re.search(r'token|secret|signature|auth|expires|^zt$|^zs$', k, re.I) for k in query):
            continue
        records.append({'name': name, 'url': url, 'source': source})
    return records


def main(out):
    records, errors = [], []
    for url in PAGES:
        try:
            request = urllib.request.Request(url, headers={'User-Agent': 'RameshRadio/1.0 (reference research)'})
            with urllib.request.urlopen(request, timeout=18) as response:
                body = response.read(2_000_001)
                if len(body) > 2_000_000:
                    raise ValueError('page too large')
            records.extend(extract(body.decode('utf-8', errors='replace'), url))
        except Exception as exc:
            errors.append({'page': url, 'error': type(exc).__name__})
    unique = list({(r['name'], r['url']): r for r in records}.values())
    result = {'records': unique, 'errors': errors, 'limits': 'Public directory leads only; all require manual identity review and bounded reachability probing.'}
    path = pathlib.Path(out); path.mkdir(parents=True, exist_ok=True)
    (path/'tamilradios-leads.json').write_text(json.dumps(result, ensure_ascii=False, indent=2)+'\n', encoding='utf8')
    print('Retrieved', len(unique), 'distinct name/URL pairs,', len(errors), 'page errors')


if __name__ == '__main__':
    main(sys.argv[1])
