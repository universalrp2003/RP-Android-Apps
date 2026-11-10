"""Source wiring regressions; not proof of GPS/provider service or Tamil semantic quality."""
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT/'cleaner/src/main/java/com/universalrp/cleansweep'

class CurrentPlaceNewsTests(unittest.TestCase):
    def test_missing_home_does_not_short_circuit_gps(self):
        worker = (BASE/'work/WeatherWatchWorker.kt').read_text()
        self.assertNotIn('?: return null // Confirm a fixed place first.', worker)
        self.assertIn('if (home != null) targets += Target', worker)
        self.assertIn('SavedRainWatches.currentEnabled(ctx) || home == null', worker)
        self.assertIn('gpsOnly = true', worker)
        self.assertIn('SimpleWeatherStore.gpsStillAt(ctx, place)', worker)
        self.assertIn('SimpleWeatherStore.GPS && WeatherAlertStore.savedPlace(ctx) == null', worker)
        self.assertNotIn('savePlace(', worker)

    def test_gps_card_and_fetch_are_accessible_without_saving_home(self):
        ui = (BASE/'ui/SimpleWeatherScreen.kt').read_text()
        missing = ui.split('if (home == null || !NamedWeatherPlace.usable(home)) {')[1].split('} else {')[0]
        self.assertIn('PlaceForecastCard(SimpleWeatherStore.GPS, null, revision)', missing)
        self.assertIn('refresh()', missing)
        self.assertIn('LaunchedEffect(home)', ui)
        self.assertIn('WeatherPlaceResolver.hasLocationPermission(context)', ui)
        vm = (BASE/'MainViewModel.kt').read_text().split('fun clearWeatherPlace()')[1].split('fun saveTomorrowApiKey')[0]
        self.assertIn('checkWeatherNow()', vm)
        self.assertNotIn('savePlace(', vm)

    def test_visible_topics_exclude_humour_and_translation_still_requires_review(self):
        prefs = (BASE/'data/NewsPreferences.kt').read_text()
        self.assertIn('it != BalancedNewsPolicy.Category.HUMOUR', prefs)
        self.assertIn('filter { it in allCategories() }', prefs)
        self.assertIn('NewsPreferences.allCategories().forEach', (BASE/'ui/WatchSettingsPanels.kt').read_text())
        translator = (BASE/'voice/NewsTranslator.kt').read_text()
        for text in ('everyday spoken Tamil', 'Never translate word by word', 'uncertainty', 'When uncertain reject', 'approved(verdict.text', 'allowSearch = false'):
            self.assertIn(text, translator)
