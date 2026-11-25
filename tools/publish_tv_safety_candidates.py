#!/usr/bin/env python3
"""Publish two owner-authorized, exact emulator-verified TV candidates without rebuilding.

Run only in the dedicated CI workflow. All candidate and baseline gates run before any draft is
created. Never overwrite a tag, release, draft, or earlier asset; a partial failure needs review.
"""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time

REPO = 'universalrp2003/RP-Android-Apps'
BRANCH = 'arena/30198928-rp-android-apps'
SIGNER = '7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d'
WORK = Path(os.environ.get('RUNNER_TEMP', '/nonexistent')) / 'tv-safety-publication'


def cmd(*args):
    return subprocess.check_output([str(a) for a in args], text=True).strip()


def api(path):
    return json.loads(cmd('gh', 'api', f'repos/{REPO}/{path}'))


def releases():
    return [entry for page in json.loads(cmd('gh', 'api', '--paginate', '--slurp', f'repos/{REPO}/releases?per_page=100')) for entry in page]


def snapshots(items):
    return {r['id']: (r['tag_name'], r['draft'], sorted((a['id'], a['name'], a['size'], a.get('digest')) for a in r['assets'])) for r in items if not r['draft']}


def digest(path):
    return hashlib.file_digest(path.open('rb'), 'sha256').hexdigest()


def one(items):
    assert len(items) == 1, f'Expected exactly one item, got {len(items)}'
    return items[0]


def tools_dir():
    return sorted((Path(os.environ['ANDROID_HOME']) / 'build-tools').iterdir(), key=lambda p: tuple(int(n) for n in re.findall(r'\d+', p.name)))[-1]


def artifact_download(run_id, artifact, root):
    root.mkdir(parents=True, exist_ok=True)
    subprocess.run(['gh', 'run', 'download', str(run_id), '--repo', REPO, '--name', artifact, '--dir', str(root)], check=True)


def download_asset(asset, target):
    with target.open('wb') as output:
        subprocess.run(['gh', 'api', f'repos/{REPO}/releases/assets/{asset["id"]}', '-H', 'Accept: application/octet-stream'], stdout=output, check=True)


def verify_prior(before):
    after = snapshots(releases())
    for release_id, snapshot in before.items():
        assert after.get(release_id) == snapshot, f'Prior public release {release_id} changed'


