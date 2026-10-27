import importlib.util
from pathlib import Path
import unittest
import io

spec = importlib.util.spec_from_file_location("feeds", Path(__file__).parents[1] / "verify_podcast_feeds.py")
feeds = importlib.util.module_from_spec(spec)
spec.loader.exec_module(feeds)

class PodcastFeedGateTest(unittest.TestCase):
    def test_empty_match_set_fails(self):
        with self.assertRaises(ValueError): feeds.feed_urls("object BuiltInShows {}")
    def test_unrecognised_url_is_not_silently_ignored(self):
        with self.assertRaises(ValueError): feeds.feed_urls('"https://bad.example/feed.xml"')
    def test_duplicate_feed_fails(self):
        with self.assertRaises(ValueError): feeds.feed_urls('"https://anchor.fm/s/a/podcast/rss" ' * 2)
    def test_html_challenge_fails(self):
        with self.assertRaises(ValueError): feeds.episodes(b'<html><title>Attention Required</title></html>')
    def test_empty_rss_fails(self):
        with self.assertRaises(ValueError): feeds.episodes(b'<rss><channel><title>x</title></channel></rss>')
    def test_real_shape_succeeds(self):
        title, audio = feeds.episodes(b'<rss><channel><title>Kids</title><item><enclosure url="https://publisher.example/audio.mp3"/></item></channel></rss>')
        self.assertEqual((title, audio), ('Kids', ['https://publisher.example/audio.mp3']))
    def test_large_archive_is_streamed_not_downloaded_in_full(self):
        item = b'<item><enclosure url="https://publisher.example/audio.mp3"/></item>'
        stream = io.BytesIO(b'<rss><channel><title>Kids</title>' + item * 1000000 + b'</channel></rss>')
        title, urls = feeds.streaming_episodes(stream)
        self.assertEqual('Kids', title)
        self.assertEqual(20, len(urls))
        self.assertLess(stream.tell(), feeds.MAX_BYTES)
    def test_streaming_html_challenge_fails(self):
        with self.assertRaises(ValueError): feeds.streaming_episodes(io.BytesIO(b'<html><title>challenge</title></html>'))
    def test_current_source_has_five_feeds(self):
        self.assertEqual(5, len(feeds.feed_urls(feeds.SOURCE.read_text())))

if __name__ == '__main__': unittest.main()
