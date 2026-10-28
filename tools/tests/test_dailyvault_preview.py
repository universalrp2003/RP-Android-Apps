from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'dailyvault/src/main/java/com/universalrp/dailyvault'
class DailyVaultPreviewTest(unittest.TestCase):
    def test_no_network_or_sensitive_permissions(self):
        root = ET.parse(ROOT / 'dailyvault/src/main/AndroidManifest.xml').getroot()
        self.assertEqual(root.findall('uses-permission'), [])
        ns='{http://schemas.android.com/apk/res/android}'
        self.assertEqual(root.find('application').attrib[ns+'allowBackup'], 'false')
    def test_portable_authenticated_encryption(self):
        s=(BASE/'VaultCrypto.kt').read_text()
        for value in ['AES/GCM/NoPadding','PBKDF2WithHmacSHA256','210_000','updateAAD','nextBytes','spec.clearPassword()']:
            self.assertIn(value,s)
        self.assertNotIn('AndroidKeyStore',s)
    def test_restore_validates_before_replacing(self):
        s=(BASE/'VaultModel.kt').read_text()
        self.assertLess(s.index('val decoded=decode(bytes,key)'),s.index('write(decoded,key)'))
        self.assertIn('AtomicFile',s)
        self.assertIn('vault.failWrite(out)',s)
        self.assertIn('output.size()+n<=VaultCrypto.MAX_FILE',s)
    def test_locks_on_background_and_does_not_publish(self):
        s=(BASE/'MainActivity.kt').read_text()
        self.assertIn('override fun onStop() { clearSensitive();model.lock()',s)
        self.assertIn('FLAG_SECURE',s)
        flow=(ROOT/'.github/workflows/dailyvault-preview.yml').read_text()
        self.assertIn('contents: read',flow)
        self.assertNotIn('gh release',flow)
        self.assertIn(':dailyvault:testDebugUnitTest',flow)
if __name__=='__main__': unittest.main()
