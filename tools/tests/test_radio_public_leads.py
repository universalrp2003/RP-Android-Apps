"""Offline contract for extracting leads; nothing here establishes station identity."""
import unittest

from tools.radio_reference.fetch_tamilradios import extract
from tools.radio_reference.inspect_public_players import extract_urls


class PublicRadioLeadTest(unittest.TestCase):
    def test_extracts_escaped_next_data_station_and_url(self):
        sample = r'{\"name\":\"Test Station\",\"genre\":\"Tamil\",\"url\":\"https:\/\/radio.example.org\/stream\"}'
        self.assertEqual(extract(sample, 'reference'), [
            {'name': 'Test Station', 'url': 'https://radio.example.org/stream', 'source': 'reference'}
        ])

    def test_discards_expiring_or_credentialled_urls(self):
        sample = r'{\"name\":\"Test\",\"url\":\"https:\/\/radio.example.org\/stream?zt=secret\"} {\"name\":\"Other\",\"url\":\"https:\/\/user:pass@radio.example.org\/stream\"}'
        self.assertEqual(extract(sample, 'reference'), [])

    def test_player_url_leads_do_not_expose_credentials_or_image_flood(self):
        text = ('https://user:password@radio.example.org/stream '
                'https://images.zeno.fm/thumbnail '
                'https://radio.example.org/stream?token=private '
                'https://radio.example.org/another.m3u8#fragment')
        self.assertEqual(extract_urls(text), [
            'https://radio.example.org/another.m3u8',
            'https://radio.example.org/stream',
        ])

    def test_challenge_is_not_a_valid_empty_listing(self):
        with self.assertRaises(ValueError):
            extract('Please wait while your request is being verified', 'reference')


if __name__ == '__main__':
    unittest.main()
