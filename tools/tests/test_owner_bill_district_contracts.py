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

    def test_destination_does_not_promote_district_or_accept_invalid_coordinates(self):
        source = (ROOT / "oorarivippu/src/main/java/com/oorarivippu/voiceannouncer/DestinationResolver.kt").read_text()
        candidate = source.split("private fun candidate(", 1)[1].split("internal fun lastKnownLocation", 1)[0]
        self.assertIn("DestinationSearchPolicy.validCoordinates", candidate)
        self.assertIn("DestinationSearchPolicy.destinationName", candidate)
        self.assertNotIn("listOf(address.featureName, address.locality, address.subLocality, address.subAdminArea)", candidate)

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


if __name__ == "__main__":
    unittest.main()
