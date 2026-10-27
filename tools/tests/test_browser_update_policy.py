import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from verify_browser_update_policy import ROOT, verify_manifest, verify_apk_permissions, verify_sources


class BrowserUpdatePolicyTest(unittest.TestCase):
    def test_current_sources(self):
        verify_sources()

    def test_reintroduced_install_permission_is_rejected(self):
        source = (ROOT / 'podcast/src/main/AndroidManifest.xml').read_text()
        broken = source.replace('<application', '<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/><application', 1)
        with self.assertRaisesRegex(ValueError, 'package-install'):
            verify_manifest(broken)

    def test_no_permissions_is_not_a_pass(self):
        with self.assertRaisesRegex(ValueError, 'No permissions'):
            verify_manifest('<manifest><application/></manifest>')
        with self.assertRaisesRegex(ValueError, 'No APK permissions'):
            verify_apk_permissions('')

    def test_merged_permission_in_an_apk_is_rejected(self):
        with self.assertRaisesRegex(ValueError, 'Merged APK'):
            verify_apk_permissions("uses-permission: name='android.permission.REQUEST_INSTALL_PACKAGES'")
        verify_apk_permissions("uses-permission: name='android.permission.INTERNET'")

    def test_reintroduced_provider_is_rejected(self):
        source = (ROOT / 'launcher/src/main/AndroidManifest.xml').read_text()
        broken = source.replace('</application>', '<provider android:authorities="com.example.updates.files"/></application>')
        with self.assertRaisesRegex(ValueError, 'provider'):
            verify_manifest(broken)


if __name__ == '__main__':
    unittest.main()
