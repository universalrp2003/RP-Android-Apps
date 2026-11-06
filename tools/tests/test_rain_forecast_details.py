"""Read-only rain presentation must not become a new warning engine or data collector."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
class RainDetailsBoundaries(unittest.TestCase):
    def test_read_only_no_extra_polling_or_permissions(self):
        policy=(ROOT/'cleaner/src/main/java/com/universalrp/cleansweep/data/RainForecastDetails.kt').read_text()
        for forbidden in ('HttpURLConnection','SharedPreferences','notify(', 'startService', 'shouldAnnounce ='):
            self.assertNotIn(forbidden,policy)
        for required in ('SimpleWeatherPolicy.FRESH_MS','isFinite()', '.take(5)', 'elapsed..120'):
            self.assertIn(required,policy)
    def test_official_and_forecast_are_not_conflated(self):
        ui=(ROOT/'cleaner/src/main/java/com/universalrp/cleansweep/ui/SimpleWeatherScreen.kt').read_text()
        for text in ('IMD rain/thunderstorm warnings are not automatically imported here',
                     'No matching GDACS report does not mean no IMD warning',
                     'Rain rate is not accumulated rainfall or rain probability',
                     'Forecast for saved home—not your phone', 'ZoneId.systemDefault().id'):
            self.assertIn(text,ui)
