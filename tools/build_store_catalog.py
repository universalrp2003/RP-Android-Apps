#!/usr/bin/env python3
"""Generate Store's live feed from canonical public GitHub releases, not CI artifacts."""
import json
import os
import re
import urllib.request
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
REPO = 'universalrp2003/RP-Android-Apps'

def generate(snapshot, releases):
    apps = []
    for app in snapshot['apps']:
        tag = app['releaseUrl'].rsplit('/', 1)[1]
        slug = tag.rsplit('-v', 1)[0]
        prefix = app['url'].rsplit('/', 1)[1].rsplit('-v', 1)[0]
        matches = []
        for release in releases:
            if release.get('draft') or release.get('prerelease'):
                continue
            m = re.fullmatch(re.escape(slug) + r'-v([0-9]+(?:\.[0-9]+){1,3})', release['tag_name'])
            if not m:
                continue
            version = m[1]
            for asset in release.get('assets', []):
                digest = asset.get('digest', '') or ''
                expected = f'https://github.com/{REPO}/releases/download/{release["tag_name"]}/{prefix}-v{version}.apk'
                if asset.get('name') == f'{prefix}-v{version}.apk' and asset.get('browser_download_url') == expected and re.fullmatch(r'sha256:[a-f0-9]{64}', digest) and 0 < asset['size'] <= 200_000_000:
                    parts = tuple(map(int, version.split('.')))
                    matches.append((parts + (0,) * (4-len(parts)), version, release, asset))
        if not matches:
            raise ValueError(f'No verified public APK metadata for {app["id"]}')
        _, version, release, asset = max(matches, key=lambda x: x[0])
        old = tuple(map(int, app['version'].split('.')))
        old = old + (0,) * (4-len(old))
        if tuple(map(int, version.split('.'))) + (0,) * (4-len(version.split('.'))) < old:
            raise ValueError('Refusing catalog rollback')
        apps.append(dict(app, version=version, url=asset['browser_download_url'], size=asset['size'], sha256=asset['digest'][7:], releaseUrl=f'https://github.com/{REPO}/releases/tag/{release["tag_name"]}'))
    return dict(snapshot, apps=apps)

def main():
    headers = {'Accept': 'application/vnd.github+json', 'User-Agent': 'RP-Store-Catalog'}
    if os.environ.get('GH_TOKEN'):
        headers['Authorization'] = 'Bearer ' + os.environ['GH_TOKEN']
    releases = []
    for page in range(1, 21):
        req = urllib.request.Request(f'https://api.github.com/repos/{REPO}/releases?per_page=100&page={page}', headers=headers)
        with urllib.request.urlopen(req, timeout=30) as response:
            batch = json.load(response)
        releases.extend(batch)
        if len(batch) < 100:
            break
    else:
        raise ValueError('Release feed exceeded pagination limit')
    snapshot = json.loads((ROOT/'tamilanstore/src/main/assets/catalog.json').read_text())
    result = generate(snapshot, releases)
    (ROOT/'site/catalog.json').write_text(json.dumps(result, ensure_ascii=False, indent=2)+'\n')

if __name__ == '__main__':
    main()
