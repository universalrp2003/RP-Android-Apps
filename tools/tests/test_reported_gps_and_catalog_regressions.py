"""Source regression guards; not a substitute for Kotlin/native/network/device tests."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
class ReportedPhoneRegressions(unittest.TestCase):
    def test_receiver_stop_retains_report_but_lifecycle_still_clears_it(self):
        s=(ROOT/'oorarivippu/src/main/java/com/oorarivippu/voiceannouncer/LocationDiagnostics.kt').read_text()
        stop=s.split('override fun onStopped()',1)[1].split('override fun onSatelliteStatusChanged',1)[0]
        self.assertNotIn('snapshot = null',stop)
        self.assertIn('val current = snapshot\n',s)
        self.assertIn('seconds old · not live',s)
        self.assertIn('snapshot = null',s.split('fun unregister()',1)[1].split('fun registerIfVisible',1)[0])
        probe=(ROOT/'oorarivippu/src/main/java/com/oorarivippu/voiceannouncer/GpsDiagnosticProbe.kt').read_text()
        self.assertIn('DURATION_MS = 30_000L',probe)
        self.assertIn('setDurationMillis(DURATION_MS)',probe)
    def test_refresh_bypasses_stale_http_cache_without_relaxing_validation(self):
        base=ROOT/'tamilanstore/src/main/java/com/universalrp/tamilanstore'
        s=(base/'LiveCatalog.kt').read_text()
        for text in ('?refresh=', 'connection.useCaches = false', 'no-cache, no-store, max-age=0',
                     'connection.instanceFollowRedirects = false', 'readBytesLimited(256 * 1024)',
                     'app.sha256 == previous.sha256', 'app.packageName == previous.packageName'):
            self.assertIn(text,s)
        ui=(base/'MainActivity.kt').read_text();model=(base/'StoreModel.kt').read_text()
        self.assertIn('Refresh catalog',ui)
        self.assertIn('Online catalog rejected by validation',ui)
        self.assertIn('model.refreshCatalog()',ui)
        self.assertIn('catalogMessage = "rejected"',model)
        self.assertIn('LiveCatalog.validate(text, apps)',model)
