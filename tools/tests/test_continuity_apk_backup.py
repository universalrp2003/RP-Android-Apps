import copy
import json
from pathlib import Path
import sys
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
from download_continuity_apks import validate_inventory,safe_staging_path,verify_file

class ContinuityApkBackupTests(unittest.TestCase):
    def inventory(self):
        return json.loads((ROOT/'docs/continuity/release-inventory.json').read_text())
    def test_canonical_fourteen_public_assets(self):
        self.assertEqual(len(validate_inventory(self.inventory())),14)
    def test_untrusted_path_or_metadata_is_rejected(self):
        for key,value in [('apk','../bad.apk'),('tag','bad;command'),('url','https://example.org/file.apk'),('sha256','x'),('bytes',0)]:
            data=copy.deepcopy(self.inventory());data['records'][0][key]=value
            with self.subTest(key=key),self.assertRaises(ValueError):validate_inventory(data)
        for p in ('/home/user/apks','/tmp','/tmp/../home/user/apks'):
            with self.subTest(path=p),self.assertRaises(ValueError):safe_staging_path(p)
        self.assertEqual(safe_staging_path('/tmp/rp-backup-test'),Path('/tmp/rp-backup-test'))
    def test_exact_file_check_rejects_wrong_download(self):
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp)/'fake.apk';p.write_bytes(b'not an apk')
            with self.assertRaises(ValueError):verify_file(p,self.inventory()['records'][0])
