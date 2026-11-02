"""Source contracts only; executed Android/policy tests are separate CI gates."""
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'cleaner/src/main/java/com/universalrp/cleansweep'
def src(path): return (BASE / path).read_text()
class LiveGuard285Contracts(unittest.TestCase):
    def test_manual_entry_and_cpu_pipeline_are_main_safe(self):
        s=src('voice/NewsHeadlinesVoiceWatcher.kt')
        self.assertIn('readTodaysHeadlinesManually(context: Context): Int = withContext(Dispatchers.IO)',s)
        self.assertIn('rotation: Int): List<NewsHeadline> = withContext(Dispatchers.Default)',src('voice/NewsBulletinPipeline.kt'))
    def test_start_checkpoint_never_commits_or_waits_on_main(self):
        s=src('voice/NewsHeadlinesVoiceWatcher.kt')
        entry=s.split('internal fun recordAudibleActivity(',1)[1].split('@Synchronized',1)[0]
        self.assertIn('Looper.myLooper()',entry)
        self.assertIn('checkpointScope.launch',entry)
        self.assertNotIn('.commit()',entry)
        self.assertNotIn('@Synchronized\n    fun announcedKeysToday',s)
        self.assertIn('editor.commit()',s) # Completed-story durability is not weakened.
    def test_player_cancellation_is_off_main_and_snapshots_old_player(self):
        s=src('voice/Announcer.kt')
        self.assertNotIn('activeMediaPlayer?.stop()',s)
        self.assertIn('val player = activeMediaPlayer ?: return',s)
        self.assertIn('playerControlExecutor.execute { runCatching { player.stop() } }',s)
    def test_current_warning_does_not_or_in_historical_automatic_error(self):
        s=src('ui/VoiceScreen.kt').split('val hdWaiting =',1)[1].split('if (hdWaiting)',1)[0]
        self.assertIn('hdWaiting(delivery.stage)',s)
        self.assertNotIn('automatic.stage',s)
    def test_handoff_wait_is_bounded_cancellable_and_does_not_spend_engine_attempts(self):
        s=src('voice/Announcer.kt')
        wait=s.split('private fun awaitHandoffInternet(',1)[1].split('/** A handoff',1)[0]
        for token in ('HANDOFF_GRACE_MS','NET_CAPABILITY_VALIDATED','networkHandle','Thread.currentThread().isInterrupted','newsExpired(request)','stable.observe'):
            self.assertIn(token,wait)
        self.assertIn('stillRelevant = { transport(context) == current }',src('notify/AutomaticAlerts.kt'))
    def test_web_tabs_retain_instances_instead_of_reloading_on_expand(self):
        s=src('ui/DisasterWebWindow.kt')
        for token in ('pages.getOrPut(selected)','frame.removeAllViews()','frame.addView(page','page.onResume()','page.destroy()','fillMaxHeight(if (expanded) 1f else .80f)'):
            self.assertIn(token,s)
        self.assertNotIn('evaluateJavascript',s)
        self.assertNotIn('addJavascriptInterface',s)
    def test_isolated_browser_has_no_location_file_bridge_or_mixed_content(self):
        s=src('ui/DisasterWebWindow.kt')
        for token in ('allowFileAccess = false','allowContentAccess = false','setGeolocationEnabled(false)','MIXED_CONTENT_NEVER_ALLOW','setAcceptThirdPartyCookies(this, false)','handler.cancel()'):
            self.assertIn(token,s)
        self.assertIn('browser.openUri(currentUrl)',s) # Current page, not the original report URL.
if __name__ == '__main__': unittest.main()
