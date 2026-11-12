"""Live Guard 3.00: the publication boundary must be explicit, not accidental.

Committing docs/live-guard-3.00-publication-pins.json is what makes CI publish. That is a
deliberate act, so it must never happen as a side effect of preparing a release: this test
fails if the trigger exists while there is no clear record that the owner asked for it.
"""
import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TRIGGER = ROOT / 'docs/live-guard-3.00-publication-pins.json'
READY = ROOT / 'docs/live-guard-3.00-publication-pins.ready.json'


class PublicationBoundaryTest(unittest.TestCase):
    def test_promotion_workflow_only_runs_on_the_pins_file(self):
        wf = (ROOT / '.github/workflows/promote-liveguard-3-00.yml').read_text()
        # The only trigger is a push that touches the pins file.
        self.assertIn('docs/live-guard-3.00-publication-pins.json', wf)
        # The publisher itself is also a trigger, so a promotion that failed before publishing
        # can be re-run. Still push-only and still fails closed on a missing/stale pins file.
        self.assertIn('tools/promote_verified_liveguard_300.py', wf)
        self.assertNotIn('workflow_dispatch', wf)

    def test_publisher_refuses_a_template_or_incomplete_pins(self):
        src = (ROOT / 'tools/promote_verified_liveguard_300.py').read_text()
        self.assertIn("assert '_note' not in p", src)
        self.assertIn("assert pins_path.exists()", src)
        # The example carried a _note key; the real pins must never have one.
        if READY.exists():
            pins = json.loads(READY.read_text())
            self.assertNotIn('_note', pins)
            self.assertEqual(64, len(pins['apk_sha256']))
            self.assertEqual(40, len(pins['source']))
            self.assertGreater(pins['bytes'], 1_000_000)
            self.assertEqual(954, pins['required_jvm_tests'])
            self.assertEqual(93, pins['required_suites'])

    def test_version_and_notes_track_the_module(self):
        gradle = (ROOT / 'cleaner/build.gradle.kts').read_text()
        self.assertIn('versionName = "3.00"', gradle)
        self.assertIn('versionCode = 105', gradle)
        note = (ROOT / 'releases/live-guard-v3.00.md').read_text()
        # Tamil must be the LAST section: the English view is everything before it.
        headings = [line for line in note.splitlines() if line.startswith('## ')]
        self.assertEqual(['## தமிழ்'], headings)
        self.assertGreater(note.index('## தமிழ்'), note.index('# Live Guard 3.00'))
        self.assertIn('not installed on a phone', note.lower())

    def test_candidate_is_chained_to_the_published_2_99_not_to_a_local_build(self):
        insp = (ROOT / 'tools/inspect_liveguard_300_candidate.py').read_text()
        self.assertIn("'live-guard-v2.99'", insp)
        self.assertIn('CleanSweep-v2.99.apk', insp)
        # 2.99's published, immutable digest - the owner's actual installed bytes.
        self.assertIn('d11818d1539d10061bf76060874e84a4bdfc4628c1dee1a5389dd0f89fd88b05', insp)
        self.assertIn("'3.00', 105", insp)


if __name__ == '__main__':
    unittest.main()
