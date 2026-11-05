#!/usr/bin/env python3
"""Owner-authorized Oor Arivippu only: promote pinned verified bytes, never rebuild/overwrite."""
import json
import os
from pathlib import Path
import re
import stat
import subprocess
import time
import zipfile
from promote_verified_health_updates import REPO, api, pages, run, sha
from verify_release_permission_delta import parse_badging

BRANCH = 'arena/e3d867e2-rp-android-apps'
SIGNER = '7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d'
TAG = 'oor-arivippu-v1.1.17'
NAME = 'Oor-Arivippu-v1.1.17.apk'
OLD_TAG = 'oor-arivippu-v1.1.16'
OLD_NAME = 'Oor-Arivippu-v1.1.16.apk'
OLD_SHA = '3d7119ae7d843ee57c4ca2a69df5ef6bfedf4e90210af20ceb4e74a6f5b42fee'
CANDIDATE_NAME = 'Oor-Arivippu-v1.1.17.apk'
STEPS = ('Source contracts and existing configured service',
         'Unit tests, release lint and signed target/native assembly',
         'Compare actual package, established signer and complete permissions to public 1.1.16',
         'Exact public update, encrypted fixture and bilingual home at font 100 and 200 percent')


def validate_receipt(r, p):
    assert (r['source'], r['package'], r['version'], r['code'], r['sha256'], r['bytes'], r['signer']) == (
        p['source'], 'com.oorarivippu.voiceannouncer', '1.1.17', 22, p['apk_sha256'], p['bytes'], SIGNER)
    for gate in ('same_uid', 'data_preserved', 'preferences_preserved', 'restart_passed', 'bilingual_family_consent_rendered', 'opening_panel_did_not_connect_or_share', 'exact_installed_apk_verified', 'baseline_family_fixture_seeded', 'encrypted_family_fixture_ciphertext_preserved', 'encrypted_family_fixture_decrypted_after_upgrade', 'bilingual_home_history_rendered', 'font100_and200_rendered'):
        assert r[gate] is True, gate
    assert r['apk']==CANDIDATE_NAME and r['native_trip_tests']==1
    assert r['added_permissions'] == r['removed_permissions'] == []
    assert r['baseline_tag'] == OLD_TAG and r['baseline_sha256'] == OLD_SHA
    assert r['physical_phone_tested'] is False and r['play_protect_tested'] is False


def snapshot(r):
    return r['id'], sorted((a['id'], a['name'], a['size'], a.get('digest')) for a in r['assets'])


