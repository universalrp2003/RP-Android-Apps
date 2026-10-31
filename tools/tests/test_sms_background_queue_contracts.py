"""Source integration guards complement JVM scanner/queue regression tests."""
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "smsguard/src/main/java/com/universalrp/smsguard"

class SmsBackgroundContracts(unittest.TestCase):
    def test_both_broadcasts_use_local_only_scan_and_durable_scheduling(self):
        receivers = (BASE / "service/SmsReceivers.kt").read_text()
        self.assertEqual(2, receivers.count("IncomingSmsProcessor.handleIncomingBroadcast("))
        processor = (BASE / "service/IncomingSmsProcessor.kt").read_text()
        broadcast = processor.split("fun handleIncomingBroadcast(", 1)[1].split("fun processReceivedSms(", 1)[0]
        self.assertIn("deferOnlineChecks = true", broadcast)
        self.assertIn("deferOnlineChecks = deferOnlineChecks || isBlocked", processor)
        self.assertIn("PendingLinkCheckWorker.schedule(appCtx, awaitPersistence = deferOnlineChecks)", processor)
        self.assertLess(processor.index("insertIncomingSmsToProvider"), processor.index("scanAllInMessage"))

    def test_queue_commit_and_work_persistence_before_finish(self):
        store = (BASE / "data/GuardSettingsStore.kt").read_text()
        write = store.split("private fun writePendingQueue(", 1)[1].split("// --- Bill", 1)[0]
        self.assertIn(".commit()", write)
        self.assertNotIn(".apply()", write)
        worker = (BASE / "service/PendingLinkCheckWorker.kt").read_text()
        for required in ("CoroutineWorker", "NetworkType.CONNECTED", "ExistingWorkPolicy.APPEND_OR_REPLACE",
                         "Result.retry()", "operation.result.get(3, TimeUnit.SECONDS)"):
            self.assertIn(required, worker)
        self.assertNotIn("setInputData(", worker)

    def test_background_drain_is_bounded_and_respects_mute_block_quiet_and_internet(self):
        source = (BASE / "service/NetworkReconnectMonitor.kt").read_text()
        for required in ("attempts >= 3", "shouldStop()", "mute?.blocked == true", "freshMute?.muteVoice != true",
                         "!SleepScheduleManager.isSleepingNow", "advancePendingLinkCheck", "!prefs.internetEnabled"):
            self.assertIn(required, source)
        callback = source.split("fun start(", 1)[1].split("suspend fun drainPendingQueueNow", 1)[0]
        self.assertIn("PendingLinkCheckWorker.schedule(appCtx)", callback)
        self.assertNotIn("drainPendingQueueNow(appCtx", callback)

    def test_default_role_is_not_offered_with_placeholder_mms(self):
        source = (BASE / "MainActivity.kt").read_text()
        setup = source.split("fun requestDefaultSmsRole()", 1)[1].split("fun requestCompanionSmsPermissions()", 1)[0]
        self.assertNotIn("roleLauncher.launch", setup)
        self.assertIn("MMS reception is implemented", setup)
        self.assertIn("companion mode", setup)
        self.assertIn("fun switchBackToPreviousSmsApp()", source)

if __name__ == "__main__":
    unittest.main()
