"""Fast integration/directory guards; runtime classifier/speech tests live in the app suites."""
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import verify_apps_localization as gates


class OwnerScreenshotContracts(unittest.TestCase):
    GOOD = "SisterApps.all.forEach { app -> SisterApps.releaseUrl(app); app.publishedVersion }"

    def test_oor_uses_public_directory_without_a_frozen_count(self):
        gates.verify_oor_about_directory(self.GOOD, "Our RP Ideas Android Apps")

    def test_oor_rejects_retired_repository_in_either_source_or_catalog(self):
        for text in ("https://github.com/universalrp2003/InternetRadio", "Legacy Mirror Repository"):
            with self.subTest(text=text), self.assertRaises(ValueError):
                gates.verify_oor_about_directory(self.GOOD + text, "")
            with self.subTest(catalog=text), self.assertRaises(ValueError):
                gates.verify_oor_about_directory(self.GOOD, text)

    def test_oor_rejects_both_languages_frozen_suite_count(self):
        for text in ("Our RP Ideas Android Apps (7)", "எமது RP Ideas செயலிகள் (14)"):
            with self.subTest(text=text), self.assertRaises(ValueError):
                gates.verify_oor_about_directory(self.GOOD, text)

    def test_oor_cannot_link_unpublished_candidate_version(self):
        with self.assertRaises(ValueError):
            gates.verify_oor_about_directory(self.GOOD.replace("app.publishedVersion", "app.version"), "")

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

    def test_oor_keeps_district_history_when_landmark_is_also_announced(self):
        source = (ROOT / "oorarivippu/src/main/java/com/oorarivippu/voiceannouncer/LocationAnnouncementService.kt").read_text()
        self.assertIn("PlaceDataPolicy.addressNames(address.locality, address.subAdminArea)", source)
        self.assertNotIn("listOf(address.locality, address.subAdminArea)", source)
        self.assertIn("logTitles += BilingualStrings.buildDistrictAnnouncement(", source)
        self.assertIn('logTitle = logTitles.joinToString(" · ")', source)
        self.assertNotIn("logTitle = landmark", source)


if __name__ == "__main__":
    unittest.main()
