"""Source boundaries only; Kotlin arithmetic and UI still require CI/device tests."""
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'oorarivippu/src/main/java/com/oorarivippu/voiceannouncer'
class TripProgressContracts(unittest.TestCase):
    def test_local_session_only_progress_and_home_history(self):
        ui = (SRC/'TripProgress.kt').read_text()
        self.assertIn('Straight-line remaining', ui)
        self.assertIn('Estimate unavailable', ui)
        self.assertIn('Lifecycle.State.RESUMED', ui)
        self.assertNotIn('requestLocationUpdates', ui)
        service = (SRC/'LocationAnnouncementService.kt').read_text()
        self.assertIn('location.hasSpeedAccuracy()', service)
        self.assertIn('location.elapsedRealtimeNanos / 1_000_000L', service)
        main = (SRC/'MainActivity.kt').read_text()
        self.assertLess(main.index('text = ui.historySectionTitle'), main.index('private fun OorSettingsFullDialog'))
        self.assertEqual(main.count('text = ui.historySectionTitle'), 1)
        family = (SRC/'family/FamilyPanel.kt').read_text()
        self.assertNotIn('பாதுகாப்பாக உள்ளார் என்பதற்கான உறுதி அல்ல', family)
        self.assertIn('do not confirm safety', family)
