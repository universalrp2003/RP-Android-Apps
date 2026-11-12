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

    def test_sms_only_default_is_explicitly_acknowledged_before_android_chooser(self):
        source = (BASE / "MainActivity.kt").read_text()
        setup = source.split("fun requestDefaultSmsRole()", 1)[1].split("fun requestCompanionSmsPermissions()", 1)[0]
        self.assertIn("DefaultSmsSetupPolicy.next(", setup)
        self.assertIn("settings.smsOnlyDefaultAcknowledged", setup)
        self.assertLess(setup.index("Step.DISCLOSE_MMS_LIMIT"), setup.index("roleLauncher.launch(request)"))
        self.assertIn("createRequestRoleIntent(RoleManager.ROLE_SMS)", setup)
        self.assertIn("ACTION_CHANGE_DEFAULT", setup)
        self.assertIn("SmsOnlyDefaultDisclosure", setup)
        self.assertIn("fun switchBackToPreviousSmsApp()", source)
        callback = source.split("val roleLauncher =", 1)[1].split("val sendPermissionNeeded", 1)[0]
        self.assertNotIn("startActivity(", callback)
        self.assertNotIn("roleLauncher.launch", callback)

    def test_regular_work_does_not_expand_into_unused_foreground_capabilities(self):
        import xml.etree.ElementTree as ET
        manifest = ET.parse(ROOT / "smsguard/src/main/AndroidManifest.xml").getroot()
        android = "{http://schemas.android.com/apk/res/android}"
        tools = "{http://schemas.android.com/tools}"
        permissions = [p for p in manifest.findall("uses-permission")
                       if p.get(android + "name") == "android.permission.FOREGROUND_SERVICE"]
        self.assertEqual(1, len(permissions))
        self.assertEqual("remove", permissions[0].get(tools + "node"))
        foreground = [s for s in manifest.findall("application/service")
                      if s.get(android + "name") == "androidx.work.impl.foreground.SystemForegroundService"]
        self.assertEqual("remove", foreground[0].get(tools + "node"))
        worker = (BASE / "service/PendingLinkCheckWorker.kt").read_text()
        self.assertNotIn(".setExpedited(", worker)
        self.assertNotIn("setForeground(", worker)

    def test_permission_button_does_not_misrepresent_send_contact_and_sim_requests(self):
        catalog = (BASE / "ui/BilingualCatalog.kt").read_text()
        self.assertNotIn("Grant Companion SMS Read/Receive Only", catalog)
        self.assertIn("Review SMS, Contacts & SIM permissions", catalog)
        self.assertIn("Default SMS active — MMS is not supported", catalog)

if __name__ == "__main__":
    unittest.main()
