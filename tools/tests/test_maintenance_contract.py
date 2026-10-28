from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]

class MaintenanceContractTest(unittest.TestCase):
    def test_every_artifact_upload_has_bounded_retention(self):
        for path in (ROOT / '.github/workflows').glob('*.yml'):
            text = path.read_text()
            for block in re.split(r'(?m)^      - ', text):
                if re.search(r'uses: actions/upload-artifact@', block):
                    match = re.search(r'retention-days:\s*(\d+)', block)
                    self.assertIsNotNone(match, str(path))
                    self.assertGreaterEqual(int(match.group(1)), 1)
                    self.assertLessEqual(int(match.group(1)), 30)

    def test_historical_radio_failure_is_archived_not_destroyed(self):
        self.assertFalse((ROOT / 'ci-candidate-radio-failure.md').exists())
        archive = ROOT / 'docs/archive/ci/ci-candidate-radio-failure.md'
        text = archive.read_text()
        self.assertIn('8f52da565ce2b72fb730cbca05b644f6e58bbb5e', text)
        self.assertIn('Unresolved reference', text)

    def test_required_preflight_and_keys_not_removed(self):
        self.assertTrue((ROOT / 'tools/_tmp_preflight.py').is_file())
        self.assertTrue((ROOT / 'ci-keystore').is_dir())

if __name__ == '__main__':
    unittest.main()
