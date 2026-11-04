import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from create_continuity_pack import create_pack, DOCUMENTS, PREFIX


class ContinuityPackTests(unittest.TestCase):
    def test_only_allowlisted_docs_and_checksums_are_exported(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / 'handover.zip'
            info = create_pack(output)
            with zipfile.ZipFile(output) as archive:
                self.assertEqual(set(archive.namelist()),
                                 {PREFIX + name for name in DOCUMENTS + ('PACK-INFO.json', 'SHA256SUMS.txt')})
                for line in archive.read(PREFIX + 'SHA256SUMS.txt').decode().splitlines():
                    digest, name = line.split('  ', 1)
                    self.assertEqual(hashlib.sha256(archive.read(PREFIX + name)).hexdigest(), digest)
                for key in ('source_code_included', 'git_history_included', 'apks_included',
                            'private_keys_included', 'account_secrets_included', 'phone_data_included',
                            'cloud_service_backup_included'):
                    self.assertFalse(info[key])
                self.assertFalse(any(n.endswith(('.p12', '.jks', '.apk', '.aab')) for n in archive.namelist()))

    def test_previous_backup_is_not_overwritten(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / 'handover.zip'
            output.write_bytes(b'owner backup')
            with self.assertRaises(FileExistsError):
                create_pack(output)
            self.assertEqual(output.read_bytes(), b'owner backup')

    def test_inventory_is_metadata_not_a_claim_of_private_backup(self):
        inventory = json.loads((ROOT / 'docs/continuity/release-inventory.json').read_text())
        self.assertFalse(inventory['actual_apk_files_backed_up'])
        self.assertFalse(inventory['private_signing_backup_verified'])
        self.assertEqual(len(inventory['records']), 14)
        catalog = {a['id']: a for a in json.loads((ROOT / 'site/catalog-v2.json').read_text())['apps']}
        for app in inventory['records']:
            self.assertRegex(app['sha256'], r'^[0-9a-f]{64}$')
            self.assertGreater(app['bytes'], 0)
            self.assertTrue(app['url'].startswith('https://github.com/universalrp2003/RP-Android-Apps/releases/download/'))
            # The inventory is dated; a later legitimate release must not require rewriting history.
            if app['id'] in catalog and app['version'] == catalog[app['id']]['version']:
                self.assertEqual((app['version'], app['bytes'], app['sha256']),
                                 (catalog[app['id']]['version'], catalog[app['id']]['size'], catalog[app['id']]['sha256']))

    def test_handover_preserves_release_and_privacy_boundaries(self):
        doc = (ROOT / 'docs/continuity/TECHNICAL-HANDOVER.md').read_text()
        for text in ('Do **not** rerun', 'No new feature is authorized', 'next requested',
                     'generic unconfigured CI APK', 'main only', 'not cloud HD',
                     'never phone-root', 'no repeat already-accepted phone tests'):
            # Deferred work is described using emphasized wording.
            if text == 'next requested':
                text = 'Next *requested*'
            self.assertIn(text, doc)
