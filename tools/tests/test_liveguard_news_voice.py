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
        self.assertIn('MAX_HD_SYNTHESIS_CHARS = 240', sanitizer)

    def test_announcer_plays_news_as_ordered_hd_parts(self):
        announcer = src('voice/Announcer.kt')
        self.assertIn('NewsSpeechSanitizer.splitForHd(request.text)', announcer)
        self.assertIn('request.partIndex < request.parts.lastIndex', announcer)
        self.assertIn('request.text = request.parts[request.partIndex]', announcer)
        self.assertIn('preparedNewsParts', announcer)
        self.assertIn('prepareAllNewsParts(', announcer)
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

    def test_news_is_not_rewritten_or_summarized_by_ai(self):
        watcher = src('voice/NewsHeadlinesVoiceWatcher.kt')
        self.assertNotIn('composeAiNewsBulletin', watcher)
        self.assertNotIn('AiClient.ask', watcher)
        self.assertIn('NewsSpeechSanitizer.buildBulletinSpeech(batch, tamil = tamil)', watcher)
        self.assertIn('NewsSpeechSanitizer.buildBulletinSpeech(freshBatch, tamil = tamil)', watcher)

    def test_preparation_policy_bounds_news_without_starving_alternatives(self):
        policy = src('voice/NewsVoicePreparationPolicy.kt')
        self.assertIn('PREPARE_TIMEOUT_MS = 240_000L', policy)
        self.assertIn('PART_TIMEOUT_MS = 30_000L', policy)
        self.assertIn('VOICE_ATTEMPT_TIMEOUT_MS = 12_000L', policy)
        self.assertIn('fun allPartsPrepared(', policy)

    def test_viewmodel_reports_voice_failure_and_stop_cancels_read(self):
        vm = src('MainViewModel.kt')
        self.assertIn('count < 0', vm)
        self.assertIn('newsReadingJob?.cancel()', vm)
        self.assertIn('newsVoiceBusy', vm)
        stop_voice = vm.split('fun stopVoice() {', 1)[1].split('fun setDailyScanOn', 1)[0]
        self.assertIn('newsReadingJob?.cancel()', stop_voice)


if __name__ == '__main__':
    unittest.main()
