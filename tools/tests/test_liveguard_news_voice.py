from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
CLEANER = ROOT / 'cleaner/src/main/java/com/universalrp/cleansweep'


def src(rel):
    return (CLEANER / rel).read_text(encoding='utf-8')


class LiveGuardNewsVoiceTest(unittest.TestCase):
    def test_eight_headlines_and_all_intervals_are_preserved(self):
        sanitizer = src('data/NewsSpeechSanitizer.kt')
        self.assertIn('DEFAULT_MAX_SPOKEN_HEADLINES = 8', sanitizer)
        self.assertIn('listOf(30, 60, 120, 180)', sanitizer)
        self.assertIn('fun splitForHd(', sanitizer)
        self.assertIn('MAX_HD_SYNTHESIS_CHARS = 420', sanitizer)

    def test_announcer_plays_news_as_ordered_hd_parts(self):
        announcer = src('voice/Announcer.kt')
        self.assertIn('NewsSpeechSanitizer.splitForHd(request.text)', announcer)
        self.assertIn('request.partIndex < request.parts.lastIndex', announcer)
        self.assertIn('request.text = request.parts[request.partIndex]', announcer)
        self.assertIn('fun speakNewsNow(', announcer)
        self.assertIn('exclusive = manual', announcer)

    def test_manual_news_owns_queue_and_waits_for_completion(self):
        announcer = src('voice/Announcer.kt')
        watcher = src('voice/NewsHeadlinesVoiceWatcher.kt')
        self.assertIn('if (request.exclusive)', announcer)
        self.assertIn('pending.clear()', announcer)
        self.assertIn('manual = true', watcher)
        self.assertIn('if (!spoken) return -1', watcher)
        self.assertIn('markHeadlinesAnnounced(app, batch, tamil = tamil)', watcher)
        self.assertLess(watcher.index('if (!spoken) return -1'), watcher.index('markHeadlinesAnnounced(app, batch, tamil = tamil)'))

    def test_automatic_news_marks_announced_only_after_success(self):
        watcher = src('voice/NewsHeadlinesVoiceWatcher.kt')
        self.assertIn('if (spoken) markHeadlinesAnnounced(app, freshBatch, tamil = tamil)', watcher)
        self.assertNotIn('onStarted = {', watcher)

    def test_headline_read_aloud_passes_title_not_label(self):
        home = src('ui/HomeScreen.kt')
        vm = src('MainViewModel.kt')
        self.assertIn('onSpeak(item.title)', home)
        self.assertIn('Text(tr("Read aloud"))', home)
        self.assertIn('Announcer.speakNewsNow(ctx, cleaned, Lang.languageIn(ctx))', vm)

    def test_viewmodel_reports_voice_failure_and_stop_cancels_read(self):
        vm = src('MainViewModel.kt')
        self.assertIn('count < 0', vm)
        self.assertIn('newsReadingJob?.cancel()', vm)
        self.assertIn('newsVoiceBusy', vm)


if __name__ == '__main__':
    unittest.main()
