from pathlib import Path
import json
import unittest

ROOT = Path(__file__).resolve().parents[2]
RADIO = ROOT / 'radio/src/main/java/com/universalrp/tamilnadufm'

class RadioSearchNewsRegressions(unittest.TestCase):
    def test_mixed_tamil_stations_are_not_presented_as_dedicated_news(self):
        stations = json.loads((ROOT / 'radio/src/main/assets/stations_seed.json').read_text())['stations']
        names = {'AIR Tuticorin', 'Sooriyan Fm Sri Lanka', 'Shakthi Fm',
                 'Universal Health Radio (தமிழ்)', 'Punnagai Radio', 'Siragugal Fm Tamil Srilanka'}
        found = [s for s in stations if s['name'] in names]
        self.assertEqual(names, {s['name'] for s in found})
        self.assertTrue(all(s['category'] != 'tamil_news' for s in found))
        self.assertTrue(all(s['url'] for s in found))

    def test_navigation_search_and_directory_have_explicit_recovery(self):
        vm = (RADIO / 'MainViewModel.kt').read_text()
        screen = (RADIO / 'ui/StationsScreens.kt').read_text()
        root = (RADIO / 'ui/AppRoot.kt').read_text()
        self.assertIn('it.copy(category = category, query = "")', vm)
        self.assertIn('state.query.isNotBlank()', screen)
        self.assertIn('"Clear search"', screen)
        self.assertIn('BackHandler { vm.closeDirectory() }', root)
        self.assertIn('vm.selectTab(Tab.RADIO)', root)
        self.assertIn('No results, or the directory could not be reached.', screen)
        self.assertIn('https://tamil.news18.com/live-tv/', screen)
        self.assertIn('https://www.polimernews.com/dlive-tv', screen)

if __name__ == '__main__':
    unittest.main()
