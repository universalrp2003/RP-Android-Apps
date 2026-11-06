"""Owner-scoped public Oor guide/catalog and exact-byte promotion contracts."""
import copy
from html.parser import HTMLParser
import json
from pathlib import Path
import re
import sys
import unittest
ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
from promote_verified_oor_115 import validate_receipt, SIGNER, OLD_TAG, OLD_SHA, CANDIDATE_NAME

class GuideHTML(HTMLParser):
    def __init__(self, source):
        super().__init__(); self.ids=[]; self.links=[]; self.images=[]; self.scripts=[]
        self.feed(source)
    def handle_starttag(self, tag, attrs):
        attrs=dict(attrs)
        if 'id' in attrs:self.ids.append(attrs['id'])
        if tag=='a':self.links.append(attrs.get('href',''))
        if tag=='img':self.images.append(attrs.get('src',''))
        if tag=='script':self.scripts.append(attrs.get('src','inline'))

class OorGuideTests(unittest.TestCase):
    def test_bilingual_navigable_and_no_private_images_or_runtime_tracking(self):
        source=(ROOT/'site/oor-arivippu-guide.html').read_text(); html=GuideHTML(source)
        self.assertEqual(len(html.ids),len(set(html.ids)))
        for link in html.links:
            if link.startswith('#'):self.assertIn(link[1:],html.ids)
        review=json.loads((ROOT/"docs/oor-family-1.1.15-gallery-review.json").read_text())
        self.assertTrue(review["visual_review_completed"])
        self.assertEqual(html.images,[im["website"] for im in review["images"]]);self.assertEqual(html.scripts,[])
        self.assertIn('lang="ta"',source);self.assertIn('name="viewport"',source)
        self.assertNotIn('/tmp/',source);self.assertNotIn('Screenshot_2026',source)
        self.assertIn('oor-arivippu-guide.html',(ROOT/'site/index.html').read_text())
        self.assertIn('oor-arivippu-guide.html',(ROOT/'site/sitemap.xml').read_text())
    def test_capacity_consent_speech_and_receiver_limits_are_explicit(self):
        source=(ROOT/'site/oor-arivippu-guide.html').read_text()
        protocol=(ROOT/'oorarivippu/src/main/java/com/oorarivippu/voiceannouncer/family/FamilyProtocol.kt').read_text()
        self.assertRegex(protocol,r'MAX_MEMBERS\s*=\s*6')
        for text in ('Pending invitations count','10 minutes','both phones','sharing stays OFF','paused',
                     'five minutes','fifteen minutes','on-device','no continuous family polling',
                     'Not emergency tracking','Do not uninstall','independently verified'):
            # Release evidence, rather than the how-to, carries independent proof wording.
            if text=='independently verified':
                self.assertIn(text,(ROOT/'releases/oor-arivippu-v1.1.15.md').read_text())
            else:self.assertIn(text,source)
    def test_catalogs_and_live_verifier_pin_exact_public_receipt(self):
        receipt=json.loads((ROOT/'docs/oor-location-1.1.18-publication-receipt.json').read_text())
        self.assertFalse(receipt['rebuilt']);self.assertTrue(receipt['prior_public_assets_unchanged'])
        for filename in ('catalog.json','catalog-v2.json'):
            app=next(a for a in json.loads((ROOT/'site'/filename).read_text())['apps'] if a['id']=='oorarivippu')
            self.assertEqual(app['version'],'1.1.18')
            for field,key in [('sha256','apk_sha256'),('size','bytes'),('url','url')]:
                self.assertEqual(app[field],receipt[key])
        live=(ROOT/'tools/verify_live_three_releases.py').read_text()
        self.assertIn('oor-arivippu-guide.html',live);self.assertIn('Oor-Arivippu-v1.1.15.apk',live)
        old=json.loads((ROOT/'docs/oor-family-1.1.15-candidate-verification.json').read_text())
        self.assertFalse(old['publication_authorized']);self.assertFalse(old['physical_phone_tested'])

class OorPromotionTests(unittest.TestCase):
    def fixture(self):
        p=json.loads((ROOT/'docs/oor-family-1.1.15-publication-pins.json').read_text())
        r=json.loads((ROOT/'docs/oor-family-1.1.15-candidate-verification.json').read_text())
        return r,p
    def test_final_receipt_and_scope(self):
        r,p=self.fixture();validate_receipt(r,p)
        self.assertEqual(r['apk'],CANDIDATE_NAME);self.assertEqual(r['signer'],SIGNER)
        self.assertEqual(r['baseline_tag'],OLD_TAG);self.assertEqual(r['baseline_sha256'],OLD_SHA)
    def test_wrong_candidate_old_behavior_or_missing_evidence_rejected(self):
        for key,value in [('sha256','0'*64),('version','1.1.14'),('code',19),('signer','0'*64),
                          ('family_jvm_tests',29),('native_tests',1),('exact_installed_apk_verified',False),
                          ('same_uid',False),('data_preserved',False),('restart_passed',False),
                          ('opening_panel_did_not_connect_or_share',False),('added_permissions',['x']),
                          ('service_url','https://example.org'),('baseline_sha256','0'*64)]:
            r,p=self.fixture();r=copy.deepcopy(r);r[key]=value
            with self.subTest(key=key),self.assertRaises(AssertionError):validate_receipt(r,p)
    def test_immutable_release_and_no_rebuild_or_unrelated_publication(self):
        source=(ROOT/'tools/promote_verified_oor_115.py').read_text()
        for text in ('Never overwrite','Never replace tag',"for phase in ('draft', 'public')",
                     'sha(archive) ==', 'parse_badging(badging) == parse_badging(previous)',
                     'OK (2 tests)', 'for tag, old in prior.items()',"p['only_app'] == 'oorarivippu'"):
            self.assertIn(text,source)
        self.assertNotIn('assembleRelease',source);self.assertNotIn('--clobber',source)
