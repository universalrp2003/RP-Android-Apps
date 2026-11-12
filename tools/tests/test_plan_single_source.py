"""The plan must mean one thing on every screen.

On 12 November 2026 the owner's screenshots showed My plan saying "0 MB left in the pack · of
3.2 GB" while History & tools said "Configured quota: 2.2 GB / Remaining estimate: 2.20 GB" at
the same moment. Two stores were being read: PlanStore for one screen, DataUsageTracker's legacy
`pack_limit_gb` mirror for the other. These tests pin the single-source rule and the other two
things the owner asked for in the same message.
"""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
UI = ROOT / 'cleaner/src/main/java/com/universalrp/cleansweep/ui'
DATA = ROOT / 'cleaner/src/main/java/com/universalrp/cleansweep/data'


class SinglePlanSourceTest(unittest.TestCase):
    def test_history_derives_its_numbers_from_the_plan_not_the_legacy_mirror(self):
        insights = (UI / 'InsightsScreen.kt').read_text()
        # The quota and remainder must come from the shared reading policy...
        self.assertIn('PlanReadingPolicy.reading(', insights)
        self.assertIn('quotaText', insights)
        self.assertIn('remainingText', insights)
        # ...and must no longer print the mirror's own formatted strings.
        self.assertNotIn('": ${it.formattedPackLimit}"', insights)
        self.assertNotIn('": ${it.formattedRemaining}"', insights)

    def test_unknown_remainder_is_not_passed_as_zero(self):
        insights = (UI / 'InsightsScreen.kt').read_text()
        # A null remainder is how "not measured" survives the call; a 0L would be a lie.
        self.assertIn('val remainingBytes = if (reading != null && reading.measured', insights)
        self.assertIn('else null', insights)
        budget = (DATA / 'DataBudget.kt').read_text()
        self.assertIn('if (remainingBytes == null || remainingBytes < 0) return DataBudgetResult(BudgetStatus.UNAVAILABLE)', budget)


class PillSizeInSettingsTest(unittest.TestCase):
    def test_size_control_lives_in_settings_as_well_as_home(self):
        settings = (UI / 'SettingsScreen.kt').read_text()
        self.assertIn('PillSizeControl(scale)', settings)
        self.assertIn('StatusPill.setScale(context, newScale)', settings)
        home = (UI / 'StatusPillHomeControls.kt').read_text()
        self.assertIn('fun PillSizeControl(', home)
        # Both screens share one control, so the limits cannot drift.
        self.assertIn('const val MIN_PILL_SCALE = 0.7f', home)
        self.assertIn('const val MAX_PILL_SCALE = 1.5f', home)
        self.assertEqual(1, len(re.findall(r'fun PillSizeControl\(', home)))


class NoSmsFillStepTest(unittest.TestCase):
    def test_the_plan_screen_no_longer_asks_the_user_to_paste_a_recharge_sms(self):
        plan = (UI / 'MyPlanScreen.kt').read_text()
        # The card is gone from the screen...
        self.assertNotIn('RechargeSmsCard(today = today', plan)
        self.assertNotIn('SectionCard(title = tr("Fill from the operator\'s SMS")', plan)
        # ...but the parser is kept, because History & tools still reads a pasted message and
        # its unit tests must keep running.
        self.assertTrue((DATA / 'RechargeSmsParser.kt').exists())


if __name__ == '__main__':
    unittest.main()
