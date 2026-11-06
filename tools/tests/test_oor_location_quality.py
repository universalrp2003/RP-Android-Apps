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
        self.assertIn('address.subLocality.takeIf { AppPreferences.state.value.announceSameCitySubArea }',s)
        self.assertIn('if (serviceDestroyed) return',s)
        self.assertIn('familyLocality = PlaceDataPolicy.resolvedLocality(address.locality, null, address.subAdminArea)',s)
        for x in ('PlaceDataPolicy.resolvedLocality','DestinationFixPolicy.usable','withTimeoutOrNull(15_000L)','neuralWorkCount.incrementAndGet() > 6','neuralExecutor.shutdownNow()','RejectedExecutionException'):
            self.assertIn(x,s)
    def test_satellite_lifecycle_and_honest_unavailable(self):
        s=(S/'LocationDiagnostics.kt').read_text()
        for x in ('Lifecycle.State.RESUMED','unregisterGnssStatusCallback','onDispose','usedInFix','coerceAtMost(256)','take(24)','SatellitePolicy.fresh','getHistoricalProcessExitReasons','Close diagnostics','heightIn(max=480.dp)','not a diagnosis of a whole-phone restart'):
            self.assertIn(x,s)
        for x in ('HttpURLConnection','OkHttp','putString','requestPermissions'):
            self.assertNotIn(x,s)
    def test_current_public_links_match_exact_authorized_receipt(self):
        self.assertIn('versionName = "1.1.19"',(R/'oorarivippu/build.gradle.kts').read_text())
        self.assertIn('publishedVersion = "1.1.18"',(R/'shared/src/main/java/com/universalrp/updates/SisterApps.kt').read_text())
        self.assertIn('Oor-Arivippu-v1.1.18.apk',(R/'site/index.html').read_text())
        inspector=(R/'tools/inspect_oor_location_candidate.py').read_text()
        self.assertIn("'1.1.19', 24",inspector)
        self.assertIn('e224ed2a9db1494c480c14e616821b24fbc73bd2ec48fea05390f401281ef0da',inspector)
    def test_authorized_foreground_probe_has_bounded_duration_and_cleanup(self):
        p=(S/'GpsDiagnosticProbe.kt').read_text()
        for value in ('DURATION_MS = 30_000L','setDurationMillis(DURATION_MS)','removeUpdates(listener)','removeCallbacks(deadline)','ACCESS_FINE_LOCATION'):
            self.assertIn(value,p)
        for value in ('AppPreferences','FamilyRepository','HttpURLConnection'):
            self.assertNotIn(value,p)
        ui=(S/'LocationDiagnostics.kt').read_text()
        for value in ('if (!open || !resumed || !allowed) probe.stop()', 'onDispose { probe.stop() }','Check GPS now','REASON_USER_REQUESTED'):
            self.assertIn(value,ui)
        progress=(S/'TripProgress.kt').read_text()
        self.assertIn('Last known straight-line remaining',progress)
        self.assertIn('Fix age:',progress)
        self.assertIn('takeIf { fresh }',progress)
    def test_exact_publisher_cannot_select_old_pre_probe_artifact(self):
        s=(R/'tools/promote_verified_oor_118.py').read_text()
        for x in ("r['native_trip_tests']==3", "r['gps_probe_timeout_and_cleanup_verified'] is True", "assert not existing", "parse_badging(badging) == parse_badging(previous)", "for tag, old in prior.items(): assert after[tag] == old", "git', 'diff', '--exit-code'"):
            self.assertIn(x,s)
    def test_background_cleanup_is_synchronous_not_dependent_on_recomposition(self):
        p=(S/'GpsDiagnosticProbe.kt').read_text()
        for x in ('LifecycleEventObserver','ON_PAUSE','ON_STOP','ON_DESTROY','lifecycle?.addObserver(observer)','lifecycle?.removeObserver(observer)','!lifecycle.currentState.isAtLeast'):
            self.assertIn(x,p)
        ui=(S/'LocationDiagnostics.kt').read_text()
        self.assertIn('GpsDiagnosticProbe(context, lifecycleOwner.lifecycle)',ui)
        self.assertIn('{ probe.stop(); unregister() }',ui)
        self.assertIn('lifecycleOwner.lifecycle.removeObserver(observer)',ui)
    def test_native_upgrade_version_assertions_match_selected_119_and_118(self):
        s=(R/'oorarivippu/src/androidTest/java/com/oorarivippu/voiceannouncer/OorLocationNativeTest.kt').read_text()
        for x in ('assertEquals("1.1.18", version)','assertEquals("1.1.19", version)','Version: 1.1.19'):
            self.assertIn(x,s)
        self.assertNotIn('1.1.17',s)