def preflight(p, prior, tools):
    assert p['package'] in ('com.universalrp.tvguard', 'com.universalrp.appdisabler')
    run = api(f'actions/runs/{p["run_id"]}')
    assert (run['status'], run['conclusion'], run['head_sha'], run['head_branch'], run['run_attempt'], run['path']) == (
        'completed', 'success', p['source'], BRANCH, p['run_attempt'], p['workflow']), run
    subprocess.run(['git', 'merge-base', '--is-ancestor', p['source'], 'HEAD'], check=True)
    subprocess.run(['git', 'diff', '--exit-code', p['source'], 'HEAD', '--', *p['compiled_paths']], check=True)
    a = one([a for a in api(f'actions/runs/{p["run_id"]}/artifacts')['artifacts'] if a['name'] == p['artifact']])
    assert a['id'] == p['artifact_id'] and not a['expired'] and a.get('digest') == 'sha256:' + p['archive_sha256']
    matching = [r for r in prior if r['tag_name'] == p['tag']]
    draft_id = p.get('resume_draft_id')
    if draft_id:
        assert all(r['id'] == draft_id for r in matching), 'Unexpected release or draft'
        draft = api(f'releases/{draft_id}')
        assert draft['draft'] and not draft['prerelease'] and draft['tag_name'] == p['tag'] and draft['target_commitish'] == p['source']
        assets = {a['name']: a for a in draft['assets']}
        assert set(assets) == {p['asset'], p['asset'] + '.sha256'}
        assert assets[p['asset']]['id'] == p['resume_apk_asset_id'] and assets[p['asset']]['digest'] == 'sha256:' + p['sha256'] and assets[p['asset']]['size'] == p['bytes']
        assert assets[p['asset'] + '.sha256']['id'] == p['resume_checksum_asset_id']
    else:
        assert not matching, 'Release or draft already exists'
    assert not cmd('git', 'ls-remote', '--tags', 'origin', f'refs/tags/{p["tag"]}'), 'Tag already exists'
    baseline = one([r for r in prior if not r['draft'] and r['tag_name'] == p['previous_tag']])
    old_asset = one([a for a in baseline['assets'] if a['name'] == p['previous_asset']])
    assert old_asset['digest'] == 'sha256:' + p['previous_sha256'] and old_asset['size'] == p['previous_bytes']

    root = WORK / p['tag']
    artifact_download(p['run_id'], p['artifact'], root / 'artifact')
    source_apk = one(list((root / 'artifact').rglob(p['candidate_file'])))
    receipt = json.loads(one(list((root / 'artifact').rglob('verification.json'))).read_text())
    assert (receipt['package'], receipt['versionName'], receipt['versionCode'], receipt['sourceCommit']) == (p['package'], p['version'], p['code'], p['source'])
    assert receipt['workflowRun'] == str(p['run_id']) and receipt['exactCandidateInstalledAndTested'] is True and receipt['published'] is False
    assert receipt['apkSha256'] == p['sha256'] and receipt['apkBytes'] == p['bytes']
    assert digest(source_apk) == p['sha256'] and source_apk.stat().st_size == p['bytes']
    sidecar = one(list((root / 'artifact').rglob(p['candidate_sidecar'])))
    assert sidecar.read_text().split()[0] == p['sha256']
    apk = root / p['asset']
    apk.write_bytes(source_apk.read_bytes())
    checksum = root / (p['asset'] + '.sha256')
    checksum.write_text(p['sha256'] + '  ' + p['asset'] + '\n')

    olddir = root / 'old'; olddir.mkdir()
    subprocess.run(['gh', 'release', 'download', p['previous_tag'], '--repo', REPO, '--pattern', p['previous_asset'], '--dir', str(olddir)], check=True)
    old = olddir / p['previous_asset']
    assert digest(old) == p['previous_sha256'] and old.stat().st_size == p['previous_bytes']
    for path in (old, apk):
        certs = re.findall(r'certificate SHA-256 digest: ([a-fA-F0-9]+)', cmd(tools / 'apksigner', 'verify', '--print-certs', path))
        assert certs and set(c.lower() for c in certs) == {SIGNER}
    badging = cmd(tools / 'aapt', 'dump', 'badging', apk)
    assert f"package: name='{p['package']}' versionCode='{p['code']}' versionName='{p['version']}'" in badging
    assert 'application-debuggable' not in badging
    def permissions(path):
        return {line.strip() for line in cmd(tools / 'aapt', 'dump', 'permissions', path).splitlines() if line.startswith('uses-permission:')}
    added = permissions(apk) - permissions(old)
    expected_added = set(p.get('expected_added_permissions', []))
    assert added == expected_added, f'Unexpected permission changes: added={added}, expected={expected_added}'
    print('PREFLIGHT PASS', p['tag'], p['sha256'], p['bytes'], flush=True)
    return apk, checksum


def wait_assets(release_id, p):
    for attempt in range(15):
        release = api(f'releases/{release_id}')
        assets = {a['name']: a for a in release['assets']}
        if set(assets) == {p['asset'], p['asset'] + '.sha256'}:
            if assets[p['asset']].get('digest') == 'sha256:' + p['sha256'] and assets[p['asset']]['size'] == p['bytes']:
                return release, assets
        time.sleep(2)
    raise AssertionError('Draft assets or digest not ready; refusing to publish')


