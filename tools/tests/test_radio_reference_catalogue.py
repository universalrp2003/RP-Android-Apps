"""Offline guardrails for the screenshot reconciliation and seed additions."""
import importlib.util
import json
import pathlib
import re
import unittest
import urllib.parse

ROOT = pathlib.Path(__file__).resolve().parents[2]
REFERENCE = ROOT / 'tools/radio_reference'


def endpoint_key(url):
    """Flag common Shoutcast aliases for review, not a proof of equivalence."""
    parsed = urllib.parse.urlsplit(url.strip())
    path = parsed.path.rstrip('/')
    if path in ('/;', '/;stream.mp3'):
        path = ''
    return (parsed.netloc.lower(), path, parsed.query)


class RadioReferenceCatalogueTest(unittest.TestCase):
    def test_every_screenshot_has_a_working_inventory_row(self):
        rows = [line for line in (REFERENCE/'screenshot-inventory.txt').read_text().splitlines()
                if line and line[0].isdigit()]
        self.assertEqual([line.split(' | ')[0] for line in rows], [f'{i:02}' for i in range(1, 39)])
        self.assertTrue(all(len(line.split(' | ')) > 5 for line in rows))

    def test_candidate_receipt_and_added_seed_entries(self):
        seed = json.loads((ROOT/'radio/src/main/assets/stations_seed.json').read_text())
        receipts = [json.loads((REFERENCE/name).read_text()) for name in ('first-probe-receipt.json', 'second-probe-receipt.json', 'artist-probe-receipt.json', 'devotional-probe-receipt.json', 'expanded-probe-receipt.json', 'further-probe-receipt.json', 'regional-probe-receipt.json', 'community-artist-probe-receipt.json', 'identity-review-probe-receipt.json', 'official-probe-receipt.json', 'kadal-probe-receipt.json', 'eleventh-probe-receipt.json', 'twelfth-probe-receipt.json', 'thirteenth-probe-receipt.json')]
        results = {r['url']: r for receipt in receipts for r in receipt['results']}
        added = [s for s in seed['stations'] if s.get('source') in ('reference-directory', 'reference-manual')]
        self.assertEqual(len(added), 103)
        self.assertEqual(seed['total'], len(seed['stations']))
        self.assertEqual(seed['verifiedCount'], sum(s.get('verified', True) for s in seed['stations']))
        self.assertEqual(seed['counts'], {k: sum(s['category'] == k for s in seed['stations']) for k in seed['counts']})
        self.assertEqual(len({s['url'].lower() for s in added}), len(added))
        latest = json.loads((REFERENCE/'catalogue-audit-receipt.json').read_text())
        latest_by_index = {r['index']: r for r in latest['shippedResults']}
        for index, station in enumerate(seed['stations']):
            if station not in added:
                continue
            result = results[station['url']]
            self.assertIn(result['status'], ('audio_reachable', 'unavailable'))
            self.assertFalse(result['providerReview'])
            self.assertFalse(result['contentReviewed'])
            current = latest_by_index.get(index)
            if current:
                self.assertEqual(current['name'], station['name'])
                expected = current['status'] == 'audio_reachable' and not current['providerReview']
                self.assertEqual(station['verified'], expected)
                self.assertEqual(station['auditStatus'], current['status'])
            else:
                self.assertEqual(station['verified'], result['status'] == 'audio_reachable')
                self.assertEqual(station['auditStatus'], result['status'])
            self.assertTrue(1 <= station['referenceScreen'] <= 38)

    def test_every_artist_photo_has_in_app_and_documented_credit(self):
        base = ROOT/'radio/src/main/java/com/universalrp/tamilnadufm/ui'
        artist = (base/'ArtistBrowse.kt').read_text()
        screen = (base/'StationsScreens.kt').read_text()
        credits = (ROOT/'docs/radio-artist-photo-credits.md').read_text()
        photos = re.findall(r'ArtistShortcut\("([^"]+)", "[^"]+", "(https://[^"]+)"\)', artist)
        self.assertGreaterEqual(len(photos), 32)
        for name, _ in photos:
            self.assertIn('Triple("' + name + ' — ', screen)
            self.assertRegex(credits, r'(?m)^- ' + re.escape(name) + r'[: —]')

    def test_existing_stations_are_retained_after_availability_checks(self):
        seed = json.loads((ROOT/'radio/src/main/assets/stations_seed.json').read_text())['stations']
        retained = json.loads((REFERENCE/'retained-station-ids.json').read_text())
        ids = [station['id'] for station in seed]
        self.assertEqual(len(ids), len(set(ids)))
        self.assertFalse(set(retained) - set(ids), "Availability checks must not remove saved stations")

    def test_latest_full_audit_matches_shipped_statuses(self):
        seed = json.loads((ROOT/'radio/src/main/assets/stations_seed.json').read_text())['stations']
        receipt = json.loads((REFERENCE/'catalogue-audit-receipt.json').read_text())
        rows = receipt['shippedResults']
        self.assertEqual(receipt['shippedChecked'], 600)
        self.assertEqual([r['index'] for r in rows], list(range(600)))
        for row in rows:
            station = seed[row['index']]
            self.assertEqual(row['name'], station['name'])
            expected = row['status'] == 'audio_reachable' and not row['providerReview']
            self.assertEqual(station.get('verified', True), expected, station['name'])

    def test_added_endpoints_do_not_duplicate_common_url_forms(self):
        self.assertEqual(endpoint_key('http://example.test:5206/;stream.mp3'),
                         endpoint_key('https://example.test:5206/'))
        self.assertNotEqual(endpoint_key('https://example.test/a?mount=one'),
                            endpoint_key('https://example.test/a?mount=two'))
        seed = json.loads((ROOT/'radio/src/main/assets/stations_seed.json').read_text())['stations']
        for station in seed:
            if station.get('source') in ('reference-directory', 'reference-manual'):
                duplicates = [s['name'] for s in seed if s is not station and
                              endpoint_key(s['url']) == endpoint_key(station['url'])]
                self.assertEqual(duplicates, [], f"{station['name']}: {duplicates}")

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
