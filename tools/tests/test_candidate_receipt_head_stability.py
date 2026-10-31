from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

class CandidateReceiptHeadStabilityTest(unittest.TestCase):
    def test_receipt_job_does_not_mutate_an_open_pr_head(self):
        source = (ROOT/'.github/workflows/build-apk.yml').read_text()
        step = source.split('      - name: Record candidate APK digests', 1)[1].split('\n      - name:', 1)[0]
        self.assertIn('GH_TOKEN: ${{ github.token }}', step)
        self.assertIn('pull-requests: read', source)
        self.assertIn('"head=$GITHUB_REPOSITORY_OWNER:$GITHUB_REF_NAME"', step)
        self.assertIn('if [[ "$OPEN_PRS" != 0 ]]; then', step)
        self.assertLess(step.index('"$GITHUB_STEP_SUMMARY"'), step.index('OPEN_PRS='))
        self.assertLess(step.index('OPEN_PRS='), step.index('git commit'))
        self.assertIn('exit 0', step[step.index('OPEN_PRS='):step.index('git commit')])
