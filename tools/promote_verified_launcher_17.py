#!/usr/bin/env python3
"""Owner-approved Launcher1.7 exact-artifact promotion. No APK build or replacement."""
import json
import os
from pathlib import Path
import re
import stat
import subprocess
import time
import zipfile
from promote_verified_health_updates import REPO, api, pages, run, sha
from verify_release_permission_delta import parse_badging, dangerous

BRANCH = 'arena/485365dd-rp-android-apps'
SOURCE = 'c4331d91b9a069d41b391665a178e09f0762c12f'
RUN = 41309213845
JOB = 124855881109
ARTIFACT = 13085047634
ARCHIVE = '24c579711bdd07d75dd7b163a2ad57273dc28d98146e5724f68dd2f8307bc0a9'
DIGEST = '13ce20550e9493f6492a399e5bfad72d8fdd95af9e7d0838b7fd0219f543bfec'
SIZE = 11521755
SIGNER = '7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d'
TAG = 'elderly-launcher-v1.7'
NAME = 'Elderly-Launcher-v1.7.apk'
OLD_TAG = 'elderly-launcher-v1.5'
OLD_NAME = 'Elderly-Launcher-v1.5.apk'
OLD_SHA = 'c4cb41847576be63e78270672fee80b40b872fa5c9a3984482643233988b5f39'


def validate_receipt(receipt):
    assert (receipt['source'], receipt['package'], receipt['version'], receipt['code'], receipt['sha256'], receipt['bytes'], receipt['signer']) == (
        SOURCE, 'com.universalrp.launcher', '1.7', 8, DIGEST, SIZE, SIGNER)
    for gate in ('same_uid', 'private_fixture_preserved', 'old_contact_preferences_preserved',
                 'native_picker_add_open_remove_and_recreate', 'process_restart_preferences_preserved',
                 'font_200_controls_reachable', 'real_media_and_ring_index_changes',
                 'two_column_favourite_tiles', 'old_app_preferences_preserved'):
        assert receipt[gate] is True, gate
    for gate in ('physical_phone_tested', 'hardware_audio_tested', 'play_protect_tested', 'published'):
        assert receipt[gate] is False, gate


def release_snapshot(release):
    return release['id'], sorted((a['id'], a['name'], a['size'], a.get('digest')) for a in release['assets'])


