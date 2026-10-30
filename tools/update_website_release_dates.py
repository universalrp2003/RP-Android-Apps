#!/usr/bin/env python3
"""Check offline, or refresh pinned website dates from GitHub's actual published_at.

Usage: python3 tools/update_website_release_dates.py [--refresh]
No date is inferred from commits, tags, APK versions or the local clock.
"""
import argparse
from datetime import datetime
import json
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
TIME = re.compile(r'<time datetime="([^"]+)" data-release-tag="([^"]+)">([^<]+)</time>')


def date_label(value):
    if not re.fullmatch(r'\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z', value):
        raise ValueError('Expected GitHub UTC published_at, not an inferred date: ' + value)
    date = datetime.fromisoformat(value.replace('Z', '+00:00'))
    months = ('Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec')
    return f'{date.day:02d} {months[date.month - 1]} {date.year}'


def check(source, metadata):
    table = source.split('<tbody>', 1)[1].split('</tbody>', 1)[0]
    rows = re.findall(r'<tr>.*?</tr>', table, re.S)
    tags = set()
    for row in rows:
        pinned = re.search(r'releases/download/([^/]+)/', row).group(1)
        dates = TIME.findall(row)
        if len(dates) != 1:
            raise ValueError('Expected one actual release publication date: ' + pinned)
        value, tag, label = dates[0]
        if tag != pinned or tag in tags or metadata['releases'].get(tag) != value or date_label(value) != label:
            raise ValueError('Date does not match pinned release evidence: ' + pinned)
        tags.add(tag)
    if not tags or tags != set(metadata['releases']):
        raise ValueError('Release date evidence and website releases differ; refresh dates after publication.')
    if metadata['source'] != 'GitHub Releases published_at (UTC)':
        raise ValueError('Release date source must be explicit.')
    return len(tags)


def refresh(source, load_release):
    evidence = {}
    def replace(row_match):
        row = row_match.group(0)
        pinned = re.search(r'releases/download/([^/]+)/', row)
        if not pinned:
            return row
        tag = pinned.group(1)
        release = load_release(tag)
        if release.get('draft') or release.get('tag_name') != tag or not release.get('published_at'):
            raise ValueError('Pinned release is not published: ' + tag)
        published = release['published_at']
        evidence[tag] = published
        value = f'<time datetime="{published}" data-release-tag="{tag}">{date_label(published)}</time>'
        if len(TIME.findall(row)) != 1:
            raise ValueError('Expected one date field per pinned release: ' + tag)
        return TIME.sub(lambda _: value, row)
    updated = re.sub(r'<tr>.*?</tr>', replace, source, flags=re.S)
    metadata = {'source': 'GitHub Releases published_at (UTC)', 'releases': evidence}
    check(updated, metadata)  # Never write a partial or stale refresh.
    return updated, metadata


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--refresh', action='store_true')
    args = parser.parse_args()
    path = ROOT / 'site/index.html'
    evidence = ROOT / 'site/release-dates.json'
    source = path.read_text()
    if args.refresh:
        def load(tag):
            return json.loads(subprocess.check_output(['gh', 'api', f'repos/universalrp2003/RP-Android-Apps/releases/tags/{tag}'], text=True))
        source, metadata = refresh(source, load)
        path.write_text(source)
        evidence.write_text(json.dumps(metadata, indent=2) + '\n')
    else:
        metadata = json.loads(evidence.read_text())
    print(f'PASS: {check(source, metadata)} website dates match recorded GitHub publication timestamps (UTC).')


if __name__ == '__main__':
    main()
