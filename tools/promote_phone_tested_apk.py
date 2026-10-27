#!/usr/bin/env python3
"""Fail-closed promotion of an immutable, owner-tested Actions APK, never a fresh rebuild."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def configuration(module):
    text = (ROOT / module / 'build.gradle.kts').read_text()
    def one(pattern):
        values = re.findall(pattern, text)
        if len(values) != 1:
            raise ValueError('Expected exactly one package/version field')
        return values[0]
    return {'applicationId': one(r'applicationId\s*=\s*"([^"\n]+)"'),
            'versionName': one(r'versionName\s*=\s*"([^"\n]+)"'),
            'versionCode': one(r'versionCode\s*=\s*(\d+)')}


def compiled_changes(paths, module):
    return [path for path in paths if
            (path.startswith(module + '/') and path != module + '/README.md') or
            path.startswith(('shared/', 'gradle/', 'ci-keystore/')) or
            path in {'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties'}]


def check_receipt(text, apk, expected, source):
    fields = {}
    for line in text.splitlines():
        if ': ' not in line:
            continue
        key, value = line.split(': ', 1)
        if key in fields:
            raise ValueError('Duplicate candidate receipt field')
        fields[key] = value
    for key, value in {**expected, 'Source': source, 'File': apk.name}.items():
        if fields.get(key) != value:
            raise ValueError('Candidate receipt identity/source does not match')
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    if fields.get('SHA-256') != digest or fields.get('Bytes') != str(apk.stat().st_size) or apk.stat().st_size == 0:
        raise ValueError('Candidate APK bytes differ from the immutable artifact receipt')
    return digest


def api(path):
    return json.loads(subprocess.check_output(['gh', 'api', path], text=True))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('mode', choices=['prepare', 'promote'])
    parser.add_argument('module', choices=['launcher', 'podcast'])
    parser.add_argument('--artifact-root', type=Path)
    args = parser.parse_args()
    expected = configuration(args.module)
    manifest = json.loads((ROOT / 'releases/phone-tested-candidates.json').read_text())
    entry = manifest.get(args.module)
    if entry is None:
        raise ValueError('No candidate manifest entry; refusing to guess')
    if entry['version'] != expected['versionName']:
        if args.mode == 'promote':
            raise ValueError('No phone-tested artifact for this version')
        with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
            output.write('eligible=false\n')
        return
    if args.mode == 'prepare':
        repo = os.environ['GITHUB_REPOSITORY']
        run = api(f'repos/{repo}/actions/runs/{entry["run"]}')
        if (run['conclusion'], run['status'], run['head_sha'], run['name'], run['event']) != (
                'success', 'completed', entry['source'], 'Verify launcher podcast and Vasanthi candidates', 'push'):
            raise ValueError('Candidate provenance is not the successful pinned source run')
        artifacts = api(f'repos/{repo}/actions/runs/{entry["run"]}/artifacts?per_page=100')['artifacts']
        matches = [a for a in artifacts if a['name'] == entry['artifact']]
        if len(matches) != 1 or matches[0]['id'] != entry['artifact_id'] or matches[0]['expired']:
            raise ValueError('Pinned candidate artifact is missing, expired or ambiguous')
        subprocess.run(['git', 'fetch', '--no-tags', 'origin', entry['source']], check=True)
        paths = subprocess.check_output(['git', 'diff', '--name-only', entry['source'], 'HEAD'], text=True).splitlines()
        if compiled_changes(paths, args.module):
            raise ValueError('Compiled inputs changed after the phone-tested candidate: ' + ', '.join(compiled_changes(paths, args.module)))
        with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
            output.write(f'eligible=true\nrun={entry["run"]}\nartifact={entry["artifact"]}\n')
        print(f'Pinned owner-tested candidate source {entry["source"]}, run {entry["run"]}, artifact {entry["artifact_id"]}')
    else:
        if args.artifact_root is None:
            raise ValueError('Artifact root is required')
        apks = list(args.artifact_root.rglob('*.apk'))
        receipts = list(args.artifact_root.rglob('candidate-info.txt'))
        if len(apks) != 1 or len(receipts) != 1:
            raise ValueError('Expected exactly one APK and receipt; refusing to promote nothing or guess')
        digest = check_receipt(receipts[0].read_text(), apks[0], expected, entry['source'])
        destination = ROOT / args.module / 'build/outputs/apk/release'
        built = list(destination.glob('*.apk'))
        if len(built) != 1:
            raise ValueError('Expected exactly one current release output')
        shutil.copyfile(apks[0], built[0])
        if hashlib.sha256(built[0].read_bytes()).hexdigest() != digest:
            raise ValueError('Promoted APK copy differs')
        print(f'Promoted exact owner-tested APK, SHA-256 {digest}; no APK rebuild is published.')


if __name__ == '__main__':
    main()
