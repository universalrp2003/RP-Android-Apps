from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

class StorePromotionTest(unittest.TestCase):
    def test_exact_artifact_main_only_without_rebuild_or_replacement(self):
        text = (ROOT/'.github/workflows/promote-tamilan-store-0-7.yml').read_text()
        for pin in ('710cf71e3bfa2aaea8f841db944db04ad46d9043', '41147678511', '124376508008', '13021219269', '0915caff33ffe1847dcd086635ea957af6006b1084e0e7bbd31b94fae025625e', '41147678510'):
            self.assertIn(pin, text)
        self.assertIn("GITHUB_REF'] == 'refs/heads/main'", text)
        self.assertIn('Compiled inputs changed:', text)
        self.assertIn('--draft', text)
        self.assertIn('--draft=false', text)
        self.assertIn('sha256sum --check Tamilan-Store-v0.7.sha256', text)
        self.assertIn('verify_release_permission_delta.py', text)
        self.assertNotIn('assembleRelease', text)
        self.assertNotIn('gh release delete', text)
        self.assertNotIn('accepted-tamilanstore', text)
        self.assertNotIn('previous-tamilanstore', text)
        self.assertNotIn('published-tamilanstore', text)
        self.assertIn('target.mkdir()', text)
        for name in ('accepted', 'previous', 'published'):
            self.assertIn(f'/tmp/{name}-store', text)
