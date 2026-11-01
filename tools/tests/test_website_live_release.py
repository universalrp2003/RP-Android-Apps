import hashlib
import importlib.util
import io
from pathlib import Path
import unittest
from unittest.mock import patch
import zipfile

TOOLS = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('live_three_release_verification', TOOLS / 'verify_live_three_releases.py')
live = importlib.util.module_from_spec(spec)
spec.loader.exec_module(live)


class LiveReleaseVerificationTests(unittest.TestCase):
    def setUp(self):
        self.data = b'synthetic fixture, not an APK'
        self.digest = hashlib.sha256(self.data).hexdigest()
        self.size = len(self.data)

    def test_exact_bytes_only(self):
        live.validate_apk(self.data, self.size, self.digest)
        for data, size, digest in [(b'stale', self.size, self.digest), (self.data, self.size + 1, self.digest),
                                   (self.data, self.size, '0' * 64)]:
            with self.assertRaises(ValueError):
                live.validate_apk(data, size, digest)

    def archive(self, names):
        output = io.BytesIO()
        with zipfile.ZipFile(output, 'w') as z:
            for name in names:
                z.writestr(name, self.data)
        return output.getvalue()

    def test_zip_exact_member_bytes_and_size(self):
        live.validate_zip(self.archive(['a.apk']), 'a.apk', self.size, self.digest)
        for names in (['wrong.apk'], ['a.apk', 'extra.txt'], ['../a.apk']):
            with self.assertRaises(ValueError):
                live.validate_zip(self.archive(names), 'a.apk', self.size, self.digest)
        with self.assertRaises(ValueError):
            live.validate_zip(self.archive(['a.apk']), 'a.apk', self.size + 1, self.digest)
        with self.assertRaises(ValueError):
            live.validate_zip(self.archive(['a.apk']), 'a.apk', self.size, '0' * 64)

    def test_response_bound(self):
        with patch.object(live.urllib.request, 'urlopen') as opened:
            opened.return_value.__enter__.return_value.read.return_value = b'12345'
            with self.assertRaises(ValueError):
                live.get_bounded('index.html', 4)

    def test_foreign_or_traversing_paths_fail_before_network(self):
        with patch.object(live.urllib.request, 'urlopen') as opened:
            for path in ('https://example.org/apk', '/index.html', '../index.html', ''):
                with self.assertRaises(AssertionError):
                    live.get_bounded(path, 10)
            opened.assert_not_called()

    def test_stale_live_metadata_is_rejected(self):
        with patch.object(live, 'get_bounded', return_value=b'stale website'):
            with self.assertRaises(ValueError):
                live.check_metadata()
