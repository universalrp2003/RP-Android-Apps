from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

class StoreUpgradeCandidateTest(unittest.TestCase):
    def test_candidate_is_release_signed_non_debug_and_cannot_publish(self):
        # This used to assert against tamilanstore-signed-candidate.yml, which pinned the 0.11
        # identities. Store moved to 0.12 / versionCode 14 in that workflow's successor,
        # store-012-candidate.yml, which left the old file asserting a version the module no
        # longer builds - a guaranteed red run on every Store push. The invariants below are
        # the ones that actually matter, and they are checked against the live workflow.
        workflow = (ROOT / '.github/workflows/store-012-candidate.yml').read_text()
        for token in ('contents: read', 'prepare-release-signing', 'RP_RELEASE_KEYSTORE_BASE64',
                      ':tamilanstore:testReleaseUnitTest', ':tamilanstore:lintRelease', ':tamilanstore:assembleRelease',
                      'application-debuggable', "versionCode='14' versionName='0.12'",
                      '7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d',
                      'sha256sum --check', 'old == new'):
            self.assertIn(token, workflow)
        self.assertNotIn('contents: write', workflow)
        self.assertNotIn('gh release', workflow)
        self.assertNotIn('git push', workflow)
        self.assertNotIn('pull_request:', workflow)

    def test_superseded_candidate_workflow_cannot_run_or_cancel_the_live_one(self):
        # Two workflows cannot share a display name or a concurrency group: the stale one
        # shared `store-signed-candidate-${{ github.ref }}` with store-012-candidate.yml, so a
        # Store push could cancel the candidate that is actually tested and promoted.
        self.assertFalse((ROOT / '.github/workflows/tamilanstore-signed-candidate.yml').exists())
        live = (ROOT / '.github/workflows/store-012-candidate.yml').read_text()
        self.assertIn('group: store-signed-candidate-${{ github.ref }}', live)
        names, groups = {}, {}
        for path in sorted((ROOT / '.github/workflows').glob('*.yml')):
            text = path.read_text()
            import re
            name = re.search(r'^name:\s*(.+)$', text, re.M)
            group = re.search(r'^\s*group:\s*(.+)$', text, re.M)
            if name:
                self.assertNotIn(name.group(1).strip(), names,
                                 f'{path.name} duplicates the workflow name of {names.get(name.group(1).strip())}')
                names[name.group(1).strip()] = path.name
            if group:
                groups.setdefault(group.group(1).strip(), []).append(path.name)
        # Different branches, so these two promotion workflows can never be queued together.
        for group, files in groups.items():
            if len(files) > 1:
                triggers = [(ROOT / '.github/workflows' / f).read_text() for f in files]
                self.assertTrue(all('branches: ["arena/' in t for t in triggers), group)
    def test_upgrade_probe_is_guarded_and_preserves_state_without_reset(self):
        smoke = (ROOT / 'tamilanstore/tools/signed-upgrade-smoke.py').read_text()
        for token in ("os.environ.get('CI') == 'true'", "os.environ.get('GITHUB_ACTIONS') == 'true'",
                      "startswith('emulator-')", "'ro.kernel.qemu'", "adb('install', '-r', str(candidate))",
                      "new['uid'] == old['uid']", 'marker_hash', "prefs().get('tamil') == 'false'",
                      "prefs().get('store_motion_v1') == 'false'", 'play_protect_tested=False', 'INSTALL_FAILED_UPDATE_INCOMPATIBLE'):
            self.assertIn(token, smoke)
        for forbidden in ("adb('uninstall'", "'pm', 'clear'", "'install', '-d'", "'install', '-t'"):
            self.assertNotIn(forbidden, smoke)
    def test_debug_build_is_clearly_distinguished_without_changing_release_identity(self):
        ui = (ROOT / 'tamilanstore/src/main/java/com/universalrp/tamilanstore/MainActivity.kt').read_text()
        self.assertIn('ApplicationInfo.FLAG_DEBUGGABLE', ui)
        self.assertIn('Debug preview · cannot update release Store', ui)
        self.assertIn('வெளியீட்டு அங்காடியை இதனால் புதுப்பிக்க முடியாது', ui)
        workflow = (ROOT / '.github/workflows/tamilanstore-preview.yml').read_text()
        self.assertIn('name: tamilan-store-debug-emulator-only', workflow)
        build = (ROOT / 'tamilanstore/build.gradle.kts').read_text()
        self.assertIn('applicationId = "com.universalrp.tamilanstore"', build)
        self.assertNotIn('applicationIdSuffix', build)

