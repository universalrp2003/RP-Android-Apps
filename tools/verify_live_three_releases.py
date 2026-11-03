#!/usr/bin/env python3
"""Post-deployment read-only proof of the authorized public releases and privacy-reviewed Launcher gallery on Pages.

Only fixed official paths; no owner data, external submissions or release mutation.
Run in main Pages deploy after the checked artifact is actually deployed.
"""
import hashlib
import io
import json
import os
from pathlib import Path
import time
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
BASE = 'https://universalrp2003.github.io/RP-Android-Apps/'


def get_bounded(relative, limit):
    # Caller supplies only checked-in metadata names or pinned canonical APK names.
    assert relative and '..' not in relative and not relative.startswith('/') and ':' not in relative
    url = BASE + relative + '?verified=' + os.environ.get('GITHUB_SHA', 'local-check')
    request = urllib.request.Request(url, headers={'User-Agent': 'RP-Pages-Release-Verification', 'Cache-Control': 'no-cache'})
    with urllib.request.urlopen(request, timeout=60) as response:
        data = response.read(limit + 1)
    if len(data) > limit:
        raise ValueError('Oversized official response: ' + relative)
    return data


def validate_apk(data, size, digest):
    if len(data) != size or hashlib.sha256(data).hexdigest() != digest:
        raise ValueError('Live APK bytes do not match the exact published candidate')


def validate_zip(data, name, size, digest):
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        if archive.namelist() != [name] or archive.getinfo(name).file_size != size:
            raise ValueError('Unexpected mirrored ZIP contents or expansion size')
        validate_apk(archive.read(name), size, digest)


def check_metadata():
    for name in ('index.html', 'oor-arivippu-guide.html', 'release-dates.json', 'catalog.json', 'catalog-v2.json'):
        data = get_bounded(name, 2_000_000)
        local = (ROOT / 'site' / name).read_bytes()
        matches = data == local if name.endswith('.html') else json.loads(data) == json.loads(local)
        if not matches:
            raise ValueError('Live metadata is not the tested current source: ' + name)
    print('PASS: live HTML, actual release dates, legacy12-app and current13-app feeds match tested source', flush=True)


def main():
    assert os.environ.get('GITHUB_ACTIONS') == 'true' and os.environ.get('GITHUB_REF') == 'refs/heads/main'
    assert os.environ['PAGES_URL'].rstrip('/') + '/' == BASE
    # Allow bounded CDN propagation; do not accept stale pages or weaken hash checks.
    for attempt in range(8):
        try:
            check_metadata()
            break
        except (OSError, ValueError) as error:
            if attempt == 7:
                raise
            print(f'Waiting for current deployed metadata ({attempt + 1}/8): {error}', flush=True)
            time.sleep(10)
    health = json.loads((ROOT / 'docs/approved-health-candidates.json').read_text())
    store = json.loads((ROOT / 'docs/approved-store-0.9-candidate.json').read_text())
    checks = [('Oor-Arivippu-v1.1.15.apk', json.loads((ROOT/'docs/oor-family-1.1.15-publication-receipt.json').read_text())),
              ('CleanSweep-v2.93.apk', json.loads((ROOT/'docs/live-guard-2.93-publication-receipt.json').read_text())),
              ('CleanSweep-v2.89.apk', json.loads((ROOT/'docs/live-guard-2.89-publication-receipt.json').read_text())),
              ('Tamilan-Store-v0.9.apk', store),
              ('Voice-Notes-v1.3.4.apk', health['apps']['voicenotes']),
              ('CleanSweep-v2.82.apk', health['apps']['cleaner']),
              ('Elderly-Launcher-v1.7.apk', json.loads((ROOT/'docs/launcher-1.7-publication.json').read_text()))]
    for name, pin in checks:
        assert 0 < pin['bytes'] < 40_000_000
        validate_apk(get_bounded('apks/' + name, pin['bytes']), pin['bytes'], pin['apk_sha256'])
        validate_zip(get_bounded('apks/' + name[:-4] + '.zip', 40_000_000), name, pin['bytes'], pin['apk_sha256'])
        print(f'PASS: live APK and ZIP {name}: {pin["bytes"]}B SHA256 {pin["apk_sha256"]}', flush=True)
    gallery = json.loads((ROOT/'docs/launcher-1.7-gallery-review.json').read_text())
    for image in gallery['images']:
        data = get_bounded(image['website'], 1_000_000)
        validate_apk(data, image['bytes'], image['sha256'])
        print('PASS: live privacy-reviewed Launcher screenshot ' + image['website'], flush=True)
    print(f'PASS: main Pages serves all {len(checks)} exact APK/ZIP pairs and safe Launcher screenshots; no Google/device clearance claimed', flush=True)


if __name__ == '__main__':
    main()
