"""Source wiring contracts, not UI rendering, provider accuracy or audible proof."""
import unittest
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT/'cleaner/src/main/java/com/universalrp/cleansweep'

class WeatherAdvisorySourceTest(unittest.TestCase):
    def test_no_key_is_neutral_working_fallback_not_mandatory_provider_error(self):
        s=(JAVA/'ui/SimpleWeatherScreen.kt').read_text()
        self.assertIn('Free weather active — no API key needed',s)
        self.assertIn('if (snapshot.tomorrowStatus == "NO_KEY") MaterialTheme.colorScheme.onSurfaceVariant',s)
        self.assertNotIn('Tomorrow.io key needed',s)
        self.assertIn('a key alone does not guarantee it',s)

    def test_model_forecast_not_reported_local_measurement(self):
        s=(JAVA/'ui/SimpleWeatherScreen.kt').read_text()
        self.assertIn('Forecast estimate, not a local rain measurement.',s)
        self.assertIn('No rain indicated in forecast',s)
        self.assertNotIn('No rain reported',s)

    def test_cyclone_fetch_requires_verified_timeline_or_direct_dated_official_bulletin(self):
        s=(JAVA/'data/NewsDisasterUpdates.kt').read_text()
        self.assertIn('type == "TC" || NewsDisasterPolicy.eligible(count(c, it.key))',s)
        self.assertIn('NewsDisasterPolicy.advisoryTimeline(details, e.id)',s)
        self.assertIn('if (type != "TC") events.addAll(batch)',s)
        self.assertIn('OfficialCycloneClient.enrich(it,reports,origin,now)',s)
        self.assertIn('it.distanceKm <= 2000 && (it.type != "TC" || it.positionAt != null)',s)
        policy=(JAVA/'data/OfficialCycloneBulletin.kt').read_text()
        self.assertIn('WARNING POSITION:',policy)
        self.assertIn('substringBefore("FORECASTS:")',policy)
        self.assertIn('it.fresh(now)',policy)

    def test_new_policy_explained_without_changing_feature_acknowledgement_id(self):
        for f in ('ui/WatchSettingsPanels.kt','ui/FeatureUpdatesNotice.kt'):
            s=(JAVA/f).read_text()
            self.assertIn('older',s)
            self.assertIn('Flood',s)
        s=(JAVA/'data/FeatureDiscovery.kt').read_text()
        self.assertIn('disaster_news',s)
        self.assertNotIn('disaster_news_289',s)

    def test_pdf_isolation_and_private_data_boundary(self):
        manifest=(ROOT/'cleaner/src/main/AndroidManifest.xml').read_text()
        self.assertIn('android:name=".data.ImdPdfTextService" android:exported="false" android:isolatedProcess="true"',manifest)
        client=(JAVA/'data/OfficialCycloneClient.kt').read_text()
        self.assertIn('Build.VERSION.SDK_INT < 35',client)
        self.assertIn('ParcelFileDescriptor.MODE_READ_ONLY',client)
        self.assertIn('withTimeoutOrNull(3500)',client)
        self.assertIn('.followRedirects(false)',client)
        self.assertNotIn('latitude=',client)
        self.assertNotIn('pdfbox',(ROOT/'cleaner/build.gradle.kts').read_text().lower())

    def test_shared_card_keeps_foreground_ownership_and_no_fake_led_animation(self):
        status=(JAVA/'notify/RoutineStatus.kt').read_text()
        self.assertIn('generation == token',status)
        self.assertIn('if (owner !in owners) return',status)
        for f in ('notify/RadioWatchService.kt','notify/ChargeMonitorService.kt','voice/NewsAutoSchedule.kt'):
            text=(JAVA/f).read_text()
            self.assertIn('startForeground(',text)
            self.assertIn('STOP_FOREGROUND_DETACH',text)
            self.assertIn('RoutineStatus.release',text)
        pill=(JAVA/'notify/StatusPill.kt').read_text()
        self.assertNotIn('numSegments',pill)
        self.assertIn('NetworkActivityPolicy.level(entry.second)',pill)
