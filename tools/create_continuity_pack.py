#!/usr/bin/env python3
"""Export an explicit documents-only handover ZIP; never walk the repo or copy secrets."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
DOCUMENTS = (
    'START-HERE.md',
    'TECHNICAL-HANDOVER.md',
    'PRIVATE-BACKUP-CHECKLIST.md',
    'agent-start-prompt.txt',
    'release-inventory.json',
)
PREFIX = 'RP-Android-Apps-continuity/'


def create_pack(output):
    source = ROOT / 'docs' / 'continuity'
    contents = {}
    for name in DOCUMENTS:
        path = source / name
        if path.is_symlink() or path.resolve().parent != source.resolve():
            raise ValueError('Refuse redirected handover document: ' + name)
        raw = path.read_bytes()
        if len(raw) > 1_000_000:
            raise ValueError('Unexpectedly large handover document: ' + name)
        raw.decode('utf-8')
        contents[name] = raw
    inventory = json.loads(contents['release-inventory.json'])
    if inventory['actual_apk_files_backed_up'] or inventory['private_signing_backup_verified']:
        raise ValueError('A documents-only exporter must not claim APK/key backups')
    if len(inventory['records']) != 14 or len({r['id'] for r in inventory['records']}) != 14:
        raise ValueError('Expected dated inventory of 13 apps plus separate Store')
    commit = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    info = dict(format_version=1, repository='universalrp2003/RP-Android-Apps',
                documentation_commit=commit, inventory_checked_on=inventory['checked_on'],
                scope='Documents and public release metadata ONLY; not a full project or private-data backup.',
                files=list(DOCUMENTS), source_code_included=False, git_history_included=False,
                apks_included=False, private_keys_included=False, account_secrets_included=False,
                phone_data_included=False, cloud_service_backup_included=False)
    contents['PACK-INFO.json'] = (json.dumps(info, indent=2) + '\n').encode()
    sums = ''.join(hashlib.sha256(raw).hexdigest() + '  ' + name + '\n'
                   for name, raw in contents.items())
    contents['SHA256SUMS.txt'] = sums.encode()
    # Exclusive creation: never replace a previously downloaded/archived backup.
    with Path(output).open('xb') as stream:
        with zipfile.ZipFile(stream, 'w', compression=zipfile.ZIP_DEFLATED) as archive:
            for name, raw in contents.items():
                archive.writestr(PREFIX + name, raw)
    return info


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True,
                        help='New ZIP file, not an existing backup. No network or release operations.')
    args = parser.parse_args()
    create_pack(args.output)
    digest = hashlib.sha256(args.output.read_bytes()).hexdigest()
    print(f'Created documents-only handover: {args.output} ({args.output.stat().st_size} bytes)')
    print(f'SHA256 {digest}')
    print('Source/APK/signing-key/phone-data/account backups remain owner actions; not included.')


if __name__ == '__main__':
    main()
