"""Source regressions complement (not replace) executable Kotlin failover tests."""
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]

class HdVoicePolicyTest(unittest.TestCase):
    def test_both_engines_use_one_shared_policy_without_recursive_same_voice_retry(self):
        for relative in ['shared/src/main/java/com/universalrp/voice/SharedNeuralVoiceEngine.kt',
                         'smsguard/src/main/java/com/universalrp/smsguard/service/NeuralCloudVoiceEngine.kt']:
            source = (ROOT / relative).read_text()
            self.assertIn('NeuralVoiceFailover.synthesize(', source)
            self.assertEqual(2, source.count('streamWebSocketOnce('), relative)
            self.assertNotIn('TRANSIENT_SERVER_RETRY', source)
            self.assertNotIn('NeuralRetryPolicy.remainingBudget', source)
            self.assertIn('Thread.currentThread().interrupt()', source)
            self.assertIn('$selectedVoice', source)
            curated = source.split('tamilNeuralId =')[1:]
            self.assertTrue(curated)
            self.assertTrue(all(not p.lstrip().startswith('"ta-IN-ValluvarNeural"') for p in curated))

    def test_probe_remains_direct_and_recovery_is_separately_excluded_from_unit_tasks(self):
        source = (ROOT / 'smsguard/src/test/java/com/universalrp/smsguard/SmsGuardEngineTest.kt').read_text()
        self.assertIn('allowVoiceFallback = false', source)
        build = (ROOT / 'smsguard/build.gradle.kts').read_text()
        self.assertIn('if (name != "testHdVoiceFailover") filter { excludeTestsMatching(hdFailoverMethod) }', build)
        self.assertIn('includeTestsMatching(hdFailoverMethod)', build)

    def test_expense_about_keeps_its_offline_permission_boundary(self):
        main = (ROOT / 'expensetracker/src/main/java/com/universalrp/expensetracker/MainActivity.kt').read_text()
        self.assertIn('SuiteAbout("expensetracker", "Expense-Tracker")', main)
        manifest = (ROOT / 'expensetracker/src/main/AndroidManifest.xml').read_text()
        permissions = {p.get('{http://schemas.android.com/apk/res/android}name') for p in ET.fromstring(manifest).findall('uses-permission')}
        self.assertNotIn('android.permission.INTERNET', permissions)
        self.assertNotIn('android.permission.REQUEST_INSTALL_PACKAGES', permissions)
        suite = (ROOT / 'shared/src/main/java/com/universalrp/updates/SuiteAbout.kt').read_text()
        self.assertIn('setOf("podcast", "launcher", "expensetracker")', suite)

    def test_published_store_daily_links_and_website_integrity_failure_stop(self):
        site = (ROOT/'site/index.html').read_text()
        for anchor in ['st-tamilanstore','st-dailyvault']:
            self.assertIn('id="'+anchor+'"',site)
        self.assertNotIn('vasanthi-ai-v1.2.4',site)
        self.assertIn("if (err.message === 'SHA-256 mismatch')",site)
        self.assertIn('Integrity failure must never become an unchecked direct download',site)
