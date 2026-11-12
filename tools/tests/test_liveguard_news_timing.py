"""Cross-layer source contracts; actual Kotlin/Android behavior is checked separately in CI."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'cleaner/src/main/java/com/universalrp/cleansweep'
A = '{http://schemas.android.com/apk/res/android}'
def src(p): return (BASE / p).read_text()
class NewsTimingContracts(unittest.TestCase):
    def test_durable_screen_off_work_is_not_owned_or_cancelled_by_activity(self):
        source = src('notify/AutomaticAlerts.kt')
        self.assertIn('NewsAutoSchedule.requestDueCheck(app, delayMs = 15_000L)', source)
        self.assertNotIn('screenOffNewsJob', source)
        self.assertIn('NewsAutoSchedule.requestDueCheck(applicationContext, delayMs = 15_000L)', src('MainActivity.kt'))
    def test_alarm_and_recovery_have_separate_jobs(self):
        source = src('voice/NewsAutoSchedule.kt')
        self.assertIn('setExactAndAllowWhileIdle', source)
        self.assertIn('setAndAllowWhileIdle', source)
        self.assertIn('ExistingWorkPolicy.KEEP', source)
        self.assertIn('wake.acquire(NewsTimingPolicy.MAX_RUN_MS + 5_000L)', source)
        self.assertIn('finally { lock.unlock() }', source)
        self.assertIn('if (wake?.isHeld == true) wake.release()', source)
        self.assertIn('NewsTimingPolicy.RETRY_MS', source)
        self.assertNotIn('NetworkType.CONNECTED', source)
    def test_manifest_only_optional_exact_access_and_bounded_private_execution(self):
        tree = ET.parse(ROOT / 'cleaner/src/main/AndroidManifest.xml').getroot()
        permissions = {e.get(A+'name') for e in tree.findall('uses-permission')}
        self.assertIn('android.permission.SCHEDULE_EXACT_ALARM', permissions)
        self.assertNotIn('android.permission.USE_EXACT_ALARM', permissions)
        app = tree.find('application')
        for kind, name in [('service', '.voice.NewsAnnouncementService'), ('receiver', '.voice.NewsWakeReceiver')]:
            node = next(e for e in app.findall(kind) if e.get(A+'name') == name)
            self.assertEqual('false', node.get(A+'exported'))
        self.assertNotIn('REQUEST_IGNORE_BATTERY_OPTIMIZATIONS', permissions)
    def test_alarm_restored_after_boot_update_clock_or_permission_change(self):
        self.assertIn('NewsHeadlinesVoiceWatcher.syncSchedule(app)', src('notify/BootReceiver.kt'))
        self.assertIn('NewsHeadlinesVoiceWatcher.syncSchedule(applicationContext)', src('MainActivity.kt'))
        manifest = (ROOT / 'cleaner/src/main/AndroidManifest.xml').read_text()
        for action in ('SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED', 'TIME_SET', 'TIMEZONE_CHANGED'):
            self.assertIn(action, manifest)
    def test_network_validation_not_just_available_wakes_pending_voice(self):
        source = src('voice/Announcer.kt')
        self.assertIn('override fun onCapabilitiesChanged', source)
        self.assertIn('NET_CAPABILITY_VALIDATED', source)
        self.assertIn('NewsTimingPolicy.HANDOFF_GRACE_MS', source)
        self.assertIn('awaitNewsInternet(context, request, deadline)', source)
        self.assertIn('minOf(preparationDeadline', source)
        self.assertIn('NewsTimingPolicy.failureStart', source)
        self.assertIn('active?.preparedNewsParts?.isNotEmpty() == true', source)
    def test_safety_is_rechecked_after_long_preparation_and_during_audio(self):
        source = src('voice/Announcer.kt')
        self.assertIn('checkMedia = NewsPlaybackPlan.checkOtherMedia(request.started)', source)
        self.assertIn('automaticBlockReason(context, checkMedia = false)', source)
        self.assertIn('!request.manual', source)
    def test_timing_evidence_is_separate_from_selection(self):
        source = src('ui/NewsBulletinStatus.kt')
        self.assertIn('NewsScheduleStatus()', source)
        self.assertIn('Next news check', source)
        self.assertIn('Last news check', source)
        self.assertIn('not playback confirmation', source)
        self.assertIn('ACTION_REQUEST_SCHEDULE_EXACT_ALARM', source)
if __name__ == '__main__': unittest.main()
