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
        self.assertIn('if (spoken) markHeadlinesAnnounced(app, batch, tamil)', watcher)
        self.assertIn('playBatch(app, batch, lang, manual = true)', watcher)

    def test_automatic_news_marks_announced_only_after_success(self):
        watcher = src('voice/NewsHeadlinesVoiceWatcher.kt')
        self.assertIn('val spoken = playBatch(app, freshBatch, lang)', watcher)
        self.assertIn('if (spoken) markHeadlinesAnnounced(app, batch, tamil)', watcher)
        self.assertIn('onStarted = { recordAudibleActivity(app) }', watcher)
        self.assertIn('completeBulletin = false', watcher)
        # A start only throttles retries; it must never consume a headline or claim completion.
        activity = watcher.split('internal fun recordAudibleActivity(', 1)[1].split('fun syncSchedule(', 1)[0]
        self.assertNotIn('markHeadlinesAnnounced', activity)
        self.assertNotIn('KEY_LAST_AUTO_SPOKEN_MS', activity)

    def test_story_checkpoint_is_after_completed_clip_not_preparation_or_start(self):
        source = src('voice/Announcer.kt')
        callback = 'request.onPartCompleted?.invoke(request.partIndex)'
        played = source.index('if (played) {')
        self.assertGreater(source.index(callback), played)
        self.assertLess(source.index(callback), source.index('request.partIndex++', played))
        self.assertNotIn(callback, source[:played])
        watcher = src('voice/NewsHeadlinesVoiceWatcher.kt')
        self.assertIn('plan.getOrNull(part)?.completedHeadline', watcher)
        self.assertIn('completeBulletin = false', watcher)
        self.assertIn('editor.commit()', watcher)

    def test_headline_read_aloud_passes_title_not_label(self):
        home = src('ui/HomeScreen.kt')
        vm = src('MainViewModel.kt')
        self.assertIn('onSpeak(item.title)', home)
        self.assertIn('Text(tr("Read aloud"))', home)
        self.assertIn('Announcer.speakNewsNow(ctx, cleaned, Lang.languageIn(ctx))', vm)

    def test_news_uses_safe_extractive_editor_not_generated_summary(self):
        watcher = src('voice/NewsHeadlinesVoiceWatcher.kt')
        self.assertNotIn('composeAiNewsBulletin', watcher)
        pipeline = src('voice/NewsBulletinPipeline.kt')
        self.assertIn('NewsBulletinEditor.edit(context, nativePool, tamil, budget)', pipeline)
        editor = src('voice/NewsBulletinEditor.kt')
        self.assertIn('allowSearch = false', editor)
        self.assertIn('options[sentence]', editor)
        self.assertIn('TIMEOUT_MS = 15_000L', editor)
        self.assertIn('NewsSpeechSanitizer.buildBulletinSpeech(batch, tamil = tamil)', watcher)
        self.assertIn('NewsSpeechSanitizer.buildBulletinSpeech(freshBatch, tamil = tamil)', watcher)

    def test_both_paths_share_balancing_and_manual_does_not_recycle(self):
        watcher = src('voice/NewsHeadlinesVoiceWatcher.kt')
        self.assertEqual(watcher.count('prepareBalancedBatch(app, snapshot.items, tamil)'), 2)
        self.assertNotIn('alreadyAnnouncedKeys = emptySet()', watcher)
        self.assertIn('KEY_ROTATION', watcher)
        self.assertIn('KEY_RECENT_EVENTS', watcher)

    def test_freshness_is_checked_after_editor_and_during_audio(self):
        watcher = src('voice/NewsHeadlinesVoiceWatcher.kt')
        self.assertIn('NewsPolicy.isToday(it.publishedAtMs, readyAt', watcher)
        announcer = src('voice/Announcer.kt')
        self.assertIn('newsExpired(request)', announcer)
        self.assertIn('newsDayScope = currentNewsDayScope()', announcer)

    def test_specialists_are_interleaved_and_descriptions_are_cached(self):
        repo = src('data/TamilInfoStripRepo.kt')
        self.assertIn('specialistFeeds(tamil)', repo)
        self.assertIn('listOfNotNull(general.getOrNull(index), specialist.getOrNull(index))', repo)
        self.assertIn('put("description", item.description)', repo)
        self.assertIn('when:1d', repo)

    def test_translation_is_source_bound_reviewed_and_shares_one_deadline(self):
        pipeline = src('voice/NewsBulletinPipeline.kt')
        translator = src('voice/NewsTranslator.kt')
        self.assertIn('withTimeoutOrNull(AiRequestPolicy.Use.NEWS.budgetMs)', pipeline)
        self.assertIn('perCategoryMultiplier = 3', pipeline)
        self.assertIn('getEnglishSpecialistsForTranslation', pipeline)
        self.assertIn('NewsTranslationPolicy.restore', translator)
        self.assertIn('approved(verdict.text, checked.keys)', translator)
        self.assertIn('translated = true', translator)
        self.assertIn('news', src('ui/NewsBulletinStatus.kt').lower())

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
