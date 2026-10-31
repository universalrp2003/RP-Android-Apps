from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[2]

class SmsRestoredContracts(unittest.TestCase):
    def test_false_bill_review_guard_covers_every_delivery_path(self):
        engine = (ROOT / "smsguard/src/main/java/com/universalrp/smsguard/engine/BillReminderEngine.kt").read_text()
        scheduler = (ROOT / "smsguard/src/main/java/com/universalrp/smsguard/service/BillReminderScheduler.kt").read_text()
        self.assertIn("!it.isPaid && !needsReview(it)", engine)
        immediate = scheduler.split("fun onBillCaptured(", 1)[1].split("private fun deliverPlan(", 1)[0]
        self.assertIn("BillReminderEngine.needsReview(bill)", immediate)
        delivery = scheduler.split("private fun deliverPlan(", 1)[1].split("private fun postUrgentNotification(", 1)[0]
        self.assertIn("plan.urgentBills.filterNot(BillReminderEngine::needsReview)", delivery)
        self.assertIn("plan.routineBills.filterNot(BillReminderEngine::needsReview)", delivery)
        manual = scheduler.split("fun announceAllUnpaidNow(", 1)[1]
        self.assertIn("!BillReminderEngine.needsReview(it)", manual)


    def test_sms_header_probe_never_fetches_a_body_or_bypasses_dns(self):
        source = (ROOT / "smsguard/src/main/java/com/universalrp/smsguard/engine/LinkSecurityScanner.kt").read_text()
        probe = source.split("override fun resolveRedirectsHeaderOnly(startUrl", 1)[1].split("@Volatile", 1)[0]
        self.assertIn('requestMethod = "HEAD"', probe)
        self.assertNotIn('requestMethod = "GET"', probe)
        self.assertNotIn("inputStream", probe)
        self.assertIn("inspectUrlLocally(current", probe)
        self.assertNotIn("https://1.1.1.2/", source)
        self.assertIn("https://security.cloudflare-dns.com/", source)
        self.assertIn('cfSec.optInt("Status", -1)', source)


    def test_sms_does_not_instruct_unlocking_restricted_settings_and_discloses_mms_limit(self):
        catalog = (ROOT / "smsguard/src/main/java/com/universalrp/smsguard/ui/BilingualCatalog.kt").read_text()
        self.assertNotIn("Allow restricted settings", catalog)
        self.assertNotIn("Android 15/16 Unlock", catalog)
        self.assertIn("Incoming MMS is not implemented", catalog)
        self.assertIn("முழுமையான MMS பெறுதல் இல்லை", catalog)
        controls = (ROOT / "smsguard/src/main/java/com/universalrp/smsguard/ui/ControlsTab.kt").read_text()
        self.assertIn("Text(ui.setupBannerBody", controls)


    def test_cached_green_verdicts_are_versioned_and_invalidated_after_failed_rescan(self):
        store = (ROOT / "smsguard/src/main/java/com/universalrp/smsguard/data/GuardSettingsStore.kt").read_text()
        self.assertIn('o.optInt("policyRevision", 0)', store)
        self.assertIn('LinkScanCachePolicy.reusable(verdict,', store)
        self.assertIn('LinkScanCachePolicy.invalidateSafe(', store)
        self.assertNotIn('if (finalVerdicts.isEmpty()) return', store)


