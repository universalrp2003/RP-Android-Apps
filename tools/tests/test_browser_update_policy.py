import sys
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from verify_browser_update_policy import (
    ROOT, verify_manifest, verify_apk_permissions, verify_sources, verify_apk_identity,
    verify_entry, verify_workflow,
)


class BrowserUpdatePolicyTest(unittest.TestCase):
    def test_browser_update_apps_keep_metadata_check_and_remove_install_capabilities(self):
        for module, entry in {
            'equalizer': 'AppUpdates.Control("PulseEQ", automatic = true)',
            'podcast': 'AppUpdates.Control("Tamil-Podcast-Player", automatic = true)',
            'launcher': 'AppUpdates.Control("Elderly-Launcher", automatic = true)',
        }.items():
            verify_entry(module, entry)
            with self.assertRaises(ValueError):
                verify_entry(module, '')
            source = (ROOT / module / 'src/main/AndroidManifest.xml').read_text()
            self.assertIn('android.permission.INTERNET', source)
            for permission in ['REQUEST_INSTALL_PACKAGES', 'INSTALL_PACKAGES']:
                bad = source.replace('<application', '<uses-permission android:name="android.permission.' + permission + '"/><application', 1)
                with self.assertRaises(ValueError):
                    verify_manifest(bad)
            bad = source.replace('</application>', '<provider android:authorities="com.example.updates.files"/></application>')
            with self.assertRaises(ValueError):
                verify_manifest(bad)

    def test_broad_apk_gate_checks_exactly_one_real_pulseeq_apk(self):
        source = (ROOT / '.github/workflows/build-apk.yml').read_text()
        verify_workflow(source)
        with self.assertRaises(ValueError):
            verify_workflow('')
        with self.assertRaises(ValueError):
            verify_workflow(source.replace('python3 tools/verify_browser_update_policy.py --apk "$EQ"', 'echo skipped'))

    def test_actual_apk_version_must_match_source(self):
        gradle = 'applicationId = "com.example.app"\nversionName = "1.4"\nversionCode = 6'
        good = "package: name='com.example.app' versionCode='6' versionName='1.4' platformBuildVersionName='16'"
        self.assertEqual('1.4', verify_apk_identity(good, gradle)['versionName'])
        with self.assertRaisesRegex(ValueError, 'differs'):
            verify_apk_identity(good.replace("versionCode='6'", "versionCode='5'"), gradle)
        with self.assertRaisesRegex(ValueError, 'exactly one'):
            verify_apk_identity(good, '')

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

    def test_merged_package_install_permission_is_rejected_but_internet_is_valid(self):
        with self.assertRaisesRegex(ValueError, 'Merged APK'):
            verify_apk_permissions("uses-permission: name='android.permission.REQUEST_INSTALL_PACKAGES'")
        verify_apk_permissions("uses-permission: name='android.permission.INTERNET'")

    def test_offline_podcast_files_and_device_specific_receipts_are_excluded_from_backups(self):
        resources = ROOT / 'podcast/src/main/res/xml'
        required = {('external', 'Podcasts/offline/'), ('sharedpref', 'podcast_offline.xml')}
        legacy = ET.fromstring((resources / 'backup_rules.xml').read_text())
        modern = ET.fromstring((resources / 'data_extraction_rules.xml').read_text())
        for section in (legacy, modern.find('cloud-backup'), modern.find('device-transfer')):
            self.assertIsNotNone(section)
            exclusions = {(node.get('domain'), node.get('path')) for node in section.findall('exclude')}
            self.assertTrue(required.issubset(exclusions))
        manifest = (ROOT / 'podcast/src/main/AndroidManifest.xml').read_text()
        self.assertIn('android:fullBackupContent="@xml/backup_rules"', manifest)
        self.assertIn('android:dataExtractionRules="@xml/data_extraction_rules"', manifest)

    def test_reintroduced_provider_is_rejected(self):
        source = (ROOT / 'launcher/src/main/AndroidManifest.xml').read_text()
        broken = source.replace('</application>', '<provider android:authorities="com.example.updates.files"/></application>')
        with self.assertRaisesRegex(ValueError, 'provider'):
            verify_manifest(broken)


if __name__ == '__main__':
    unittest.main()
