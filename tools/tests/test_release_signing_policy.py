import importlib.util
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('release_signing', ROOT/'tools/verify_release_signing_policy.py')
policy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(policy)


class ReleaseSigningPolicyTest(unittest.TestCase):
    def test_exposed_identity_is_rejected_independently_of_current_file_presence(self):
        with self.assertRaisesRegex(ValueError, 'legacy signing private-key container'):
            policy.verify_signer(policy.LEGACY_MAIN)

    def test_format_changes_cannot_clear_the_hold(self):
        formatted = ':'.join(policy.LEGACY_MAIN[i:i+2] for i in range(0,64,2)).upper()
        with self.assertRaises(ValueError): policy.verify_signer(formatted)

    def test_invalid_identity_is_not_treated_as_a_new_private_key(self):
        for value in ['', 'unknown', '00'*31, 'g'*64]:
            with self.assertRaises(ValueError): policy.verify_signer(value)

    def test_only_the_pinned_owner_backed_fresh_install_signer_passes(self):
        self.assertEqual(policy.FRESH_INSTALL_SIGNER, policy.verify_fresh_install_signer(policy.FRESH_INSTALL_SIGNER))
        with self.assertRaisesRegex(ValueError, 'differs from the owner-backed'):
            policy.verify_fresh_install_signer('a' * 64)
        with self.assertRaises(ValueError):
            policy.verify_fresh_install_signer(policy.LEGACY_MAIN)

    def test_all_main_publishers_gate_on_fresh_install_signer_and_secret_stays_separate(self):
        for path in (ROOT/'.github/workflows').glob('*.yml'):
            source = path.read_text()
            if 'gh release create' not in source or path.name == 'secret-message-wifi-test.yml': continue
            self.assertIn('python3 tools/verify_release_signing_policy.py --fresh-install', source, str(path))
        secret = (ROOT/'.github/workflows/secret-message-wifi-test.yml').read_text()
        self.assertNotIn('verify_release_signing_policy.py --fresh-install', secret)
