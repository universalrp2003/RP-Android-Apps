from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
BASE=ROOT/'cleaner/src/main/java/com/universalrp/cleansweep'
class LiveGuard294ScopeTests(unittest.TestCase):
    def test_each_sim_uses_its_own_subscription_without_location(self):
        s=(BASE/'notify/PillSignalReader.kt').read_text()
        self.assertIn('createForSubscriptionId(info.subscriptionId)',s)
        self.assertIn('simSlotIndex',s)
        self.assertNotIn('getDefaultDataSubscriptionId',s)
        self.assertNotIn('allCellInfo',s)
        self.assertNotIn('ACCESS_FINE_LOCATION',s)
        self.assertIn('Manifest.permission.READ_PHONE_STATE',s)
    def test_default_does_not_overwrite_explicit_news_choice(self):
        s=(BASE/'data/NewsPreferences.kt').read_text()
        self.assertIn('getString("language", Language.TAMIL_TRANSLATED.name)',s)
        self.assertIn('getOrDefault(Language.TAMIL_TRANSLATED)',s)
        self.assertIn('putString("language", lang.name)',s)
    def test_empty_permission_row_no_longer_adds_lazy_card_gap(self):
        s=(BASE/'ui/HomeScreen.kt').read_text()
        self.assertNotIn('item(key = "permission_summary")',s)
        self.assertIn('HomePermissionSummary(state, vm)\n                    HomeHeroCard(state, vm)',s)
        self.assertIn('compact = true',s)
        c=(BASE/'ui/Components.kt').read_text()
        self.assertIn('heightIn(min = 48.dp)',c)
        self.assertNotIn('fontScale = 1',c)
    def test_dedicated_candidate_is_pinned_to_latest_public_baseline_and_new_native_tests(self):
        s=(ROOT/'.github/workflows/liveguard-294-candidate.yml').read_text()
        self.assertIn('arena/282fce0a-rp-android-apps',s)
        self.assertIn("assert len(new)==3",s)
        self.assertIn("assert len(cases)==5",s)
        self.assertIn('Final installed APK bytes differ',s)
        self.assertIn('Live-Guard-2.94-signed-tested-candidate',s)
        p=(ROOT/'tools/inspect_liveguard_294_candidate.py').read_text()
        self.assertIn("'2.94', 99, 'live-guard-v2.93'",p)
        self.assertIn('9f8e7936ae98ed5fb43e9087acf6a6de87dc5837fd66c9a97faa0216aa55f012',p)
