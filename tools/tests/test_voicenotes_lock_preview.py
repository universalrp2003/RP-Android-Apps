"""Source guardrails only; Android/device tests remain necessary."""
import unittest
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]

class VoiceNotesLockPreviewTest(unittest.TestCase):
    def test_lock_is_opt_in_and_authenticated(self):
        ui = (ROOT/'voicenotes/src/main/java/com/universalrp/voicenotes/MainActivity.kt').read_text()
        self.assertIn('getBoolean("app_lock", false)', ui)
        self.assertIn('if (lockEnabled && !unlocked)', ui)
        self.assertIn('authenticate {\n                                lockEnabled = !lockEnabled', ui)
        self.assertIn('BiometricManager.Authenticators.DEVICE_CREDENTIAL', ui)
        self.assertIn('speaker.stop()', ui)
        self.assertIn('unlocked = false', ui)
        self.assertIn('not note-file encryption', ui)
        self.assertIn('Our apps / Website', ui)

    def test_fragment_dependency_and_preview_only(self):
        for module in ['dailyvault','voicenotes']:
            self.assertIn('androidx.fragment:fragment-ktx:1.8.6', (ROOT/module/'build.gradle.kts').read_text())
        flow = (ROOT/'.github/workflows/voicenotes-preview.yml').read_text()
        self.assertIn('contents: read', flow)
        self.assertIn(':voicenotes:lintDebug', flow)
        self.assertNotIn('gh release', flow)
