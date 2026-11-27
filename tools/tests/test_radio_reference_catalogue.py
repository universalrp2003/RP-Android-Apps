"""Offline guardrails for the screenshot reconciliation and seed additions."""
import importlib.util
import json
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
REFERENCE = ROOT / 'tools/radio_reference'


class RadioReferenceCatalogueTest(unittest.TestCase):
    def test_every_screenshot_has_a_working_inventory_row(self):
        rows = [line for line in (REFERENCE/'screenshot-inventory.txt').read_text().splitlines()
                if line and line[0].isdigit()]
        self.assertEqual([line.split(' | ')[0] for line in rows], [f'{i:02}' for i in range(1, 39)])
        self.assertTrue(all(len(line.split(' | ')) > 5 for line in rows))

    def test_candidate_receipt_and_added_seed_entries(self):
        seed = json.loads((ROOT/'radio/src/main/assets/stations_seed.json').read_text())
        receipts = [json.loads((REFERENCE/name).read_text()) for name in ('first-probe-receipt.json', 'second-probe-receipt.json', 'artist-probe-receipt.json', 'devotional-probe-receipt.json', 'expanded-probe-receipt.json', 'further-probe-receipt.json', 'regional-probe-receipt.json', 'community-artist-probe-receipt.json')]
        results = {r['url']: r for receipt in receipts for r in receipt['results']}
        added = [s for s in seed['stations'] if s.get('source') in ('reference-directory', 'reference-manual')]
        self.assertEqual(len(added), 81)
        self.assertEqual(seed['total'], len(seed['stations']))
        self.assertEqual(seed['verifiedCount'], sum(s.get('verified', True) for s in seed['stations']))
        self.assertEqual(seed['counts'], {k: sum(s['category'] == k for s in seed['stations']) for k in seed['counts']})
        self.assertEqual(len({s['url'].lower() for s in added}), len(added))
        for station in added:
            result = results[station['url']]
            self.assertEqual(result['status'], 'audio_reachable')
            self.assertFalse(result['providerReview'])
            self.assertFalse(result['contentReviewed'])
            self.assertEqual(station['auditStatus'], 'audio_reachable')
            self.assertTrue(1 <= station['referenceScreen'] <= 38)

    def test_nesaganam_keeps_original_id_without_duplicate_station(self):
        seed = json.loads((ROOT/'radio/src/main/assets/stations_seed.json').read_text())
        stations = [s for s in seed['stations'] if s['name'] == 'Nesaganam Tamil Radio']
        self.assertEqual(len(stations), 1)
        self.assertEqual(stations[0]['id'], 'http://192.95.39.65:5206/;stream.mp3')
        self.assertEqual(stations[0]['url'], 'https://usa9.fastcast4u.com/proxy/nesaganam?mp=/1')

    def test_discovery_excludes_generic_search_terms(self):
        spec = importlib.util.spec_from_file_location('radio_reference_fetch', REFERENCE/'fetch_directory.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        q = module.reference_queries('01 | Tamil Radio | NJ Sunrise Radio | SPB Hits HD | AIR Karaikal')
        self.assertIn('sunrise', q)
        self.assertIn('karaikal', q)
        self.assertNotIn('radio', q)
        self.assertNotIn('tamil', q)


if __name__ == '__main__':
    unittest.main()
