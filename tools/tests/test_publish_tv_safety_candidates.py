"""Tests for the publication guard's release snapshot and fail-closed gates."""
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

path = Path(__file__).resolve().parents[1] / 'publish_tv_safety_candidates.py'
spec = importlib.util.spec_from_file_location('publish_tv_safety_candidates', path)
pub = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pub)

class TvPublisherSafetyTest(unittest.TestCase):
    def test_snapshots_ignore_drafts_and_preserve_all_public_assets(self):
        data = [dict(id=1, tag_name='old', draft=False, assets=[dict(id=3, name='x', size=4, digest='sha256:a')]),
                dict(id=2, tag_name='draft', draft=True, assets=[])]
        before = pub.snapshots(data)
        self.assertEqual(set(before), {1})
        with patch.object(pub, 'releases', return_value=data):
            pub.verify_prior(before)
        data[0]['assets'][0]['size'] = 5
        with patch.object(pub, 'releases', return_value=data):
            with self.assertRaises(AssertionError):
                pub.verify_prior(before)

    def test_singleton_guard_refuses_missing_or_ambiguous(self):
        self.assertEqual(pub.one(['one']), 'one')
        for items in ([], [1, 2]):
            with self.assertRaises(AssertionError):
                pub.one(items)

    def test_ci_and_branch_checks_precede_publication(self):
        with patch.dict(pub.os.environ, {'GITHUB_ACTIONS': 'false'}, clear=True):
            with self.assertRaises(AssertionError):
                pub.main()

if __name__ == '__main__':
    unittest.main()
