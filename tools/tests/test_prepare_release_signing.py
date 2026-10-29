import base64
import importlib.util
import os
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('prepare_release_signing', ROOT/'tools/prepare_release_signing.py')
prepare_module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(prepare_module)


class PrepareReleaseSigningTest(unittest.TestCase):
    def test_missing_actions_secrets_fail_without_creating_a_keystore(self):
        with tempfile.TemporaryDirectory() as temp:
            target = Path(temp) / 'release.p12'
            with patch.object(prepare_module, 'KEYSTORE', target):
                with self.assertRaisesRegex(ValueError, 'Both RP_RELEASE_KEYSTORE_BASE64'):
                    prepare_module.prepare({})
            self.assertFalse(target.exists())

    def test_private_keystore_is_decoded_and_its_certificate_is_pinned(self):
        with tempfile.TemporaryDirectory() as temp:
            folder = Path(temp)
            key = folder / 'test-key.pem'
            cert = folder / 'test-cert.pem'
            p12 = folder / 'test.p12'
            target = folder / 'ci-keystore' / 'release.p12'
            password = 'test-only-not-a-production-password'
            subprocess.run([
                'openssl', 'req', '-x509', '-newkey', 'rsa:2048', '-nodes', '-sha256',
                '-keyout', str(key), '-out', str(cert), '-days', '1',
                '-subj', '/CN=unit-test/O=RP Ideas/C=IN',
            ], check=True, capture_output=True)
            subprocess.run([
                'openssl', 'pkcs12', '-export', '-in', str(cert), '-inkey', str(key),
                '-out', str(p12), '-name', 'cleansweep', '-passout', f'pass:{password}',
            ], check=True, capture_output=True)
            fingerprint = subprocess.check_output(
                ['openssl', 'x509', '-in', str(cert), '-noout', '-fingerprint', '-sha256'], text=True
            ).strip().split('=', 1)[1].replace(':', '').lower()
            env = {
                **os.environ,
                'RP_RELEASE_KEYSTORE_BASE64': base64.b64encode(p12.read_bytes()).decode('ascii'),
                'RP_RELEASE_KEYSTORE_PASSWORD': password,
            }
            with patch.object(prepare_module, 'KEYSTORE', target), patch.object(prepare_module, 'EXPECTED_SIGNER', fingerprint):
                self.assertEqual(fingerprint, prepare_module.prepare(env))
            self.assertEqual(p12.read_bytes(), target.read_bytes())
            self.assertEqual(0o600, target.stat().st_mode & 0o777)

    def test_wrong_certificate_is_deleted_and_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            folder = Path(temp)
            key = folder / 'test-key.pem'
            cert = folder / 'test-cert.pem'
            p12 = folder / 'test.p12'
            target = folder / 'ci-keystore' / 'release.p12'
            password = 'test-only-not-a-production-password'
            subprocess.run([
                'openssl', 'req', '-x509', '-newkey', 'rsa:2048', '-nodes', '-sha256',
                '-keyout', str(key), '-out', str(cert), '-days', '1',
                '-subj', '/CN=wrong-test/O=RP Ideas/C=IN',
            ], check=True, capture_output=True)
            subprocess.run([
                'openssl', 'pkcs12', '-export', '-in', str(cert), '-inkey', str(key),
                '-out', str(p12), '-name', 'cleansweep', '-passout', f'pass:{password}',
            ], check=True, capture_output=True)
            env = {
                **os.environ,
                'RP_RELEASE_KEYSTORE_BASE64': base64.b64encode(p12.read_bytes()).decode('ascii'),
                'RP_RELEASE_KEYSTORE_PASSWORD': password,
            }
            with patch.object(prepare_module, 'KEYSTORE', target), patch.object(prepare_module, 'EXPECTED_SIGNER', '0' * 64):
                with self.assertRaisesRegex(ValueError, 'does not match'):
                    prepare_module.prepare(env)
            self.assertFalse(target.exists())


if __name__ == '__main__':
    unittest.main()
