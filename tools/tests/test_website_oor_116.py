"""Exact owner-accepted About/Guide release; never substitute a rebuild."""
import copy
import json
from pathlib import Path
import sys
import unittest
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
from promote_verified_oor_116 import validate_receipt

class ExactAboutRelease(unittest.TestCase):
    def fixtures(self):
        return tuple(json.loads((ROOT/p).read_text()) for p in (
            'docs/oor-about-guide-1.1.16-candidate-verification.json',
            'docs/oor-about-guide-1.1.16-publication-pins.json'))
    def test_owner_accepted_exact_candidate(self):
        r,p=self.fixtures();validate_receipt(r,p)
        self.assertTrue(p['publication_authorized']);self.assertEqual(p['owner_acceptance'],'Its perfect you can publish')
        self.assertFalse(r['publication_authorized']) # historical candidate boundary must remain truthful
    def test_wrong_bytes_or_missing_new_native_evidence_fail_closed(self):
        for key,value in [('sha256','0'*64),('version','1.1.15'),('code',20),('native_tests',2),
                          ('about_guide_jvm_tests',3),('bilingual_about_guide_rendered',False),
                          ('encrypted_family_fixture_decrypted_after_upgrade',False),
                          ('baseline_family_fixture_seeded',False),('same_uid',False),
                          ('signer','0'*64),('added_permissions',['x'])]:
            r,p=self.fixtures();r=copy.deepcopy(r);r[key]=value
            with self.subTest(key=key),self.assertRaises(AssertionError): validate_receipt(r,p)
    def test_no_rebuild_overwrite_or_blind_resume(self):
        source=(ROOT/'tools/promote_verified_oor_116.py').read_text()
        for required in ('Only explicitly pinned exact draft may resume','resume_apk_asset_id',
                         'Never replace tag',"for phase in ('draft', 'public')",'OK (3 tests)',
                         'OK (1 test)','sha(archive) ==', 'for tag, old in prior.items()'):
            self.assertIn(required,source)
        for forbidden in ('assembleRelease','--clobber'):
            self.assertNotIn(forbidden,source)

    def test_public_catalogs_and_both_mirrors_keep_old_assets(self):
        receipt=json.loads((ROOT/'docs/oor-about-guide-1.1.16-publication-receipt.json').read_text())
        self.assertFalse(receipt['rebuilt']);self.assertTrue(receipt['prior_public_assets_unchanged'])
        self.assertEqual((receipt['release_id'],receipt['asset_id']),(433392185,707716347))
        current=json.loads((ROOT/'docs/oor-location-1.1.20-publication-receipt.json').read_text())
        for name in ('catalog.json','catalog-v2.json'):
            app=next(a for a in json.loads((ROOT/'site'/name).read_text())['apps'] if a['id']=='oorarivippu')
            self.assertEqual(app['version'],'1.1.20')
            self.assertEqual((app['sha256'],app['size'],app['url']),(current['apk_sha256'],current['bytes'],current['url']))
        pages=(ROOT/'.github/workflows/pages.yml').read_text()
        live=(ROOT/'tools/verify_live_three_releases.py').read_text()
        for version in ('1.1.15','1.1.16'):
            self.assertIn('Oor-Arivippu-v'+version+'.apk',pages)
            self.assertIn('Oor-Arivippu-v'+version+'.apk',live)
        previous=json.loads((ROOT/'docs/oor-family-1.1.15-publication-receipt.json').read_text())
        self.assertEqual(previous['apk_sha256'],'ec625cbfb6faa1bab680b71e4184d945913908662e56f898e695dea932c1abe5')
