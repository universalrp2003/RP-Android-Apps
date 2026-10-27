import hashlib
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from promote_phone_tested_apk import check_receipt, compiled_changes


class PhoneTestedPromotionTest(unittest.TestCase):
    def test_compiled_changes_cannot_be_hidden_among_docs(self):
        paths = ['releases/note.md', 'podcast/README.md', '.github/workflows/podcast-release.yml', 'podcast/src/main/AndroidManifest.xml', 'shared/src/main/java/Example.kt', 'gradle.properties']
        self.assertEqual(paths[3:], compiled_changes(paths, 'podcast'))
        self.assertEqual([], compiled_changes(paths[:3], 'podcast'))
        self.assertEqual(['podcast/build.gradle.kts'], compiled_changes(['podcast/build.gradle.kts'], 'podcast'))

    def test_receipt_requires_exact_bytes_version_and_source(self):
        with tempfile.TemporaryDirectory() as directory:
            apk = Path(directory) / 'podcast-release.apk'
            apk.write_bytes(b'immutable candidate')
            digest = hashlib.sha256(apk.read_bytes()).hexdigest()
            expected = {'applicationId': 'com.example.app', 'versionName': '1.4', 'versionCode': '6'}
            receipt = '\n'.join(f'{key}: {value}' for key, value in {**expected, 'Source': 'abc', 'File': apk.name, 'SHA-256': digest, 'Bytes': str(apk.stat().st_size)}.items())
            self.assertEqual(digest, check_receipt(receipt, apk, expected, 'abc'))
            for broken in ['', receipt.replace('versionName: 1.4', 'versionName: 1.3'), receipt.replace('Source: abc', 'Source: other')]:
                with self.assertRaises(ValueError):
                    check_receipt(broken, apk, expected, 'abc')
            with self.assertRaisesRegex(ValueError, 'Duplicate'):
                check_receipt(receipt + '\nSource: abc', apk, expected, 'abc')
            apk.write_bytes(b'changed candidate')
            with self.assertRaisesRegex(ValueError, 'bytes differ'):
                check_receipt(receipt, apk, expected, 'abc')
