"""Source boundaries only; native/JVM and phone behavior need their own evidence."""
from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[2]
S=R/'oorarivippu/src/main/java/com/oorarivippu/voiceannouncer'
class OorLocationQualityContracts(unittest.TestCase):
    def test_no_admin_fallback_and_bounded_resources(self):
        s=(S/'LocationAnnouncementService.kt').read_text()
        self.assertNotIn('listOf(address.locality, address.subAdminArea)',s)
        self.assertIn('FamilyRepository.publish(this@LocationAnnouncementService, resolved.familyLocality, resolved.district, location)',s)
        self.assertIn('familyLocality = PlaceDataPolicy.resolvedLocality(address.locality, null, address.subAdminArea)',s)
        for x in ('PlaceDataPolicy.resolvedLocality','DestinationFixPolicy.usable','withTimeoutOrNull(15_000L)','neuralWorkCount.incrementAndGet() > 6','neuralExecutor.shutdownNow()','RejectedExecutionException'):
            self.assertIn(x,s)
    def test_satellite_lifecycle_and_honest_unavailable(self):
        s=(S/'LocationDiagnostics.kt').read_text()
        for x in ('Lifecycle.State.RESUMED','unregisterGnssStatusCallback','onDispose','usedInFix','coerceAtMost(256)','take(24)','SatellitePolicy.fresh','getHistoricalProcessExitReasons','not a diagnosis of a whole-phone restart'):
            self.assertIn(x,s)
        for x in ('requestLocationUpdates','HttpURLConnection','OkHttp','putString','requestPermissions'):
            self.assertNotIn(x,s)
    def test_current_public_links_are_not_advanced(self):
        self.assertIn('versionName = "1.1.18"',(R/'oorarivippu/build.gradle.kts').read_text())
        self.assertIn('publishedVersion = "1.1.17"',(R/'shared/src/main/java/com/universalrp/updates/SisterApps.kt').read_text())
        self.assertNotIn('Oor-Arivippu-v1.1.18.apk',(R/'site/index.html').read_text())
        inspector=(R/'tools/inspect_oor_location_candidate.py').read_text()
        self.assertIn("'1.1.18', 23",inspector)
        self.assertIn('7b7ad216bc2b5cdbb4c2d1e6323bcd637c49a6365b804e62fa9bd9d1998c708f',inspector)