def main():
    assert os.environ.get('GITHUB_ACTIONS') == 'true'
    assert os.environ['GITHUB_REF'] == 'refs/heads/' + BRANCH
    assert os.environ['GITHUB_EVENT_NAME'] == 'push' and os.environ['GH_REPO'] == REPO
    approval = Path('docs/launcher-1.7-owner-acceptance.md').read_text()
    for value in ('Publication authorized: yes', SOURCE, DIGEST, ARCHIVE):
        assert value in approval
    subprocess.run(['git', 'merge-base', '--is-ancestor', SOURCE, 'HEAD'], check=True)
    # Publication/website/gallery work must never substitute a freshly built Launcher.
    subprocess.run(['git', 'diff', '--exit-code', SOURCE, 'HEAD', '--', 'launcher/', 'shared/', 'gradle/',
                    'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew', 'gradlew.bat'], check=True)
    info = api(f'actions/runs/{RUN}')
    assert (info['head_sha'], info['head_branch'], info['status'], info['conclusion'], info['path']) == (
        SOURCE, BRANCH, 'completed', 'success', '.github/workflows/launcher-home-candidate.yml')
    job = api(f'actions/jobs/{JOB}')
    assert job['run_id'] == RUN and job['conclusion'] == 'success'
    for name in ('Source, unit, lint and exact signed release candidate', 'Pin baseline, signer, permissions and candidate hash',
                 'Exact 1.6 update, data retention and native accessible home', 'Upload only the tested signed candidate, not the old baseline APK'):
        assert next(s for s in job['steps'] if s['name'] == name)['conclusion'] == 'success'
    for run_id in (41309213829, 41309213881, 41309213914):
        gate = api(f'actions/runs/{run_id}')
        assert gate['head_sha'] == SOURCE and gate['status'] == 'completed' and gate['conclusion'] == 'success'
    artifact = api(f'actions/artifacts/{ARTIFACT}')
    assert not artifact['expired'] and artifact['workflow_run']['id'] == RUN and artifact['workflow_run']['head_sha'] == SOURCE
    assert artifact['name'] == 'Elderly-Launcher-1.7-signed-tested-candidate'
    assert artifact['digest'] == 'sha256:' + ARCHIVE and artifact['size_in_bytes'] == 11049740
    root = Path('/tmp/publish-launcher17'); root.mkdir()
    archive = root/'candidate.zip'
    with archive.open('wb') as out:
        subprocess.run(['gh', 'api', f'repos/{REPO}/actions/artifacts/{ARTIFACT}/zip'], stdout=out, check=True)
    assert sha(archive) == ARCHIVE and archive.stat().st_size == 11049740
    with zipfile.ZipFile(archive) as z:
        assert len(z.namelist()) == len(set(z.namelist())) == 3
        assert set(z.namelist()) == {NAME, 'Elderly-Launcher-v1.7.sha256', 'verification.json'}
        assert sum(i.file_size for i in z.infolist()) < 20_000_000
        for item in z.infolist():
            assert not Path(item.filename).is_absolute() and '..' not in Path(item.filename).parts
            assert not stat.S_ISLNK(item.external_attr >> 16)
        validate_receipt(json.loads(z.read('verification.json')))
        candidate = root/NAME; candidate.write_bytes(z.read(NAME))
        assert z.read('Elderly-Launcher-v1.7.sha256').decode().strip() == DIGEST + '  ' + NAME
    assert sha(candidate) == DIGEST and candidate.stat().st_size == SIZE
    tools = sorted((Path(os.environ['ANDROID_HOME'])/'build-tools').iterdir(), key=lambda p: tuple(int(n) for n in re.findall(r'\d+', p.name)))[-1]
    badging = run(str(tools/'aapt2'), 'dump', 'badging', str(candidate))
    assert "package: name='com.universalrp.launcher' versionCode='8' versionName='1.7'" in badging
    assert re.search(r"(?:sdkVersion|minSdkVersion):\s*'26'", badging) and 'application-debuggable' not in badging
    cert = run(str(tools/'apksigner'), 'verify', '--print-certs', str(candidate))
    assert set(re.findall(r'certificate SHA-256 digest: ([a-fA-F0-9]+)', cert)) == {SIGNER}
    baseline = root/'baseline'; baseline.mkdir()
    subprocess.run(['gh', 'release', 'download', OLD_TAG, '--pattern', OLD_NAME, '--dir', str(baseline)], check=True)
    assert sha(baseline/OLD_NAME) == OLD_SHA and (baseline/OLD_NAME).stat().st_size == 11498427
    assert SIGNER in run(str(tools/'apksigner'), 'verify', '--print-certs', str(baseline/OLD_NAME))
    previous = run(str(tools/'aapt2'), 'dump', 'badging', str(baseline/OLD_NAME))
    assert parse_badging(badging) == parse_badging(previous) | {'android.permission.MODIFY_AUDIO_SETTINGS'}
    assert dangerous(parse_badging(badging)) == dangerous(parse_badging(previous))
    # Snapshot every prior public APK identity/digest, not only Launcher's baseline.
    releases = pages('releases')
    old = {r['tag_name']: release_snapshot(r) for r in releases if not r['draft']}
    assert not any(r['tag_name'] == TAG for r in releases), 'Never replace or blindly resume a release'
    assert not any(t['name'] == TAG for t in pages('tags')), 'Never replace a tag'
    checksum = root/'Elderly-Launcher-v1.7.sha256'; checksum.write_text(DIGEST+'  '+NAME+'\n')
    subprocess.run(['gh', 'release', 'create', TAG, '--target', SOURCE, '--title', 'Elderly Launcher 1.7',
                    '--notes-file', 'docs/launcher-1.7-public-release-notes.md', '--draft',
                    str(candidate)+'#'+NAME, str(checksum)+'#'+checksum.name], check=True)
    # Drafts have no public tag endpoint; find the authenticated numeric draft ID.
    for attempt in range(12):
        matches = [r for r in pages('releases') if r['tag_name'] == TAG]
        assert len(matches) == 1
        draft = api('releases/'+str(matches[0]['id']))
        assert draft['draft'] and not draft['prerelease'] and draft['target_commitish'] == SOURCE
        assert sorted(a['name'] for a in draft['assets']) == [NAME, checksum.name]
        asset = next(a for a in draft['assets'] if a['name'] == NAME)
        sidecar = next(a for a in draft['assets'] if a['name'] == checksum.name)
        if asset.get('digest') == 'sha256:'+DIGEST and sidecar.get('digest') == 'sha256:'+sha(checksum):
            break
        time.sleep(2)
    else:
        raise AssertionError('Unverified draft; nothing made public')
    assert asset['size'] == SIZE
    for phase in ('draft', 'public'):
        destination = root/phase; destination.mkdir()
        with (destination/NAME).open('wb') as out:
            subprocess.run(['gh', 'api', f'repos/{REPO}/releases/assets/{asset["id"]}', '-H', 'Accept: application/octet-stream'], stdout=out, check=True)
        assert sha(destination/NAME) == DIGEST and (destination/NAME).stat().st_size == SIZE
        if phase == 'draft':
            subprocess.run(['gh', 'release', 'edit', TAG, '--draft=false', '--latest=false'], check=True)
    public = api('releases/tags/'+TAG)
    assert not public['draft'] and not public['prerelease'] and public['id'] == draft['id']
    asset = next(a for a in public['assets'] if a['name'] == NAME)
    assert asset['digest'] == 'sha256:'+DIGEST and asset['size'] == SIZE
    for tag, snapshot in old.items():
        assert release_snapshot(api('releases/tags/'+tag)) == snapshot, tag
    result = dict(tag=TAG, source=SOURCE, run_id=RUN, artifact_id=ARTIFACT, release_id=public['id'], asset_id=asset['id'],
                  published_at=public['published_at'], url=asset['browser_download_url'], bytes=SIZE, sha256=DIGEST, signer=SIGNER,
                  rebuilt=False, owner_acceptance=True, installed_phone_hash_matched=False, play_protect_clearance=False)
    Path(os.environ['RUNNER_TEMP'], 'launcher17-publication-receipt.json').write_text(json.dumps(result, indent=2)+'\n')
    print('::notice title=Published exact Launcher1.7::'+json.dumps(result), flush=True)


if __name__ == '__main__':
    try:
        main()
    except Exception:
        import traceback
        print('::error title=Launcher1.7 promotion stopped::'+traceback.format_exc()[-3500:].replace('%', '%25').replace('\n', '%0A'), flush=True)
        raise
