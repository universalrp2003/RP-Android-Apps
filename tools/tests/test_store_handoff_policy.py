import unittest
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import verify_store_handoff_policy as policy

class StoreHandoffPolicyTest(unittest.TestCase):
    def test_actual_suite_sources(self): policy.verify_sources()
    def test_installer_permission_injection_rejected(self):
        with self.assertRaises(ValueError):
            policy.verify_manifest('<manifest xmlns:android="http://schemas.android.com/apk/res/android"><uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/><application/></manifest>')
    def test_installer_provider_injection_rejected(self):
        with self.assertRaises(ValueError):
            policy.verify_manifest('<manifest xmlns:android="http://schemas.android.com/apk/res/android"><application><provider android:authorities="com.example.updates.files"/></application></manifest>')
    def test_non_update_attachment_provider_is_not_deleted(self):
        policy.verify_manifest('<manifest xmlns:android="http://schemas.android.com/apk/res/android"><application><provider android:authorities="com.example.attachments"/></application></manifest>')

    def test_every_store_route_has_real_website_download_anchor(self):
        import json, re
        router = (policy.ROOT/'shared/src/main/java/com/universalrp/updates/StoreUpdateRouter.kt').read_text()
        aliases = dict(re.findall(r'"([a-z]+)" to "([a-z]+)"', router))
        feed = json.loads((policy.ROOT/'site/catalog.json').read_text())
        site = (policy.ROOT/'site/index.html').read_text()
        for app in feed['apps']:
            self.assertIn('id="st-' + aliases.get(app['id'], app['id']) + '"', site)

    def test_secret_native_gate_does_not_require_retired_install_permission(self):
        build = (policy.ROOT/'secretmessage/wifi-test/build-wifi-test.sh').read_text()
        self.assertNotIn('    android.permission.REQUEST_INSTALL_PACKAGES', build)

    def test_oor_deterministic_gate_is_not_an_external_service_claim(self):
        probe = (policy.ROOT/'oorarivippu/src/test/java/com/oorarivippu/voiceannouncer/NeuralCloudVoiceEngineTest.kt').read_text()
        self.assertIn('System.getenv("RP_RUN_LIVE_SPEECH_TEST") == "true"', probe)
        self.assertNotIn('if (!reachable) return', probe)
        self.assertIn('no successful audio claimed', probe)

    def test_known_certificate_alone_cannot_authorize_store_handoff(self):
        router = (policy.ROOT/'shared/src/main/java/com/universalrp/updates/StoreUpdateRouter.kt').read_text()
        self.assertIn('if (linksSupported && matchesPublishedStore(context)', router)
        self.assertIn('apk.length() != STORE_BYTES', router)
        self.assertIn('digest.digest().joinToString', router)
        self.assertIn('== STORE_APK_SHA256', router)
        self.assertIn('d5f41e7b5142507e8b42fb9d2c4e0e2d492eaf03581a53b3dabacf692daf4bc7', router)

    def test_secret_generated_handoff_changes_trigger_isolated_build(self):
        ci = (policy.ROOT/'.github/workflows/secret-message-wifi-test.yml').read_text()
        self.assertEqual(2, ci.count('"shared/src/main/java/com/universalrp/updates/**"'))

    def test_legacy_store_without_app_deep_link_cannot_claim_app_specific_handoff(self):
        router = (policy.ROOT/'shared/src/main/java/com/universalrp/updates/StoreUpdateRouter.kt').read_text()
        self.assertIn('queryIntentActivities(discovery, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)', router)
        self.assertIn('if (linksSupported && matchesPublishedStore(context)', router)
        self.assertIn('return openWebsite(context, appId)', router)

    def test_cleaner_tamil_launcher_and_widget_labels_use_owner_name(self):
        import xml.etree.ElementTree as ET
        labels = ET.parse(policy.ROOT/'cleaner/src/main/res/values-ta/strings.xml').getroot()
        for item in labels.findall('string'):
            if item.get('name') == 'app_name' or item.get('name', '').startswith('widget_') and item.get('name', '').endswith('_label'):
                self.assertTrue((item.text or '').startswith('கைபேசி காவலன்'), item.get('name'))
