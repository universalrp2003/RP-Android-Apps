#!/usr/bin/env python3
"""Owner-run public APK backup staging. No build, publication, Drive auth or secret access.

Run on a trusted computer with GitHub CLI/network access. All output must be below /tmp.
This sandbox may not be able to follow GitHub release asset downloads.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
REPO = 'universalrp2003/RP-Android-Apps'


def validate_inventory(data):
    records = data['records']
    if len(records) != 14 or len({r['id'] for r in records}) != 14:
        raise ValueError('Expected dated inventory of 14 distinct apps')
    for app in records:
        if not re.fullmatch(r'[A-Za-z0-9._-]+\.apk', app['apk']):
            raise ValueError('Invalid canonical APK filename')
        if not re.fullmatch(r'[a-z0-9-]+-v[0-9]+(?:\.[0-9]+)+', app['tag']):
            raise ValueError('Invalid public release tag')
        expected = f"https://github.com/{REPO}/releases/download/{app['tag']}/{app['apk']}"
        if app['url'] != expected or not re.fullmatch(r'[0-9a-f]{64}', app['sha256']):
            raise ValueError('Invalid public URL/digest')
        if not 0 < app['bytes'] <= 100_000_000:
            raise ValueError('Unexpected APK size')
    return records


def safe_staging_path(path):
    result = Path(path).resolve()
    if result == Path('/tmp') or Path('/tmp') not in result.parents:
        raise ValueError('APK staging must be a new directory strictly below /tmp')
    return result


def verify_file(path, app):
    if path.stat().st_size != app['bytes'] or hashlib.sha256(path.read_bytes()).hexdigest() != app['sha256']:
        raise ValueError('APK size/SHA256 mismatch; do not use: ' + app['apk'])


def download(inventory, output):
    records = validate_inventory(inventory)
    output = safe_staging_path(output)
    # No resume/overwrite: a failed attempt is visibly incomplete in its own directory.
    output.mkdir(parents=True, exist_ok=False)
    for app in records:
        metadata = json.loads(subprocess.check_output(
            ['gh', 'api', f"repos/{REPO}/releases/{app['release_id']}"], text=True))
        asset = next(a for a in metadata['assets'] if a['id'] == app['asset_id'])
        if metadata['draft'] or metadata['prerelease'] or metadata['tag_name'] != app['tag']:
            raise ValueError('Expected immutable public release')
        if (asset['name'], asset['size'], asset.get('digest'), asset['browser_download_url']) != (
                app['apk'], app['bytes'], 'sha256:' + app['sha256'], app['url']):
            raise ValueError('Public release metadata no longer matches inventory')
        subprocess.run(['gh', 'release', 'download', app['tag'], '--repo', REPO,
                        '--pattern', app['apk'], '--dir', str(output)], check=True)
        verify_file(output / app['apk'], app)
        print('Verified ' + app['apk'], flush=True)
    archive_path = output / 'VERIFIED-14-APKs.zip'
    with zipfile.ZipFile(archive_path, 'x', compression=zipfile.ZIP_STORED) as archive:
        for app in records:
            archive.write(output / app['apk'], app['apk'])
        archive.writestr('release-inventory.json', json.dumps(inventory, indent=2) + '\n')
        archive.writestr('SHA256SUMS.txt', ''.join(a['sha256'] + '  ' + a['apk'] + '\n' for a in records))
    report = dict(complete=True, verified_apks=14, archive=archive_path.name,
                  bytes=archive_path.stat().st_size, sha256=hashlib.sha256(archive_path.read_bytes()).hexdigest(),
                  uploaded_to_drive=False, scope='Verified APK files staged in /tmp; owner must upload archive and verify Drive copy.')
    (output / 'DOWNLOAD-RESULT.json').write_text(json.dumps(report, indent=2) + '\n')
    return archive_path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--inventory', type=Path, default=ROOT / 'docs/continuity/release-inventory.json')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    archive = download(json.loads(args.inventory.read_text()), args.output)
    print(f'All 14 APKs verified: {archive}')
    print('Not a Drive backup yet. Upload the ZIP to your private snapshot folder and check the copied file.')


if __name__ == '__main__':
    main()
