"""Offline contract for extracting leads; nothing here establishes station identity."""
import unittest

from tools.radio_reference.fetch_tamilradios import extract


class PublicRadioLeadTest(unittest.TestCase):
    def test_extracts_escaped_next_data_station_and_url(self):
        sample = r'{\"name\":\"Test Station\",\"genre\":\"Tamil\",\"url\":\"https:\/\/radio.example.org\/stream\"}'
        self.assertEqual(extract(sample, 'reference'), [
            {'name': 'Test Station', 'url': 'https://radio.example.org/stream', 'source': 'reference'}
        ])

    def test_discards_expiring_or_credentialled_urls(self):
        sample = r'{\"name\":\"Test\",\"url\":\"https:\/\/radio.example.org\/stream?zt=secret\"} {\"name\":\"Other\",\"url\":\"https:\/\/user:pass@radio.example.org\/stream\"}'
        self.assertEqual(extract(sample, 'reference'), [])

    def test_challenge_is_not_a_valid_empty_listing(self):
        with self.assertRaises(ValueError):
            extract('Please wait while your request is being verified', 'reference')


if __name__ == '__main__':
    unittest.main()
