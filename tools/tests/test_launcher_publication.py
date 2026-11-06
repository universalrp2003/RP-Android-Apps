"""Offline regression coverage for exact publication pins and sanitized galleries."""
import hashlib
import importlib.util
import json
from pathlib import Path
import sys
import unittest
ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT/'tools'))
import promote_verified_launcher_17 as promotion

class LauncherPublicationTests(unittest.TestCase):
    def receipt(self):
        return dict(source=promotion.SOURCE, package='com.universalrp.launcher', version='1.7', code=8,
                    sha256=promotion.DIGEST, bytes=promotion.SIZE, signer=promotion.SIGNER,
                    **{k: True for k in ('same_uid', 'private_fixture_preserved', 'old_contact_preferences_preserved',
                       'native_picker_add_open_remove_and_recreate', 'process_restart_preferences_preserved',
                       'font_200_controls_reachable', 'real_media_and_ring_index_changes',
                       'two_column_favourite_tiles', 'old_app_preferences_preserved')},
                    **{k: False for k in ('physical_phone_tested', 'hardware_audio_tested', 'play_protect_tested', 'published')})

    def test_candidate_receipt_fail_closed_for_every_pin_and_gate(self):
        valid = self.receipt()
        promotion.validate_receipt(valid)
        for key, value in valid.items():
            invalid = dict(valid)
            invalid[key] = not value if type(value) is bool else str(value) + 'wrong'
            with self.subTest(key=key), self.assertRaises(AssertionError):
                promotion.validate_receipt(invalid)
            missing = dict(valid); del missing[key]
            with self.assertRaises(KeyError):
                promotion.validate_receipt(missing)

    def test_publication_receipt_matches_immutable_candidate(self):
        receipt = json.loads((ROOT/'docs/launcher-1.7-publication.json').read_text())
        self.assertEqual((receipt['source'], receipt['artifact_id'], receipt['apk_sha256'], receipt['bytes']),
                         (promotion.SOURCE, promotion.ARTIFACT, promotion.DIGEST, promotion.SIZE))
        self.assertEqual(receipt['release_id'], 430611845)
        self.assertEqual(receipt['asset_id'], 698463228)
        self.assertIs(receipt['rebuilt'], False)
        notes = (ROOT/'docs/launcher-1.7-public-release-notes.md').read_text()
        self.assertIn(promotion.DIGEST, notes)

    def test_website_and_next_store_gallery_are_exact_reviewed_crops(self):
        review = json.loads((ROOT/'docs/launcher-1.7-gallery-review.json').read_text())
        self.assertIs(review['owner_authorized'], True)
        self.assertIs(review['store_current_apk_unchanged'], True)
        index = json.loads((ROOT/'tamilanstore/src/main/assets/screenshots/index.json').read_text())
        self.assertEqual(index['launcher'], [x['store'] for x in review['images']])
        self.assertEqual(len(review['images']), 3)
        card = (ROOT/'site/index.html').read_text().split('<!-- Elderly Launcher -->')[1].split('</article>')[0]
        for image in review['images']:
            data = (ROOT/'site'/image['website']).read_bytes()
            self.assertEqual(len(data), image['bytes'])
            self.assertEqual(hashlib.sha256(data).hexdigest(), image['sha256'])
            self.assertEqual(data, (ROOT/'tamilanstore/src/main/assets/screenshots'/image['store']).read_bytes())
            self.assertTrue(data.startswith(b'\xff\xd8'))
            self.assertNotIn(b'Exif\x00\x00', data)
            self.assertIn(image['website'], card)
        self.assertIn('personal contacts', card)
        self.assertIn('not a completed call', card)

    def test_unrelated_accepted_apk_versions_hashes_still_pinned(self):
        feed = json.loads((ROOT/'site/catalog-v2.json').read_text())
        apps = {a['id']: a for a in feed['apps']}
        self.assertEqual((apps['launcher']['version'], apps['launcher']['sha256']), ('1.7', promotion.DIGEST))
        for name, version, digest in (
            ('liveguard', '2.95', 'b51d355d113511f45a6cf121fffc0765f188b578e69001e11e4f986f0a7eecb7'),
            ('voicenotes', '1.3.4', '1aa2ea8eb1037098881ce4a0a4329b6164f77a77e0ab31112610fa2d03f8094a')):
            self.assertEqual((apps[name]['version'], apps[name]['sha256']), (version, digest))
        pages = (ROOT/'.github/workflows/pages.yml').read_text()
        self.assertIn('d359a17252551a5503cd5814f89da132e3f307205a24f2302436b457c25f488b', pages)
        self.assertIn('"elderly-launcher-v1.7"', pages)  # the current launcher stays mirrored
        # 1.5 and older are still public release assets; only current builds are mirrored now.
        self.assertNotIn('"elderly-launcher-v1.5"', pages)
        self.assertIn(promotion.DIGEST, pages)
        self.assertIn("if: github.ref == 'refs/heads/main'", pages)
