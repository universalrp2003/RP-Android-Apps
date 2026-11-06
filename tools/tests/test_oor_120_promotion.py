"""Exact final bytes and new GPS native proof cannot be replaced by older1.1.18 artifacts."""
import copy
import json
from pathlib import Path
import sys
import unittest
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
from promote_verified_oor_120 import validate_receipt

class FinalLifecyclePublication(unittest.TestCase):
    def fixtures(self):
        return tuple(json.loads((ROOT/p).read_text()) for p in (
            'docs/oor-1.1.20-candidate-verification.json',
            'docs/oor-location-1.1.20-publication-pins.json'))
    def test_exact_final_gps_receipt(self):
        r,p=self.fixtures(); validate_receipt(r,p)
        self.assertFalse(r['publication_authorized']); self.assertTrue(p['publication_authorized'])
        pub=json.loads((ROOT/'docs/oor-location-1.1.20-publication-receipt.json').read_text())
        self.assertEqual((pub['apk_sha256'],pub['bytes']),(r['sha256'],r['bytes']))
        self.assertFalse(pub['rebuilt']);self.assertTrue(pub['prior_public_assets_unchanged'])
        self.assertEqual((pub['release_id'],pub['asset_id']),(436223513,716334026))
    def test_old_artifact_or_missing_gates_fail_closed(self):
        for key,value in [('sha256','cf280091200f33442a5b81cd94e6146e64cd0247de7c5d3fee8d4536754865e1'),
                          ('native_trip_tests',3),('gps_probe_background_lifecycle_verified',False),('gps_probe_timeout_and_cleanup_verified',False),
                          ('same_uid',False),('data_preserved',False),('preferences_preserved',False),
                          ('restart_passed',False),('encrypted_family_fixture_ciphertext_preserved',False),
                          ('encrypted_family_fixture_decrypted_after_upgrade',False),('font100_and200_rendered',False),
                          ('exact_installed_apk_verified',False),('signer','0'*64),('added_permissions',['new'])]:
            r,p=self.fixtures();r=copy.deepcopy(r);r[key]=value
            with self.subTest(key=key),self.assertRaises(AssertionError):validate_receipt(r,p)
    def test_both_feeds_and_historical_mirrors_keep_exact_public_bytes(self):
        pub=json.loads((ROOT/'docs/oor-location-1.1.20-publication-receipt.json').read_text())
        for filename in ('catalog.json','catalog-v2.json'):
            app=next(a for a in json.loads((ROOT/'site'/filename).read_text())['apps'] if a['id']=='oorarivippu')
            self.assertEqual((app['version'],app['sha256'],app['size'],app['url']),('1.1.20',pub['apk_sha256'],pub['bytes'],pub['url']))
        pages=(ROOT/'.github/workflows/pages.yml').read_text()
        live=(ROOT/'tools/verify_live_three_releases.py').read_text()
        for version in ('1.1.15','1.1.16','1.1.17','1.1.20'):
            self.assertIn('Oor-Arivippu-v'+version+'.apk',pages)
            self.assertIn('Oor-Arivippu-v'+version+'.apk',live)