def main():
    assert os.environ.get('GITHUB_ACTIONS') == 'true' and os.environ['GITHUB_REF'] == 'refs/heads/'+BRANCH
    assert os.environ['GITHUB_EVENT_NAME'] == 'push' and os.environ['GH_REPO'] == REPO
    p = json.loads(Path('docs/oor-trip-1.1.17-publication-pins.json').read_text())
    assert p['publication_authorized'] is True and p['only_app'] == 'oorarivippu'
    assert p['scoped_corrections_authorized'] is True and p['accepted_features_preserved'] is True
    assert p['new_phone_test_required'] is False
    for key in ('source',): assert re.fullmatch('[a-f0-9]{40}', p[key])
    for key in ('apk_sha256', 'archive_sha256'): assert re.fullmatch('[a-f0-9]{64}', p[key])
    assert p['required_android35_native_tests'] == 1
    source = p['source']
    subprocess.run(['git', 'merge-base', '--is-ancestor', source, 'HEAD'], check=True)
    subprocess.run(['git', 'diff', '--exit-code', source, 'HEAD', '--', 'oorarivippu/', 'shared/', 'gradle/',
                    'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew', 'gradlew.bat',
                    'tools/inspect_oor_trip_candidate.py', 'tools/oor_trip_upgrade_smoke.py',
                    'tools/run_oor_trip_native.py', '.github/workflows/oor-trip-candidate.yml'], check=True)
    info = api(f'actions/runs/{p["run_id"]}')
    assert (info['head_sha'], info['head_branch'], info['status'], info['conclusion'], info['path']) == (
        source, BRANCH, 'completed', 'success', '.github/workflows/oor-trip-candidate.yml')
    job = api(f'actions/jobs/{p["job_id"]}')
    assert job['run_id'] == p['run_id'] and job['conclusion'] == 'success'
    for name in STEPS: assert next(s for s in job['steps'] if s['name'] == name)['conclusion'] == 'success', name
    artifact = api(f'actions/artifacts/{p["artifact_id"]}')
    assert not artifact['expired'] and artifact['workflow_run']['id'] == p['run_id']
    assert artifact['workflow_run']['head_sha'] == source
    assert artifact['name'] == 'Oor-1.1.17-exact-upgrade-candidate'
    assert artifact['digest'] == 'sha256:'+p['archive_sha256'] and artifact['size_in_bytes'] < 50_000_000
    root = Path('/tmp')/'promote-oor-117'; root.mkdir()
    archive = root/'candidate.zip'
    with archive.open('wb') as stream:
        subprocess.run(['gh', 'api', f'repos/{REPO}/actions/artifacts/{p["artifact_id"]}/zip'], stdout=stream, check=True)
    assert sha(archive) == p['archive_sha256']
    with zipfile.ZipFile(archive) as z:
        assert sum(i.file_size for i in z.infolist()) < 80_000_000
        for item in z.infolist():
            assert not Path(item.filename).is_absolute() and '..' not in Path(item.filename).parts
            assert not stat.S_ISLNK(item.external_attr >> 16)
        assert [n for n in z.namelist() if n.endswith('.apk')] == [CANDIDATE_NAME]
        receipt = json.loads(z.read('verification.json')); validate_receipt(receipt, p)
        raw=z.read('trip-native.txt').decode(); assert 'OK (1 test)' in raw and 'INSTRUMENTATION_CODE: -1' in raw
        seed=z.read('baseline-family-fixture-instrumentation.txt').decode(); assert 'OK (1 test)' in seed and 'INSTRUMENTATION_CODE: -1' in seed
        candidate = root/NAME; candidate.write_bytes(z.read(CANDIDATE_NAME))
    assert sha(candidate) == p['apk_sha256'] and candidate.stat().st_size == p['bytes']
    tools = sorted((Path(os.environ['ANDROID_HOME'])/'build-tools').iterdir(), key=lambda d: tuple(int(n) for n in re.findall(r'\d+', d.name)))[-1]
    badging = run(str(tools/'aapt2'), 'dump', 'badging', str(candidate))
    assert "package: name='com.oorarivippu.voiceannouncer' versionCode='22' versionName='1.1.17'" in badging
    assert re.search(r"(?:sdkVersion|minSdkVersion):\s*'26'", badging) and 'application-debuggable' not in badging
    certs = run(str(tools/'apksigner'), 'verify', '--print-certs', str(candidate))
    assert set(re.findall(r'certificate SHA-256 digest: ([a-fA-F0-9]+)', certs)) == {SIGNER}
    baseline = root/'baseline'; baseline.mkdir()
    subprocess.run(['gh', 'release', 'download', OLD_TAG, '--pattern', OLD_NAME, '--dir', str(baseline)], check=True)
    assert sha(baseline/OLD_NAME) == OLD_SHA
    certs = run(str(tools/'apksigner'), 'verify', '--print-certs', str(baseline/OLD_NAME))
    assert set(re.findall(r'certificate SHA-256 digest: ([a-fA-F0-9]+)', certs)) == {SIGNER}
    previous = run(str(tools/'aapt2'), 'dump', 'badging', str(baseline/OLD_NAME))
    assert parse_badging(badging) == parse_badging(previous) == set(receipt['permissions'])
    new_file = root/'candidate-badging.txt'; old_file = root/'previous-badging.txt'
    new_file.write_text(badging); old_file.write_text(previous)
    assert 'EVIDENCE-REQUIRED: no' in run('python3', 'tools/verify_release_permission_delta.py',
        '--candidate-badging', str(new_file), '--previous-badging', str(old_file))
    before = pages('releases')
    prior = {r['tag_name']: snapshot(r) for r in before if not r['draft']}
    existing=[r for r in before if r['tag_name']==TAG]
    if p.get('resume_draft_id'):
        assert len(existing)==1 and existing[0]['id']==p['resume_draft_id'] and existing[0]['draft'], 'Only explicitly pinned exact draft may resume'
        pinned=api('releases/'+str(p['resume_draft_id']))
        assert pinned['draft'] and not pinned['prerelease'] and pinned['target_commitish']==source
        assert {a['id'] for a in pinned['assets']}=={p['resume_apk_asset_id'],p['resume_checksum_asset_id']}
    else:
        assert not existing, 'Never overwrite or blindly resume release'
    assert not any(t['name'] == TAG for t in pages('tags')), 'Never replace tag'
    checksum = root/'Oor-Arivippu-v1.1.17.sha256'; checksum.write_text(p['apk_sha256']+'  '+NAME+'\n')
    if not p.get('resume_draft_id'): subprocess.run(['gh', 'release', 'create', TAG, '--target', source, '--title', 'Oor Arivippu 1.1.17 — compact travel home, speed and distance',
                    '--notes-file', 'releases/'+TAG+'.md', '--draft', str(candidate)+'#'+NAME, str(checksum)+'#'+checksum.name], check=True)
    for attempt in range(12):
        if p.get('resume_draft_id'):
            draft=api('releases/'+str(p['resume_draft_id']))
        else:
            matches = [r for r in pages('releases') if r['tag_name'] == TAG]
            if not matches: time.sleep(2); continue
            assert len(matches)==1
            draft = api('releases/'+str(matches[0]['id']))
        assert draft['draft'] and not draft['prerelease'] and draft['target_commitish'] == source
        assert sorted(a['name'] for a in draft['assets']) == [NAME, checksum.name]
        asset = next(a for a in draft['assets'] if a['name'] == NAME)
        sidecar = next(a for a in draft['assets'] if a['name'] == checksum.name)
        if asset.get('digest') == 'sha256:'+p['apk_sha256'] and sidecar.get('digest') == 'sha256:'+sha(checksum): break
        time.sleep(2)
    else: raise AssertionError('Unverified draft, nothing public')
    assert asset['size'] == p['bytes']
    for phase in ('draft', 'public'):
        directory = root/phase; directory.mkdir()
        with (directory/NAME).open('wb') as stream:
            subprocess.run(['gh', 'api', f'repos/{REPO}/releases/assets/{asset["id"]}', '-H', 'Accept: application/octet-stream'], stdout=stream, check=True)
        assert sha(directory/NAME) == p['apk_sha256'] and (directory/NAME).stat().st_size == p['bytes']
        if phase == 'draft': subprocess.run(['gh', 'release', 'edit', TAG, '--draft=false', '--latest=false'], check=True)
    for attempt in range(12):
        public = api('releases/'+str(draft['id']))
        asset = next(a for a in public['assets'] if a['name'] == NAME)
        if not public['draft'] and asset['browser_download_url']==f'https://github.com/{REPO}/releases/download/{TAG}/{NAME}': break
        time.sleep(2)
    else: raise AssertionError('Published state not yet independently verified; do not rerun publication blindly')
    assert not public['draft'] and not public['prerelease'] and public['id'] == draft['id']
    assert asset['digest'] == 'sha256:'+p['apk_sha256'] and asset['size'] == p['bytes']
    assert asset['browser_download_url'] == f'https://github.com/{REPO}/releases/download/{TAG}/{NAME}'
    after = {r['tag_name']: snapshot(r) for r in pages('releases') if not r['draft']}
    for tag, old in prior.items(): assert after[tag] == old, tag
    result = dict(p, tag=TAG, release_id=public['id'], asset_id=asset['id'], published_at=public['published_at'],
        url=asset['browser_download_url'], signer=SIGNER, rebuilt=False, physical_phone_tested=False,
        installed_phone_hash_matched=False, play_protect_clearance=False, prior_public_assets_unchanged=True, owner_reports_working=False, physical_phone_hash_independently_verified=False)
    Path(os.environ['RUNNER_TEMP'], 'oor117-publication-receipt.json').write_text(json.dumps(result, indent=2)+'\n')
    print('::notice title=Published exact Oor1.1.17::'+json.dumps(result), flush=True)

if __name__ == '__main__':
    try: main()
    except Exception:
        import traceback
        print('::error title=Oor1.1.17 promotion stopped::'+traceback.format_exc()[-3500:].replace('%','%25').replace('\n','%0A'), flush=True)
        raise