def publish(p, apk, checksum, before):
    if p.get('resume_draft_id'):
        draft = api(f'releases/{p["resume_draft_id"]}')
    else:
        # The create response supplies the ID directly; release listings can lag new drafts.
        payload = json.dumps({'tag_name': p['tag'], 'target_commitish': p['source'], 'name': p['title'],
                              'body': Path(p['notes']).read_text(), 'draft': True, 'prerelease': False})
        draft = json.loads(subprocess.run(['gh', 'api', '--method', 'POST', f'repos/{REPO}/releases', '--input', '-'],
                                          input=payload, text=True, capture_output=True, check=True).stdout)
        subprocess.run(['gh', 'release', 'upload', p['tag'], str(apk), str(checksum), '--repo', REPO], check=True)
    assert draft['draft'] and not draft['prerelease'] and draft['target_commitish'] == p['source']
    draft, assets = wait_assets(draft['id'], p)
    actual = apk.parent / 'draft-download.apk'
    download_asset(assets[p['asset']], actual)
    assert actual.stat().st_size == p['bytes'] and digest(actual) == p['sha256']
    downloaded_checksum = apk.parent / 'draft-download.sha256'
    download_asset(assets[p['asset'] + '.sha256'], downloaded_checksum)
    assert downloaded_checksum.read_bytes() == checksum.read_bytes()
    verify_prior(before)
    subprocess.run(['gh', 'api', '--method', 'PATCH', f'repos/{REPO}/releases/{draft["id"]}', '-F', 'draft=false'], check=True)
    public = api('releases/tags/' + p['tag'])
    assert public['id'] == draft['id'] and not public['draft'] and not public['prerelease']
    a = one([a for a in public['assets'] if a['name'] == p['asset']])
    assert a['id'] == assets[p['asset']]['id'] and a['digest'] == 'sha256:' + p['sha256'] and a['size'] == p['bytes']
    out = apk.parent / 'public'; out.mkdir()
    subprocess.run(['gh', 'release', 'download', p['tag'], '--repo', REPO, '--pattern', p['asset'] + '*', '--dir', str(out)], check=True)
    assert digest(out / p['asset']) == p['sha256'] and (out / p['asset']).stat().st_size == p['bytes']
    assert (out / (p['asset'] + '.sha256')).read_bytes() == checksum.read_bytes()
    verify_prior(before)
    result = {'release_id': public['id'], 'asset_id': a['id'], 'tag': p['tag'], 'published_at': public['published_at'],
              'apk_sha256': p['sha256'], 'bytes': p['bytes'], 'url': a['browser_download_url'],
              'candidate_run': p['run_id'], 'source_commit': p['source'], 'owner_authorized': True,
              'physical_test_scope': p.get('physical_test_scope', 'Owner reported earlier candidates working on their TV and remote; corrected APK not physically retested.')}
    (WORK / (p['tag'] + '-publication.json')).write_text(json.dumps(result, indent=2) + '\n')
    print('PUBLICATION RECEIPT ' + json.dumps(result), flush=True)


def main():
    assert os.environ.get('GITHUB_ACTIONS') == 'true'
    assert os.environ.get('GITHUB_REF') == f'refs/heads/{BRANCH}'
    assert os.environ.get('GH_REPO') == REPO
    pins = Path('docs/tv-safety-candidate-pins.json')
    data = json.loads(pins.read_text())
    dry_run = os.environ.get('TV_SAFETY_PREFLIGHT_ONLY') == 'true'
    if not dry_run:
        approval = json.loads(Path('docs/publish-tv-safety-go.json').read_text())
        assert approval['owner_authorized'] is True and approval['pins_sha256'] == digest(pins)
    assert data['owner_authorized'] is True and len(data['candidates']) == 2
    assert {p['tag'] for p in data['candidates']} == {'tvguard-v1.5.2', 'system-app-disabler-v0.5.0'}
    WORK.mkdir(parents=True, exist_ok=True)
    prior = releases()
    before = snapshots(prior)
    tools = tools_dir()
    ready = [(p, *preflight(p, prior, tools)) for p in data['candidates']]
    if dry_run:
        print('PASS: both candidates passed preflight; no release was created')
        return
    for p, apk, checksum in ready:
        publish(p, apk, checksum, before)
    print('PASS: both exact candidates are public; previous release assets are unchanged')


if __name__ == '__main__':
    try:
        main()
    except Exception:
        import traceback
        text = traceback.format_exc()
        print('::error title=TV candidate publication stopped::' + text.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A'), flush=True)
        raise