class StoreUpgradeProbeSafetyTest(unittest.TestCase):
    @staticmethod
    def probe():
        import importlib.util
        spec = importlib.util.spec_from_file_location('store_upgrade_probe', ROOT/'tamilanstore/tools/signed-upgrade-smoke.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        return module

    def test_non_ci_environment_never_contacts_a_device(self):
        from unittest.mock import patch
        probe = self.probe()
        with patch.dict('os.environ', {'CI': 'false', 'GITHUB_ACTIONS': 'true'}), patch.object(probe, 'adb') as adb:
            with self.assertRaises(AssertionError):
                probe.run(Path('/tmp/unused-synthetic-output'))
            adb.assert_not_called()

    def test_physical_device_serial_stops_before_root_or_install(self):
        from unittest.mock import patch
        probe = self.probe()
        with patch.dict('os.environ', {'CI': 'true', 'GITHUB_ACTIONS': 'true'}), patch.object(probe, 'adb', return_value='physical-fixture-serial') as adb:
            with self.assertRaises(AssertionError):
                probe.run(Path('/tmp/unused-synthetic-output'))
            adb.assert_called_once_with('get-serialno')

    def test_non_qemu_device_stops_before_root_or_install(self):
        from unittest.mock import patch, call
        probe = self.probe()
        with patch.dict('os.environ', {'CI': 'true', 'GITHUB_ACTIONS': 'true'}), patch.object(probe, 'adb', side_effect=['emulator-5554', '0']) as adb:
            with self.assertRaises(AssertionError):
                probe.run(Path('/tmp/unused-synthetic-output'))
            self.assertEqual(adb.call_args_list, [call('get-serialno'), call('shell', 'getprop', 'ro.kernel.qemu')])

    def test_adbd_restart_error_is_not_mistaken_for_an_upgrade_failure(self):
        from unittest.mock import patch
        import subprocess
        probe = self.probe()
        # A failed root command can mean that adbd is restarting. The UID, not exit status,
        # decides readiness; no installation operation is involved in this helper.
        with patch.object(probe, 'adb', side_effect=[subprocess.CalledProcessError(1, ['adb','root'], output=b'restarting'), '', '0']) as adb, patch.object(probe.time, 'sleep'):
            probe.enable_ci_root()
            self.assertEqual(adb.call_count, 3)
            self.assertEqual(adb.call_args_list[-1].args, ('shell', 'id', '-u'))

    def test_root_readiness_is_bounded_and_fails_closed(self):
        from unittest.mock import patch
        probe = self.probe()
        with patch.object(probe, 'adb', side_effect=['', '', '2000'] * 5) as adb, patch.object(probe.time, 'sleep'):
            with self.assertRaisesRegex(AssertionError, 'five bounded attempts'):
                probe.enable_ci_root()
            self.assertEqual(adb.call_count, 15)
            self.assertFalse(any('install' in call.args for call in adb.call_args_list))

    def test_package_evidence_supports_old_and_new_dumpsys_uid_labels(self):
        probe = self.probe()
        for label in ('userId', 'appId'):
            dump = f'{label}=10123\nversionCode=8 minSdk=26 targetSdk=36\nversionName=0.8\n'
            result = probe.parse_package_state(dump, 'package:com.universalrp.tamilanstore uid:10123\n')
            self.assertEqual(result, {'code':8, 'name':'0.8', 'uid':'10123'})

    def test_package_evidence_does_not_guess_missing_code_name_or_exact_uid(self):
        probe = self.probe()
        valid = 'versionCode=10\nversionName=0.9-rc1\n'
        listing = 'package:com.universalrp.tamilanstore uid:10123\n'
        for dump, entries in ((valid, ''), (valid, 'package:com.universalrp.tamilanstore.other uid:10123'),
                              ('versionName=0.9-rc1', listing), ('versionCode=10', listing)):
            with self.assertRaises(AssertionError):
                probe.parse_package_state(dump, entries)
