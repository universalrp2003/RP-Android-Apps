#!/usr/bin/env python3
"""Inspect a short, fixed list of public player pages for stream-link leads.

No JavaScript execution, login, cookies, challenge bypass, or audio is retained.
Output consists of public URL origins/paths (query strings are dropped) for manual
review; links alone do not establish station identity, rights, or availability.
"""
import html
import json
import pathlib
import re
import sys
import urllib.parse
import urllib.request

PAGES = {
    'Aanmeega Sudar Zeno': 'https://zeno.fm/radio/aanmeegasudar/',
    'Aanmeega Sudar directory': 'https://www.tamilradios.com/aanmeega-sudar',
    'Bisme official': 'https://bismeradio.com/',
    'Gilli official': 'https://gillifm.com/',
    'Muthamil official': 'https://muthamiltv.com/',
    'Mersey Tamil directory': 'https://liveradios.in/mersey-radio-fm.html',
}


def inspect(url):
    request = urllib.request.Request(url, headers={'User-Agent': 'RameshRadio/1.0 (reference research)'})
    with urllib.request.urlopen(request, timeout=18) as response:
        body = response.read(2_000_001)
        if len(body) > 2_000_000:
            raise ValueError('page too large')
    text = html.unescape(body.decode('utf-8', errors='replace')).replace('\\/', '/')
    if 'Please wait while your request is being verified' in text:
        raise ValueError('site verification challenge')
    urls = re.findall(r'https?://[^\s\"\'<>\\]+', text, re.IGNORECASE)
    matches = set()
    for raw in urls:
        parsed = urllib.parse.urlsplit(raw.rstrip('),;'))
        if not parsed.hostname:
            continue
        host = parsed.hostname.lower()
        path = parsed.path
        if re.search(r'(stream|listen|radio\.co|zeno\.fm|\.m3u8|\.mp3|\.aac|player|embed)', host + path, re.I):
            matches.add(urllib.parse.urlunsplit((parsed.scheme, parsed.netloc, path, '', '')))
    contexts = []
    if url == PAGES['Aanmeega Sudar directory']:
        for needle in ('ymkr7th4f2zuv', 'aadhisivam', 'omsakthifm', 'vishnufm', 'ayngaranradio', 'islamicradio', 'saranamayyappa'):
            offset = text.find(needle)
            if offset >= 0:
                contexts.append(text[max(0, offset - 300):offset + len(needle) + 100])
    return {'urls': sorted(matches)[:250], 'contexts': contexts}


def main(out):
    result = {}
    for name, url in PAGES.items():
        try:
            result[name] = {'page': url, 'leads': inspect(url)}
        except Exception as exc:
            result[name] = {'page': url, 'error': type(exc).__name__}
    path = pathlib.Path(out)
    path.mkdir(parents=True, exist_ok=True)
    (path / 'public-player-leads.json').write_text(json.dumps(result, ensure_ascii=False, indent=2)+'\n', encoding='utf8')
    print('Inspected', len(result), 'public pages; leads require individual review.')


if __name__ == '__main__':
    main(sys.argv[1])
