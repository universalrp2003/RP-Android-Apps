package com.universalrp.smsguard

import com.universalrp.smsguard.engine.AnnouncementSettingsSnapshot
import com.universalrp.smsguard.engine.DltCategory
import com.universalrp.smsguard.engine.FinanceLedgerEngine
import com.universalrp.smsguard.engine.LinkSecurityScanner
import com.universalrp.smsguard.engine.LinkVerdict
import com.universalrp.smsguard.engine.OtpDetector
import com.universalrp.smsguard.engine.SenderIdentityEngine
import com.universalrp.smsguard.engine.SmsAnnouncementPlanner
import com.universalrp.smsguard.engine.SpoofClassification
import com.universalrp.smsguard.engine.UserAccountProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class SmsGuardEngineTest {

    private val zone = ZoneId.of("Asia/Kolkata")

    private val mockCleanAndPhishTransport = object : LinkSecurityScanner.NetworkTransport {
        override fun resolveRedirectsHeaderOnly(startUrl: String, maxHops: Int): LinkSecurityScanner.RedirectProbeResult {
            return when {
                "bit.ly/scamkyc" in startUrl -> LinkSecurityScanner.RedirectProbeResult(
                    reachable = true,
                    finalUrl = "https://hdfc-pan-verify-now.com/login",
                    statusCode = 200,
                    contentType = "text/html",
                    contentDisposition = null,
                )
                "bit.ly/stealthapk" in startUrl -> LinkSecurityScanner.RedirectProbeResult(
                    reachable = true,
                    finalUrl = "https://files-drop-server.org/download",
                    statusCode = 200,
                    contentType = "application/vnd.android.package-archive",
                    contentDisposition = "attachment; filename=\"EBBill_Update.apk\"",
                )
                "bit.ly/parcel99" in startUrl -> LinkSecurityScanner.RedirectProbeResult(
                    reachable = true,
                    finalUrl = "https://www.indiapost.gov.in/track",
                    statusCode = 200,
                    contentType = "text/html",
                    contentDisposition = null,
                )
                else -> LinkSecurityScanner.RedirectProbeResult(
                    reachable = true,
                    finalUrl = startUrl,
                    statusCode = 200,
                    contentType = "text/html",
                    contentDisposition = null,
                )
            }
        }

        override fun queryDohThreatIntel(host: String): LinkSecurityScanner.ThreatDnsVerdict {
            return if ("evil-phish-portal.org" in host) {
                LinkSecurityScanner.ThreatDnsVerdict.BLOCKED_MALWARE_OR_PHISHING
            } else {
                LinkSecurityScanner.ThreatDnsVerdict.CLEAN
            }
        }

        override fun querySafeBrowsingOptional(url: String, apiKey: String): Boolean = false
    }

    @Test
    fun traiDlt2026HeadersAreIdentifiedAccurately() {
        val sbi = SenderIdentityEngine.identify("JT-SBIINB-T", "Your A/c XX4821 credited")
        assertTrue(sbi.isOfficialDltHeader)
        assertFalse(sbi.isPersonalNumber)
        assertEquals(DltCategory.TRANSACTIONAL, sbi.dltCategory)
        assertEquals("Jio", sbi.operatorEn)
        assertEquals("Tamil Nadu", sbi.circleEn)
        assertEquals("State Bank of India", sbi.displayNameEn)
        assertTrue(sbi.isVerifiedFinancialSender)

        val promo = SenderIdentityEngine.identify("JD-SWIGGY-P", "Flat 50% off tonight!")
        assertEquals(DltCategory.PROMOTIONAL, promo.dltCategory)
        assertFalse(promo.isVerifiedFinancialSender)

        val govt = SenderIdentityEngine.identify("AT-TANGED-G", "Electricity bill generated")
        assertEquals(DltCategory.GOVERNMENT, govt.dltCategory)
        assertTrue(govt.displayNameTa.contains("மின்சார"))

        val personal = SenderIdentityEngine.identify("+91 98765 43210", "Hello nanba")
        assertTrue(personal.isPersonalNumber)
        assertFalse(personal.isOfficialDltHeader)
        assertFalse(personal.isVerifiedFinancialSender)
    }

    @Test
    fun personalNumberFakeBankSmsNeverAddsIncomeAndSpeaksFraudWarning() {
        val scamBody = "Dear Customer, Your A/c XX4821 is credited by Rs. 50,000.00 via GPay UPI Ref 629588776655 -SBI"
        val sender = SenderIdentityEngine.identify("+919840011223", scamBody)
        val otp = OtpDetector.analyze(scamBody)
        val ts = LocalDate.of(2026, 10, 22).atTime(10, 30).atZone(zone).toInstant().toEpochMilli()

        val fin = FinanceLedgerEngine.parseSingleSms(101L, ts, scamBody, sender, otp.isOtp)
        assertEquals(SpoofClassification.SPOOF_PERSONAL_NUMBER_FAKE_MONEY, fin.classification)

        val (mStart, mEnd) = FinanceLedgerEngine.monthBounds(YearMonth.of(2026, 10), zone)
        val summary = FinanceLedgerEngine.summarizePeriod(listOf(fin), mStart, mEnd, zoneId = zone)
        assertEquals(0.0, summary.totalIncome, 0.001)
        assertEquals(0.0, summary.totalSpend, 0.001)
        assertEquals(1, summary.blockedSpoofCount)

        val tamilAlert = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = scamBody,
            sender = sender,
            otp = otp,
            links = emptyList(),
            finance = fin,
            settings = AnnouncementSettingsSnapshot(tamil = true, voiceEnabled = true, shortEveryMessageEnabled = true, mutePromoVoice = true),
        )
        assertNotNull(tamilAlert)
        assertTrue(tamilAlert!!.contains("போலியான பண வரவு"))
    }

    @Test
    fun otpMessagesAreStrictlySilentAndNeverCountedAsSpend() {
        val body = "739204 is your OTP for transaction of Rs.850.00 on HDFC Bank Card XX5521. Valid for 10 mins."
        val sender = SenderIdentityEngine.identify("AD-HDFCBK-T", body)
        val otp = OtpDetector.analyze(body)
        assertTrue(otp.isOtp)
        assertEquals("739204", otp.code)

        val fin = FinanceLedgerEngine.parseSingleSms(202L, 1_700_000_000_000L, body, sender, otp.isOtp)
        assertEquals(SpoofClassification.NOT_FINANCIAL, fin.classification)

        val speechTa = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = body,
            sender = sender,
            otp = otp,
            links = emptyList(),
            finance = fin,
            settings = AnnouncementSettingsSnapshot(tamil = true, voiceEnabled = true, shortEveryMessageEnabled = true, mutePromoVoice = false),
        )
        val speechEn = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = body,
            sender = sender,
            otp = otp,
            links = emptyList(),
            finance = fin,
            settings = AnnouncementSettingsSnapshot(tamil = false, voiceEnabled = true, shortEveryMessageEnabled = true, mutePromoVoice = false),
        )
        assertNull(speechTa)
        assertNull(speechEn)
    }

    @Test
    fun urlExtractorNeverConfusesAmountsOrUpiHandlesWithLinks() {
        val bankText = "Your A/c.XX4821 debited Rs.1,250.50 on 22.10.2026 at 11.45am to ramesh.kumar@oksbi Ref.No.629512349876"
        assertEquals(emptyList<String>(), LinkSecurityScanner.extractUrls(bankText))

        val scamText = "Account blocked! Click bit.ly/scamkyc or install SBI_KYC_Update.apk immediately."
        val extracted = LinkSecurityScanner.extractUrls(scamText)
        assertEquals(listOf("bit.ly/scamkyc", "SBI_KYC_Update.apk"), extracted)
    }

    @Test
    fun linkScannerDetectsApkImpersonationStealthDropThreatDnsAndSafeSites() {
        val personal = SenderIdentityEngine.identify("+919876543210")
        val officialRbi = SenderIdentityEngine.identify("JT-RBISAY-G")

        // 1. Offline APK trap
        val apkRes = LinkSecurityScanner.scanSingleUrl(
            "SBI_KYC_Update.apk",
            sender = personal,
            internetAllowed = true,
            hasValidatedNetwork = false,
            transport = mockCleanAndPhishTransport,
        )
        assertEquals(LinkVerdict.UNSAFE, apkRes.verdict)

        // 2. Shortener hiding a fake HDFC lookalike domain
        val hiddenPhish = LinkSecurityScanner.scanSingleUrl(
            "https://bit.ly/scamkyc",
            sender = personal,
            internetAllowed = true,
            hasValidatedNetwork = true,
            transport = mockCleanAndPhishTransport,
        )
        assertEquals(LinkVerdict.UNSAFE, hiddenPhish.verdict)

        // 3. Stealth APK served behind clean URL via Content-Disposition / MIME
        val stealthApk = LinkSecurityScanner.scanSingleUrl(
            "https://bit.ly/stealthapk",
            sender = personal,
            internetAllowed = true,
            hasValidatedNetwork = true,
            transport = mockCleanAndPhishTransport,
        )
        assertEquals(LinkVerdict.UNSAFE, stealthApk.verdict)

        // 4. Threat DNS blocked domain
        val threatDns = LinkSecurityScanner.scanSingleUrl(
            "https://evil-phish-portal.org/claim",
            sender = personal,
            internetAllowed = true,
            hasValidatedNetwork = true,
            transport = mockCleanAndPhishTransport,
        )
        assertEquals(LinkVerdict.UNSAFE, threatDns.verdict)

        // 5. Clean official HTTPS domain -> Green Tick SAFE
        val cleanRbi = LinkSecurityScanner.scanSingleUrl(
            "https://rbi.org.in",
            sender = officialRbi,
            internetAllowed = true,
            hasValidatedNetwork = true,
            transport = mockCleanAndPhishTransport,
        )
        assertEquals(LinkVerdict.SAFE, cleanRbi.verdict)
    }

    @Test
    fun offlineHonestyThenAutoVerifyOnReconnectAnnouncesProperly() {
        val postSender = SenderIdentityEngine.identify("JT-INDPST-G")

        val whileOffline = LinkSecurityScanner.scanSingleUrl(
            "https://bit.ly/parcel99",
            sender = postSender,
            internetAllowed = true,
            hasValidatedNetwork = false,
            transport = mockCleanAndPhishTransport,
        )
        assertEquals(LinkVerdict.PENDING_OFFLINE, whileOffline.verdict)

        val offlineVoice = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = "Track parcel https://bit.ly/parcel99",
            sender = postSender,
            otp = OtpDetector.analyze("Track parcel"),
            links = listOf(whileOffline),
            finance = FinanceLedgerEngine.parseSingleSms(1L, 1000L, "Track parcel", postSender, false),
            settings = AnnouncementSettingsSnapshot(tamil = false, voiceEnabled = true, shortEveryMessageEnabled = true, mutePromoVoice = true),
        )
        assertNotNull(offlineVoice)
        assertTrue(offlineVoice!!.contains("cannot verify"))

        val afterReconnect = LinkSecurityScanner.scanSingleUrl(
            "https://bit.ly/parcel99",
            sender = postSender,
            internetAllowed = true,
            hasValidatedNetwork = true,
            transport = mockCleanAndPhishTransport,
        )
        assertEquals(LinkVerdict.SAFE, afterReconnect.verdict)
        val reconnectVoice = SmsAnnouncementPlanner.buildReconnectedLinkAnnouncement(
            senderNameEn = postSender.spokenNameEn,
            senderNameTa = postSender.spokenNameTa,
            results = listOf(afterReconnect),
            tamil = false,
        )
        assertNotNull(reconnectVoice)
        assertTrue(reconnectVoice!!.contains("safe to open"))
    }

    @Test
    fun financeSeparatesAccountsDeduplicatesDualUtrAndCalculatesMonthYearCustomRanges() {
        val oct10 = LocalDate.of(2026, 10, 10).atTime(9, 15).atZone(zone).toInstant().toEpochMilli()
        val oct10Plus20s = oct10 + 20_000L
        val oct18 = LocalDate.of(2026, 10, 18).atTime(18, 40).atZone(zone).toInstant().toEpochMilli()
        val jun05 = LocalDate.of(2026, 6, 5).atTime(14, 0).atZone(zone).toInstant().toEpochMilli()

        val customProfiles = listOf(
            UserAccountProfile("sbi_main", "My SBI Savings", "4821", "BANK"),
            UserAccountProfile("iob_shop", "My IOB Account", "9104", "BANK"),
        )

        // Dual alerts for the SAME ₹1,500 credit (SBI Bank SMS + Google Pay SMS sharing UTR 629512349876)
        val sbiSender = SenderIdentityEngine.identify("JT-SBIINB-T")
        val gpaySender = SenderIdentityEngine.identify("AX-GOOGLP-S")
        val iobSender = SenderIdentityEngine.identify("VM-IOBCHN-T")
        val promoSender = SenderIdentityEngine.identify("JD-LOANAD-P")

        val tx1Bank = FinanceLedgerEngine.parseSingleSms(
            1L, oct10,
            "Your A/c XX4821 credited by Rs.1,500.00 on 10Oct26 by UPI Ref 629512349876",
            sbiSender, false, customProfiles,
        )
        val tx1GpayDup = FinanceLedgerEngine.parseSingleSms(
            2L, oct10Plus20s,
            "Rs.1,500.00 credited via Google Pay to ramesh@oksbi UPI Ref 629512349876",
            gpaySender, false, customProfiles,
        )
        // Separate PhonePe spend from IOB ••9104 in October: ₹420
        val tx2Spend = FinanceLedgerEngine.parseSingleSms(
            3L, oct18,
            "Your A/c XX9104 is debited by Rs.420.00 paid to merchant@ybl via PhonePe Ref 629900001111",
            iobSender, false, customProfiles,
        )
        // Earlier June income in SBI ••4821: ₹10,000
        val tx3JuneIncome = FinanceLedgerEngine.parseSingleSms(
            4L, jun05,
            "Your A/c XX4821 credited by Rs.10,000.00 salary deposit",
            sbiSender, false, customProfiles,
        )
        // Promo loan trap in October: Rs. 2,00,000 (MUST BE REJECTED!)
        val tx4PromoTrap = FinanceLedgerEngine.parseSingleSms(
            5L, oct18,
            "Congratulations! Rs.2,00,000 credited to your loan offer! Click now",
            promoSender, false, customProfiles,
        )

        val deduped = FinanceLedgerEngine.deduplicateAndLink(listOf(tx1Bank, tx1GpayDup, tx2Spend, tx3JuneIncome, tx4PromoTrap))

        // October summary: ₹1,500 income (NOT ₹3,000!), ₹420 spend
        val (octStart, octEnd) = FinanceLedgerEngine.monthBounds(YearMonth.of(2026, 10), zone)
        val octSummary = FinanceLedgerEngine.summarizePeriod(deduped, octStart, octEnd, zoneId = zone)
        assertEquals(1500.0, octSummary.totalIncome, 0.001)
        assertEquals(420.0, octSummary.totalSpend, 0.001)
        assertEquals(1080.0, octSummary.netBalance, 0.001)

        // Separate Account Filter check: IOB Account alone shows ₹0 income, ₹420 spend
        val iobOnly = FinanceLedgerEngine.summarizePeriod(deduped, octStart, octEnd, selectedAccountKey = "USER:iob_shop", zoneId = zone)
        assertEquals(0.0, iobOnly.totalIncome, 0.001)
        assertEquals(420.0, iobOnly.totalSpend, 0.001)

        // Yearly 2026 summary: ₹11,500 income (₹10,000 June + ₹1,500 Oct), ₹420 spend, all 12 months populated
        val (yrStart, yrEnd) = FinanceLedgerEngine.yearBounds(2026, zone)
        val yrSummary = FinanceLedgerEngine.summarizePeriod(deduped, yrStart, yrEnd, yearForMonthlyBars = 2026, zoneId = zone)
        assertEquals(11500.0, yrSummary.totalIncome, 0.001)
        assertEquals(420.0, yrSummary.totalSpend, 0.001)
        assertEquals(12, yrSummary.monthlyBreakdown.size)
        assertEquals(10000.0, yrSummary.monthlyBreakdown[5].income, 0.001) // June = index 5
        assertEquals(1500.0, yrSummary.monthlyBreakdown[9].income, 0.001)  // Oct = index 9
    }

    @Test
    fun manifestDeclaresExactRoleSmsPermissionsAndReceivers() {
        val manifestFile = sequenceOf(
            java.io.File("src/main/AndroidManifest.xml"),
            java.io.File("smsguard/src/main/AndroidManifest.xml"),
        ).firstOrNull { it.isFile }
        assertNotNull("AndroidManifest.xml must be readable in unit test", manifestFile)
        val xml = manifestFile!!.readText()
        // Exact permission required by AOSP RoleManager.ROLE_SMS on HeadlessSmsSendService
        assertTrue(xml.contains("android.permission.SEND_RESPOND_VIA_MESSAGE"))
        assertTrue(xml.contains("android.permission.USE_BIOMETRIC"))
        assertTrue(xml.contains("android.provider.Telephony.SMS_DELIVER"))
        assertTrue(xml.contains("android.provider.Telephony.WAP_PUSH_DELIVER"))
        assertTrue(xml.contains("android.intent.action.RESPOND_VIA_MESSAGE"))
        assertTrue(xml.contains("android.intent.action.SENDTO"))
        assertTrue(xml.contains("android.permission.WRITE_SMS"))
    }

    @Test
    fun whatsappJobSalaryScamViaDltHeaderIsBlockedImmediatelyAndNeverAddsMoney() {
        val scamBody = "Dear Sir/Mam, Interface We are looking for Staff and Salary is Rs38900 on Device api.whatsapp.com/send?phone=916299919364 Meghbela"
        val sender = SenderIdentityEngine.identify("AD-MCBSPL-S", scamBody)
        val links = LinkSecurityScanner.scanAllInMessage(
            body = scamBody,
            sender = sender,
            internetAllowed = true,
            hasValidatedNetwork = true,
            transport = mockCleanAndPhishTransport,
        )
        assertEquals(1, links.size)
        assertEquals(LinkVerdict.UNSAFE, links[0].verdict)
        assertTrue(links[0].displayDomain.contains("+916299919364"))

        val fin = FinanceLedgerEngine.parseSingleSms(501L, 1_760_000_000_000L, scamBody, sender, isOtp = false)
        assertEquals(SpoofClassification.NOT_FINANCIAL, fin.classification)
    }

    @Test
    fun sbiFastagTopUpAndTollDeductionsAreSeparatedFromSbiSavingsAndVoiceNeverReadsAccountDigits() {
        val oct22 = LocalDate.of(2026, 10, 22).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

        // 1. Real SBI Savings Debit of Rs. 500 to recharge FASTag (with A/c XX1453)
        val sbiBankSender = SenderIdentityEngine.identify("AT-SBIPSG-T", "Your A/c XX1453 debited by Rs.500.00 towards FASTag")
        val bankDebit = FinanceLedgerEngine.parseSingleSms(
            601L, oct22,
            "Your A/c XX1453 is debited by Rs.500.00 on 22Oct26 Ref 629911223344",
            sbiBankSender, false,
        )

        // 2. SBI FASTag Wallet Credit of Rs. 500 (MUST NOT count as fake ₹500 Monthly Income!)
        val fastagSender = SenderIdentityEngine.identify("AT-SBIFST-T", "Rs.500.00 credited to your SBI FASTag")
        val fastagTopUp = FinanceLedgerEngine.parseSingleSms(
            602L, oct22 + 5_000L,
            "Rs.500.00 credited to your SBI FASTag linked to TN55AB1234. Avl Bal Rs.720.00",
            fastagSender, false,
        )

        // 3. SBI FASTag Highway Toll Deduction of Rs. 115 at Toll Plaza (MUST be separated into FASTag Toll bucket, NOT added twice to Bank Spend!)
        val fastagToll = FinanceLedgerEngine.parseSingleSms(
            603L, oct22 + 3_600_000L,
            "Rs.115.00 deducted from your SBI FASTag at Ulundurpet Toll Plaza for Vehicle No TN55AB1234",
            fastagSender, false,
        )

        val (octStart, octEnd) = FinanceLedgerEngine.monthBounds(YearMonth.of(2026, 10), zone)
        val summary = FinanceLedgerEngine.summarizePeriod(listOf(bankDebit, fastagTopUp, fastagToll), octStart, octEnd, zoneId = zone)

        assertEquals(0.0, summary.totalIncome, 0.001)     // Top-up NEVER counted as fake income!
        assertEquals(500.0, summary.totalSpend, 0.001)    // Bank spend = true ₹500 (not ₹615 double-count!)
        assertEquals(115.0, summary.fastagTollTotal, 0.001) // Highway toll cleanly tracked separately!
        assertEquals(1, summary.fastagTollCount)

        // Selecting "FASTAG:SBI" shows exact ₹115 highway toll spend
        val fastagOnly = FinanceLedgerEngine.summarizePeriod(
            listOf(bankDebit, fastagTopUp, fastagToll), octStart, octEnd, selectedAccountKey = "FASTAG:SBI", zoneId = zone,
        )
        assertEquals(115.0, fastagOnly.totalSpend, 0.001)

        // Voice announcement check: ONLY Bank Name + Debited/Received + Exact Amount — ZERO "1453" or "XX1453"!
        val enVoice = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = "Your A/c XX1453 is debited by Rs.500.00 on 22Oct26 Ref 629911223344",
            sender = sbiBankSender,
            otp = OtpDetector.analyze("Your A/c XX1453 is debited"),
            links = emptyList(),
            finance = bankDebit,
            settings = AnnouncementSettingsSnapshot(tamil = false, voiceEnabled = true, shortEveryMessageEnabled = true, mutePromoVoice = true),
        )
        assertEquals("State Bank of India. Rupees 500 debited.", enVoice)
        assertFalse(enVoice!!.contains("1453"))
    }

    @Test
    fun defaultShortSmsVoiceIsOffAndSleepScheduleQueuesAndReadsBankAlertsOneByOne() {
        // 1. Default Short Announcement for Every SMS MUST be OFF (false) out of the box!
        val defaults = com.universalrp.smsguard.data.GuardPrefs()
        assertFalse(defaults.shortEveryMessageEnabled)

        // 2. Overnight window: 10:00 PM (1320) -> 7:00 AM (420)
        assertTrue(com.universalrp.smsguard.service.SleepScheduleManager.isMinuteInSleepWindow(23 * 60 + 30, 22 * 60, 7 * 60))
        assertTrue(com.universalrp.smsguard.service.SleepScheduleManager.isMinuteInSleepWindow(5 * 60 + 45, 22 * 60, 7 * 60))
        assertFalse(com.universalrp.smsguard.service.SleepScheduleManager.isMinuteInSleepWindow(7 * 60, 22 * 60, 7 * 60))
        assertFalse(com.universalrp.smsguard.service.SleepScheduleManager.isMinuteInSleepWindow(15 * 60, 22 * 60, 7 * 60))

        // 3. Daytime rest/test window: 4:00 PM (960) -> 4:30 PM (990)
        assertTrue(com.universalrp.smsguard.service.SleepScheduleManager.isMinuteInSleepWindow(16 * 60 + 10, 16 * 60, 16 * 60 + 30))
        assertFalse(com.universalrp.smsguard.service.SleepScheduleManager.isMinuteInSleepWindow(16 * 60 + 30, 16 * 60, 16 * 60 + 30))

        // 4. Strictly 12-hour AM/PM clock formatting
        assertEquals("10:00 PM", com.universalrp.smsguard.service.SleepScheduleManager.formatTwelveHourMinuteOfDay(22 * 60))
        assertEquals("7:00 AM", com.universalrp.smsguard.service.SleepScheduleManager.formatTwelveHourMinuteOfDay(7 * 60))

        // 5. Sequential one-by-one wake-up bank readout
        val items = listOf(
            com.universalrp.smsguard.data.QueuedSleepAnnouncement(
                id = "a1",
                timestampMs = 1_000L,
                scriptTa = "ஸ்டேட் பேங்க் ஆஃப் இந்தியா. ரூபாய் 500 செலவானது.",
                scriptEn = "State Bank of India. Rupees 500 debited.",
            ),
            com.universalrp.smsguard.data.QueuedSleepAnnouncement(
                id = "a2",
                timestampMs = 2_000L,
                scriptTa = "இந்தியன் ஓவர்சீஸ் வங்கி. ரூபாய் 1,200 வரவு வந்துள்ளது.",
                scriptEn = "Indian Overseas Bank. Rupees 1,200 received.",
            ),
        )
        val seqEn = com.universalrp.smsguard.service.SleepScheduleManager.buildSequentialReadoutUtterances(items, tamil = false)
        assertEquals(3, seqEn.size)
        assertEquals("Message 1. State Bank of India. Rupees 500 debited.", seqEn[1])
        assertEquals("Message 2. Indian Overseas Bank. Rupees 1,200 received.", seqEn[2])
    }
}
