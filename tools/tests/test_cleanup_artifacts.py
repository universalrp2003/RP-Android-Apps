"""Source contracts for the Actions-artifact cleanup (storage hygiene, not a release path).

The cleanup workflow is the only thing in this repository that deletes anything, so its limits are
pinned here: a bounded number of packages per run, a workflow token that may only touch Actions
storage, a keep window, and the 7-day retention that stops the heavy build uploads filling the
account again.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = (ROOT / '.github/workflows/cleanup-artifacts.yml').read_text()
SCRIPT = (ROOT / 'tools/cleanup_actions_artifacts.py').read_text()
BUILD = (ROOT / '.github/workflows/build-apk.yml').read_text()

HEAVY_UPLOADS = ('store-bundles', 'cleansweep-apk', 'pulseeq-apk', 'ramesh-radio-apk',
                 'rp-vocal-remover-studio-apks')


class CleanupArtifactsContractTest(unittest.TestCase):
    def test_cleanup_runs_on_a_schedule_with_actions_write_only(self):
        self.assertIn('schedule:', WORKFLOW)
        self.assertRegex(WORKFLOW, r'cron:\s*"\d+ \*/\d+ \* \* \*"')
        self.assertIn('workflow_dispatch:', WORKFLOW)
        permissions = WORKFLOW[WORKFLOW.index('permissions:'):WORKFLOW.index('concurrency:')]
        self.assertIn('actions: write', permissions)
        self.assertIn('contents: read', permissions)
        for forbidden in ('packages: write', 'contents: write', 'deployments: write'):
            self.assertNotIn(forbidden, permissions)

    def test_deletes_are_bounded_and_manual_runs_start_as_a_dry_run(self):
        default_max = int(re.search(r"MAX_DELETES', '(\d+)'", SCRIPT).group(1))
        self.assertLessEqual(default_max, 900, 'must stay under the 1,000 requests/hour token limit')
        self.assertIn("default: true", WORKFLOW)          # workflow_dispatch dry_run defaults on
        self.assertIn("DRY_RUN", SCRIPT)
        self.assertIn("if DRY_RUN:\n            continue", SCRIPT)
        self.assertIn("default: \"300\"", WORKFLOW)
        self.assertIn("default: \"7\"", WORKFLOW)          # keep_days

    def test_keep_rules_and_heavy_upload_retention_are_pinned(self):
        self.assertIn("reasons = {'young': 0, 'newest-per-name': 0, 'pages-recent': 0, 'referenced': 0}",
                      SCRIPT)
        self.assertIn("PAGES_KEEP_DAYS", SCRIPT)
        self.assertIn("protected_ids()", SCRIPT)
        # The five heavy per-build packages must expire in a week, not a month.
        for name in HEAVY_UPLOADS:
            block = BUILD[:BUILD.index(f'name: {name}')]
            retention = re.findall(r'retention-days:\s*(\d+)', block)[-1]
            self.assertEqual(retention, '7', name)
        self.assertEqual(int(re.search(r"KEEP_DAYS', '(\d+)'", SCRIPT).group(1)), 7)
        # A real run that cannot delete anything must fail loudly instead of looking green.
        self.assertIn('no package could be deleted', SCRIPT)
        self.assertIn('return 1', SCRIPT)
