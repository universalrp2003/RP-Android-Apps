from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

class StoreUpgradeCandidateTest(unittest.TestCase):
    def test_candidate_is_release_signed_non_debug_and_cannot_publish(self):
        workflow = (ROOT / '.github/workflows/tamilanstore-signed-candidate.yml').read_text()
        for token in ('contents: read', 'prepare-release-signing', 'RP_RELEASE_KEYSTORE_BASE64',
                      ':tamilanstore:testReleaseUnitTest', ':tamilanstore:lintRelease', ':tamilanstore:assembleRelease',
                      'application-debuggable', "versionCode='10' versionName='0.9-rc1'",
                      '7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d',
                      '3fae24373a6ead0bab0c0b2ad56a80a3b191de7a44ca69b50561deac3eea8474',
                      'Tamilan-Store-0.9-rc1-release-key-upgrade-candidate', 'sha256sum --check', 'old == new'):
            self.assertIn(token, workflow)
        self.assertNotIn('contents: write', workflow)
        self.assertNotIn('gh release', workflow)
        self.assertNotIn('git push', workflow)
        self.assertNotIn('pull_request:', workflow)
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
