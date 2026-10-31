from pathlib import Path
import json
import unittest
ROOT = Path(__file__).resolve().parents[2]
RADIO = ROOT / 'radio/src/main/java/com/universalrp/tamilnadufm'

class RadioReviewContracts(unittest.TestCase):
    def test_tab_order_is_consumed_and_stored_independently(self):
        vm = (RADIO/'MainViewModel.kt').read_text()
        repo = (RADIO/'data/StationRepository.kt').read_text()
        self.assertIn('navigationOrder = repo.tabOrder(true)', vm)
        self.assertIn('categoryOrder = repo.tabOrder(false)', vm)
        self.assertIn('navigation_order_v1', repo)
        self.assertIn('category_order_v1', repo)
        self.assertIn('BottomNav(state.tab, state.navigationOrder)', (RADIO/'ui/AppRoot.kt').read_text())
        self.assertIn('state.categoryOrder.take(6)', (RADIO/'ui/StationsScreens.kt').read_text())
        editor = (RADIO/'ui/TabOrderEditor.kt').read_text()
        self.assertIn('Modifier.size(48.dp)', editor)
        self.assertIn('contentDescription', editor)
        self.assertIn('vm.resetTabOrder(navigation)', editor)

    def test_review_guard_covers_manual_queue_restore_and_media_service(self):
        self.assertIn('RadioStreamPolicy.canPlay(station.url)', (RADIO/'MainViewModel.kt').read_text())
        self.assertIn('RadioStreamPolicy.playableQueue(urls, names', (RADIO/'data/StationRepository.kt').read_text())
        service = (RADIO/'player/PlaybackService.kt').read_text()
        self.assertIn('ResolvingDataSource', service)
        self.assertIn('RadioStreamPolicy.canPlay(spec.uri.toString())', service)
        self.assertIn('!com.universalrp.tamilnadufm.data.RadioStreamPolicy.canPlay(url)', service)
        self.assertIn('Provider stream held for review', (RADIO/'ui/StationsScreens.kt').read_text())

    def test_audit_has_full_shipped_coverage_without_relabeling_as_content_review(self):
        source = (ROOT/'tools/audit_radio_catalogue.py').read_text()
        self.assertIn('pool.map(probe,old+new)', source)
        self.assertIn("'contentReviewed':False", source)
        self.assertIn("if len(new)>=80:break", source)
        self.assertIn("'stations_seed.json'", source)
        self.assertNotIn('verify=False', source)
        self.assertNotIn('Referer', source)

if __name__ == '__main__': unittest.main()
