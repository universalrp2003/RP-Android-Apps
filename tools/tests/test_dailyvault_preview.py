"""Owner-approved 0.5 contracts. These are source checks, not device/crypto runtime proof."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'dailyvault/src/main/java/com/universalrp/dailyvault'

class DailyVaultPreviewTest(unittest.TestCase):
    def test_metadata_only_network_no_auth_or_sensitive_permissions(self):
        root = ET.parse(ROOT / 'dailyvault/src/main/AndroidManifest.xml').getroot()
        ns = '{http://schemas.android.com/apk/res/android}'
        self.assertEqual({e.attrib[ns+'name'] for e in root.findall('uses-permission')}, {'android.permission.INTERNET'})
        self.assertEqual(root.find('application').attrib[ns+'allowBackup'], 'false')
        main = (BASE / 'MainActivity.kt').read_text()
        self.assertIn('DailyWebsiteUpdateNotice("dailyvault", tamil)', main)
        self.assertNotIn('BiometricPrompt', main)
        self.assertNotIn('FLAG_SECURE', main)
        self.assertNotIn('model.lock()', main)
        self.assertNotIn('model.unlock(', main)
        notice = (ROOT / 'shared/src/main/java/com/universalrp/updates/DailyWebsiteUpdateNotice.kt').read_text()
        self.assertIn('does not download or install APKs', notice)
        self.assertIn('catalog-v2.json', notice)

    def test_portable_authenticated_encryption_is_unchanged(self):
        source = (BASE / 'VaultCrypto.kt').read_text()
        for value in ['AES/GCM/NoPadding', 'PBKDF2WithHmacSHA256', '210_000', 'updateAAD', 'nextBytes', 'spec.clearPassword()']:
            self.assertIn(value, source)
        self.assertNotIn('AndroidKeyStore', source)
        self.assertIn('0x54,0x44,0x56,0x01', source)

    def test_restore_validates_before_replacing_with_local_key(self):
        source = (BASE / 'VaultModel.kt').read_text()
        restore = source.split('fun restore(', 1)[1].split('private fun obtainLocalKey', 1)[0]
        self.assertLess(restore.index('decode(bytes, key)'), restore.index('val deviceKey'))
        self.assertLess(restore.index('decode(bytes, key)'), restore.index('write(decoded, deviceKey)'))
        self.assertNotIn('convenience.clear()', restore)
        self.assertNotIn('obtainLocalKey()', restore)
        self.assertLess(restore.index('write(decoded, deviceKey)'), restore.index('convenience.save("none", deviceKey)'))
        self.assertIn('key.copyOf()', restore)
        self.assertIn('restored_recovery_needed', restore)
        self.assertIn('AtomicFile', source)
        self.assertIn('vault.failWrite(out)', source)
        self.assertIn('output.size() + n <= VaultCrypto.MAX_FILE', source)

    def test_local_open_never_overwrites_unreadable_existing_file(self):
        source = (BASE / 'VaultModel.kt').read_text()
        opening = source.split('private fun openLocal()', 1)[1].split('fun recoverLegacy(', 1)[0]
        existing = opening.split('if (exists()) {', 1)[1].split('} else {', 1)[0]
        self.assertIn('convenience.read()', existing)
        self.assertIn('decode(vault.openRead()', existing)
        self.assertNotIn('write(', existing)
        self.assertIn('init { openLocal() }', source)
        self.assertIn('SecureRandom().nextBytes', source)
        self.assertIn('convenience.save("none", key)', source)

    def test_legacy_recovery_validates_before_saving_wrapper(self):
        source = (BASE / 'VaultModel.kt').read_text().split('fun recoverLegacy(', 1)[1].split('fun save(', 1)[0]
        self.assertLess(source.index('decode('), source.index('convenience.save('))
        self.assertNotIn('write(', source)
        self.assertIn('key.fill', source)

    def test_export_always_uses_explicit_backup_password(self):
        source = (BASE / 'VaultModel.kt').read_text().split('fun export(', 1)[1].split('fun restore(', 1)[0]
        self.assertIn('val key = text.toCharArray()', source)
        self.assertIn('VaultCrypto.seal(plain, key)', source)
        self.assertLess(source.index('VaultCrypto.seal'), source.index('openOutputStream'))
        self.assertNotIn('vault.openRead()', source)
        self.assertIn('key.fill', source)
        ui = (BASE / 'MainActivity.kt').read_text()
        self.assertIn('password == confirm && password.isNotBlank()', ui)
        self.assertIn('mode != "restore" || accepted', ui)
        self.assertIn('REPLACES all current records', ui)

    def test_accessible_redesign_and_explicit_screenshot_disclosure(self):
        ui = (BASE / 'MainActivity.kt').read_text()
        for token in ['NavigationBarItem(', 'SegmentedButton(', 'DatePickerDialog', 'TimePickerDialog', 'BuildConfig.VERSION_NAME',
                      'isSystemInDarkTheme()', 'contentDescription = addLabel', 'isAppearanceLightNavigationBars = !dark', 'LaunchedEffect(page, notes) { listState.scrollToItem(0) }', 'Save failed. Your draft is still here.', 'Screenshots, recordings',
                      'categoryLabel(', 'clearPasswords()', 'repeatOnLifecycle(Lifecycle.State.STARTED)']:
            self.assertIn(token, ui)
        self.assertNotIn('preview 0.2', ui)
        self.assertIn('category = if (page == 1 && notes) "Note" else "General"', ui)
        source = (BASE / 'VaultModel.kt').read_text()
        persist = source.split('private fun persist(', 1)[1].split('fun export(', 1)[0]
        self.assertLess(persist.index('write(entries, key)'), persist.index('onSuccess()'))

    def test_preview_still_cannot_publish(self):
        flow = (ROOT / '.github/workflows/dailyvault-preview.yml').read_text()
        self.assertIn('contents: read', flow)
        self.assertNotIn('gh release', flow)
        self.assertIn(':dailyvault:testDebugUnitTest', flow)

    def test_existing_local_keystore_namespace_is_preserved(self):
        local = (BASE / 'LocalUnlock.kt').read_text()
        for token in ['AndroidKeyStore', 'AES/GCM/NoPadding', '"local-unlock"', 'daily-vault-local-unlock-v1']:
            self.assertIn(token, local)
        # Local device-only credentials never enter portable file crypto.
        self.assertNotIn('LocalUnlock', (BASE / 'VaultCrypto.kt').read_text())

if __name__ == '__main__': unittest.main()
