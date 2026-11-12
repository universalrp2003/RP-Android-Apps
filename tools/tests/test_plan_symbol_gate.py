"""Pins tools/check_plan_symbols.py so the plan files stay resolvable between CI runs.

No Compose toolchain exists in the source gates, so a wrong receiver or a missing import in
these files is otherwise only found by the Android build - which is what happened three times
on the Live Guard plan work.
"""
import subprocess
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / 'tools/check_plan_symbols.py'


class PlanSymbolGateTest(unittest.TestCase):
    def test_checker_is_clean_on_the_current_tree(self):
        run = subprocess.run([sys.executable, str(SCRIPT)], capture_output=True, text=True)
        self.assertEqual(0, run.returncode, run.stdout + run.stderr)
        self.assertIn('0 unresolved', run.stdout)

    def test_checker_catches_a_missing_import_not_just_a_missing_member(self):
        # SettingsScreen once called StatusPill.signalMode() while importing only
        # PillSignalPolicy. The member existed, so a pure member check passed; the Android
        # build failed with 'Unresolved reference StatusPill'. Verified end to end by
        # temporarily removing the import and expecting a non-zero exit.
        script = SCRIPT.read_text()
        self.assertIn('never imported', script)
        self.assertIn('ui/SettingsScreen.kt', script)

    def test_checker_covers_the_files_that_actually_broke_the_build(self):
        script = SCRIPT.read_text()
        for name in ('ui/MyPlanScreen.kt', 'notify/ChargeNotifier.kt', 'notify/StatusPill.kt',
                     'data/PlanReadingPolicy.kt', 'widget/CleanSweepWidget.kt'):
            self.assertIn(name, script)
