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

    def test_cyclone_fetch_requires_verified_timeline_not_legacy_lifetime_cap(self):
        s=(JAVA/'data/NewsDisasterUpdates.kt').read_text()
        self.assertIn('type == "TC" || NewsDisasterPolicy.eligible(count(c, it.key))',s)
        self.assertIn('NewsDisasterPolicy.advisoryTimeline(details, e.id)',s)
        self.assertIn('if (type != "TC") events.addAll(batch)',s)
        self.assertIn('if (enriched != null && enriched.distanceKm <= 2000.0)',s)

    def test_new_policy_explained_without_changing_feature_acknowledgement_id(self):
        for f in ('ui/WatchSettingsPanels.kt','ui/FeatureUpdatesNotice.kt'):
            s=(JAVA/f).read_text()
            self.assertIn('older',s)
            self.assertIn('Flood',s)
        s=(JAVA/'data/FeatureDiscovery.kt').read_text()
        self.assertIn('disaster_news',s)
        self.assertNotIn('disaster_news_289',s)
