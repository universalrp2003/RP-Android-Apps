from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
PLANNER = (ROOT / "smsguard/src/main/java/com/universalrp/smsguard/engine/SmsAnnouncementPlanner.kt").read_text(encoding="utf-8")
PREFS = (ROOT / "smsguard/src/main/java/com/universalrp/smsguard/data/GuardSettingsStore.kt").read_text(encoding="utf-8")
PROCESSOR = (ROOT / "smsguard/src/main/java/com/universalrp/smsguard/service/IncomingSmsProcessor.kt").read_text(encoding="utf-8")
CONTROLS = (ROOT / "smsguard/src/main/java/com/universalrp/smsguard/ui/ControlsTab.kt").read_text(encoding="utf-8")
MESSAGES = (ROOT / "smsguard/src/main/java/com/universalrp/smsguard/ui/MessagesTab.kt").read_text(encoding="utf-8")
ACTIVITY = (ROOT / "smsguard/src/main/java/com/universalrp/smsguard/MainActivity.kt").read_text(encoding="utf-8")


class SmsGuardOtpSenderAndProgressContractTest(unittest.TestCase):
    def test_otp_announcement_is_separate_and_off_by_default(self):
        self.assertIn('announceOtpSenderOnly: Boolean = false', PREFS)
        self.assertIn('"announce_otp_sender_only", false', PREFS)
        self.assertIn('if (!settings.announceOtpSenderOnly) return null', PLANNER)
        self.assertIn('Private message from $safeName arrived.', PLANNER)
        self.assertNotIn('otp.code', PLANNER)
        self.assertIn('never its code or message text', CONTROLS)
        self.assertIn('announceOtpSenderOnly = prefs.announceOtpSenderOnly', PROCESSOR)

    def test_inbox_hydration_shows_indeterminate_progress_and_elapsed_time(self):
        self.assertIn('LinearProgressIndicator(', MESSAGES)
        self.assertIn('loadingSeconds++', MESSAGES)
        self.assertIn('Loading saved messages…', MESSAGES)
        self.assertIn('சேமித்த செய்திகளை ஏற்றுகிறது', MESSAGES)
        self.assertIn('isInboxLoading by vm.isLoading.collectAsState()', ACTIVITY)
        self.assertIn('loadedMessageCount = allMessages.size', ACTIVITY)

    def test_small_oem_cursor_pages_and_private_safe_failure_stage(self):
        repository = (ROOT / "smsguard/src/main/java/com/universalrp/smsguard/data/SmsRepository.kt").read_text(encoding="utf-8")
        result = (ROOT / "smsguard/src/main/java/com/universalrp/smsguard/data/InboxLoadResult.kt").read_text(encoding="utf-8")
        view_model = (ROOT / "smsguard/src/main/java/com/universalrp/smsguard/ui/MainViewModel.kt").read_text(encoding="utf-8")
        self.assertIn("CURSOR_PAGE_SIZE = 100", repository)
        self.assertIn("InboxLoadResult.Unavailable(failureStage)", repository)
        self.assertIn("enum class InboxLoadFailureStage", result)
        self.assertIn("Could not refresh messages [$stage]", view_model)
        self.assertIn("SMS bodies, contacts or addresses", repository)


if __name__ == "__main__":
    unittest.main()
