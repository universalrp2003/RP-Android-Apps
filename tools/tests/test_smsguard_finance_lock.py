"""Source regression checks; Android authentication still needs device testing."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]/'smsguard/src/main/java/com/universalrp/smsguard'
class FinanceLockSecurityTest(unittest.TestCase):
    def test_disable_and_password_changes_require_auth(self):
        vm=(ROOT/'ui/MainViewModel.kt').read_text()
        self.assertIn('_financeUnlockedInSession.value = !enabled',vm)
        self.assertIn('if (!enabled && !_financeUnlockedInSession.value) return',vm)
        self.assertIn('if (prefsFlow.value.financeLockEnabled && !_financeUnlockedInSession.value)',vm)
        controls=(ROOT/'ui/ControlsTab.kt').read_text()
        self.assertIn('onCheckedChange = onFinanceLockChange',controls)
        self.assertNotIn('vm.setFinanceLockEnabled(it)',controls)
        activity=(ROOT/'MainActivity.kt').read_text()
        self.assertIn('finishFinanceAuthentication()',activity)
        self.assertIn('override fun onAuthenticationError',activity)
