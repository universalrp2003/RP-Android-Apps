package com.universalrp.smsguard

import com.universalrp.smsguard.engine.AnnouncementSettingsSnapshot
import com.universalrp.smsguard.engine.BillCategory
import com.universalrp.smsguard.engine.BillReminder
import com.universalrp.smsguard.engine.BillReminderEngine
import com.universalrp.smsguard.engine.DltCategory
import com.universalrp.smsguard.engine.FinanceLedgerEngine
import com.universalrp.smsguard.engine.LinkSecurityScanner
import com.universalrp.smsguard.engine.LinkVerdict
import com.universalrp.smsguard.engine.OtpDetector
import com.universalrp.smsguard.engine.SenderIdentityEngine
import com.universalrp.smsguard.engine.ServiceInfoDetector
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

    @Test
    fun massiveTenThousandSmsInboxDeduplicatesInMillisecondsWithoutLagOrOom() {
        val baseTs = 1_760_000_000_000L
        val sbi = SenderIdentityEngine.identify("JT-SBIINB-T")
        val items = (1..10_000).map { i ->
            FinanceLedgerEngine.parseSingleSms(
                messageId = i.toLong(),
                timestampMs = baseTs + i * 60_000L,
                body = "Your A/c XX4821 debited by Rs.100.00 Ref ${600000000000L + i}",
                sender = sbi,
                isOtp = false,
            )
        }
        val t0 = System.nanoTime()
        val deduped = FinanceLedgerEngine.deduplicateAndLink(items)
        val elapsedMs = (System.nanoTime() - t0) / 1_000_000L
        assertEquals(10_000, deduped.size)
        assertTrue("10,000 SMS deduplication must finish well under 1500ms, took ${elapsedMs}ms", elapsedMs < 1500L)
    }

    @Test
    fun secMsGecV2MatchesOfficialEdgeTts731VectorsBitForBit() {
        assertEquals(
            "05CD31D13308C082-CA0B7B171AAF7C429C0ECC6927617FCA41076C993B856256E35DB43B651320B5",
            com.universalrp.smsguard.service.NeuralCloudVoiceEngine.generateSecMsGecV2(1792660800.0),
        )
        assertEquals(
            "B9563096D63AAF0C-A28069EF5114F534B5879E4305BF38B192D4D2102C9D91B31BB4006BDD793521",
            com.universalrp.smsguard.service.NeuralCloudVoiceEngine.generateSecMsGecV2(1792661100.0),
        )
        assertEquals(
            "99E54519B490BE5C-E162DB0977FBDCB9B86DD8148BF1BCDC3C0303176CE39852A42B415935C20654",
            com.universalrp.smsguard.service.NeuralCloudVoiceEngine.generateSecMsGecV2(1800000000.0),
        )
    }

    @Test
    fun dailyMaleFemaleVoiceAlternatesStrictlyEveryDayAcrossYear() {
        val engine = com.universalrp.smsguard.service.NeuralCloudVoiceEngine
        assertEquals(3, engine.MALE_VOICES.size)
        assertEquals(3, engine.FEMALE_VOICES.size)

        val seenIds = mutableSetOf<String>()
        var day = java.time.LocalDate.of(2026, 10, 22)
        repeat(366) {
            val todayProfile = engine.scheduledVoiceForDate(day)
            val tomorrowProfile = engine.scheduledVoiceForDate(day.plusDays(1))
            seenIds.add(todayProfile.id)
            assertTrue(
                "Consecutive days must strictly alternate Male <-> Female (${day} vs ${day.plusDays(1)})",
                todayProfile.gender != tomorrowProfile.gender,
            )
            day = day.plusDays(1)
        }
        assertEquals("All 6 curated Male & Female HD voices must participate in daily rotation", 6, seenIds.size)
    }

    @Test
    fun outgoingNeftBeneficiarySettlementIsNeverCountedAsIncomeAndAnnouncesTransferDelivered() {
        val neftBody = "ICICI BANK NEFT Transaction with reference number IN12629660952897 for Rs. 700.00 has been credited to the beneficiary account on 23-10-2026 at 09:02:27"
        val sender = SenderIdentityEngine.identify("VM-ICICIB-S", neftBody)
        val otp = OtpDetector.analyze(neftBody)
        val ts = LocalDate.of(2026, 10, 23).atTime(9, 2, 27).atZone(zone).toInstant().toEpochMilli()
        val parsed = FinanceLedgerEngine.parseSingleSms(
            messageId = 9001L,
            timestampMs = ts,
            body = neftBody,
            sender = sender,
            isOtp = otp.isOtp,
        )
        assertEquals(SpoofClassification.OUTGOING_TRANSFER_SETTLEMENT, parsed.classification)
        assertNull("Beneficiary settlement confirmation MUST NOT have CREDIT or DEBIT direction", parsed.direction)
        assertEquals(700.0, parsed.amountInr, 0.001)

        // Ledger summary must report 0 income and 0 spend for the settlement confirmation itself
        val (mStart, mEnd) = FinanceLedgerEngine.monthBounds(YearMonth.of(2026, 10), zone)
        val summary = FinanceLedgerEngine.summarizePeriod(listOf(parsed), mStart, mEnd, zoneId = zone)
        assertEquals(0.0, summary.totalIncome, 0.001)
        assertEquals(0.0, summary.totalSpend, 0.001)

        // Voice announcement must confirm transfer reached beneficiary (never say received!)
        val taSpeech = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = neftBody,
            sender = sender,
            otp = otp,
            links = emptyList(),
            finance = parsed,
            settings = AnnouncementSettingsSnapshot(tamil = true, voiceEnabled = true),
        )
        val enSpeech = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = neftBody,
            sender = sender,
            otp = otp,
            links = emptyList(),
            finance = parsed,
            settings = AnnouncementSettingsSnapshot(tamil = false, voiceEnabled = true),
        )
        assertNotNull(taSpeech)
        assertNotNull(enSpeech)
        assertTrue(taSpeech!!.contains("பெறுநர் கணக்கில் சேர்ந்துவிட்டது"))
        assertFalse(taSpeech.contains("வரவு வந்துள்ளது"))
        assertTrue(enSpeech!!.contains("reached the beneficiary's account"))
        assertFalse(enSpeech.contains("received"))
    }

    @Test
    fun senderOnlyVoiceAndReadFullNormalSmsModesWorkSafelyWhilePreservingOtpSilenceAndScamAlerts() {
        val bankBody = "Your A/c XX4821 is credited with Rs. 1500.00 on 23-10-2026."
        val bankSender = SenderIdentityEngine.identify("VM-ICICIB-S", bankBody)
        val bankOtp = OtpDetector.analyze(bankBody)
        val bankFin = FinanceLedgerEngine.parseSingleSms(1L, 1_000L, bankBody, bankSender, bankOtp.isOtp)

        val senderOnlyTa = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = bankBody,
            sender = bankSender,
            otp = bankOtp,
            links = emptyList(),
            finance = bankFin,
            settings = AnnouncementSettingsSnapshot(tamil = true, voiceEnabled = true, senderOnlyVoiceEnabled = true),
        )
        val senderOnlyEn = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = bankBody,
            sender = bankSender,
            otp = bankOtp,
            links = emptyList(),
            finance = bankFin,
            settings = AnnouncementSettingsSnapshot(tamil = false, voiceEnabled = true, senderOnlyVoiceEnabled = true),
        )
        assertEquals("ICICI வங்கியில் இருந்து குறுஞ்செய்தி.", senderOnlyTa)
        assertEquals("SMS from ICICI Bank.", senderOnlyEn)

        // Personal friend full message readout
        val friendBody = "வணக்கம் நண்பா! இன்று மாலை 6 மணிக்கு திருச்சி பேருந்து நிலையம் அருகில் சந்திப்போம். மறக்காமல் வாருங்கள்!"
        val friendSender = SenderIdentityEngine.identify("+919876543210", friendBody, savedContactName = "Ramesh Prathap")
        val friendOtp = OtpDetector.analyze(friendBody)
        val friendFin = FinanceLedgerEngine.parseSingleSms(2L, 2_000L, friendBody, friendSender, friendOtp.isOtp)

        val fullReadTa = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = friendBody,
            sender = friendSender,
            otp = friendOtp,
            links = emptyList(),
            finance = friendFin,
            settings = AnnouncementSettingsSnapshot(tamil = true, voiceEnabled = true, readFullNormalSmsEnabled = true),
        )
        assertNotNull(fullReadTa)
        assertTrue(fullReadTa!!.contains("Ramesh Prathap"))
        assertTrue(fullReadTa.contains("மறக்காமல் வாருங்கள்"))

        // OTP must STILL stay 100% silent even when both senderOnlyVoice & readFullNormalSms are ON!
        val otpBody = "Your OTP for login is 849201. Do not share with anyone."
        val otpSender = SenderIdentityEngine.identify("VM-ICICIB-S", otpBody)
        val otpRes = OtpDetector.analyze(otpBody)
        val otpFin = FinanceLedgerEngine.parseSingleSms(3L, 3_000L, otpBody, otpSender, otpRes.isOtp)
        assertNull(
            SmsAnnouncementPlanner.buildIncomingAnnouncement(
                body = otpBody,
                sender = otpSender,
                otp = otpRes,
                links = emptyList(),
                finance = otpFin,
                settings = AnnouncementSettingsSnapshot(
                    tamil = true,
                    voiceEnabled = true,
                    senderOnlyVoiceEnabled = true,
                    readFullNormalSmsEnabled = true,
                ),
            ),
        )
    }

    @Test
    fun rationShopSchoolResultsAndAllGovernmentSmsAnnounceCompletelyByDefaultAndThreadsSortNewestTop() {
        // 1. Default GuardPrefs MUST read full normal/government SMS out of the box!
        val defaultPrefs = com.universalrp.smsguard.data.GuardPrefs()
        assertTrue("Full normal/government SMS voice readout must be ON by default", defaultPrefs.readFullNormalSmsEnabled)

        // 2. TN Ration Shop (JM-TNEPDS-S) real message check
        val pdsBody = "PDS Bill 131671026012619 P.Oil-1.0pkt issued on 23.10.2026 for Rs.25.0/-"
        val pdsSender = SenderIdentityEngine.identify("JM-TNEPDS-S", pdsBody)
        assertEquals("TN Ration Shop (TNEPDS)", pdsSender.displayNameEn)
        assertEquals("தமிழ்நாடு ரேஷன் கடை", pdsSender.displayNameTa)
        val pdsOtp = OtpDetector.analyze(pdsBody)
        val pdsFin = FinanceLedgerEngine.parseSingleSms(41L, 1_761_200_000_000L, pdsBody, pdsSender, pdsOtp.isOtp)
        val pdsSpeechTa = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = pdsBody,
            sender = pdsSender,
            otp = pdsOtp,
            links = emptyList(),
            finance = pdsFin,
            settings = AnnouncementSettingsSnapshot(tamil = true, voiceEnabled = true),
        )
        assertNotNull("Ration shop SMS must announce in full by default", pdsSpeechTa)
        assertTrue(pdsSpeechTa!!.contains("தமிழ்நாடு ரேஷன் கடை"))
        assertTrue(pdsSpeechTa.contains("பாமாயில்"))
        assertTrue(pdsSpeechTa.contains("ரூபாய் 25"))
        assertFalse("15-digit raw receipt ID must not be read digit by digit", pdsSpeechTa.contains("131671026012619"))

        // 3. TN School Education HSC Exam Results (AX-TNSEDU-S) full marks readout check
        val examBody = "HSC RESULTS 2026, ROLL No : 3406958, Name : INIYANEYA R, LAN : TAM- 96, ENG : 86, Sub3 : PHY - 91, Sub4 : CHE - 91, Sub5 : CSC - 100, Sub6 : MAT - 93, TOT - 557, Pass -TNSEDU"
        val examSender = SenderIdentityEngine.identify("AX-TNSEDU-S", examBody)
        assertEquals("TN School Education Dept", examSender.displayNameEn)
        assertEquals("தமிழ்நாடு பள்ளிக் கல்வித் துறை", examSender.displayNameTa)
        val examOtp = OtpDetector.analyze(examBody)
        val examFin = FinanceLedgerEngine.parseSingleSms(42L, 1_761_210_000_000L, examBody, examSender, examOtp.isOtp)
        val examSpeechTa = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = examBody,
            sender = examSender,
            otp = examOtp,
            links = emptyList(),
            finance = examFin,
            settings = AnnouncementSettingsSnapshot(tamil = true, voiceEnabled = true),
        )
        assertNotNull("HSC exam result SMS must announce completely from start to finish", examSpeechTa)
        assertTrue(examSpeechTa!!.contains("தமிழ்நாடு பள்ளிக் கல்வித் துறை"))
        assertTrue(examSpeechTa.contains("தமிழ் 96"))
        assertTrue(examSpeechTa.contains("கணினி அறிவியல் 100"))
        assertTrue(examSpeechTa.contains("மொத்த மதிப்பெண் 557"))
        assertTrue(examSpeechTa.contains("Pass"))

        // 4. Multi-message thread view MUST put NEWEST message at index 0 (TOP) and OLDEST at bottom!
        val iciciSender = SenderIdentityEngine.identify("VM-ICICIB-S", "ICICI alert")
        val oct14Msg = com.universalrp.smsguard.data.EnrichedSmsMessage(
            id = 100L,
            threadId = 7L,
            rawAddress = "VM-ICICIB-S",
            threadKey = iciciSender.normalizedAddress,
            body = "Older Oct 14 ICICI message",
            dateMs = 1_760_400_000_000L,
            isSentByMe = false,
            isUnread = false,
            subId = 1,
            sender = iciciSender,
            otp = com.universalrp.smsguard.engine.OtpResult(false, null),
            links = emptyList(),
            finance = FinanceLedgerEngine.parseSingleSms(100L, 1_760_400_000_000L, "Older Oct 14 ICICI message", iciciSender, false),
        )
        val oct15Msg = oct14Msg.copy(
            id = 101L,
            body = "Newer Oct 15 ICICI message",
            dateMs = 1_760_490_000_000L,
        )
        val grouped = com.universalrp.smsguard.data.SmsRepository.groupThreads(listOf(oct14Msg, oct15Msg))
        assertEquals(1, grouped.size)
        assertEquals("Newest message MUST be at index 0 (TOP) of opened thread", 101L, grouped[0].messages[0].id)
        assertEquals("Older message MUST follow below newer message", 100L, grouped[0].messages[1].id)

        // 5. Shared Weather Alert speech MUST include both village/town AND district details!
        val rainSpeech = com.universalrp.weather.WeatherAlertPolicy.formatSpeech(
            severity = com.universalrp.weather.WeatherSeverity.RAIN_SOON,
            placeLabel = "ஆலங்குடி",
            minutesUntilRain = 30,
            placeDetails = "புதுக்கோட்டை மாவட்டம், தமிழ்நாடு 622301",
        )
        assertTrue(rainSpeech.second.contains("ஆலங்குடி"))
        assertTrue(rainSpeech.second.contains("புதுக்கோட்டை மாவட்டம்"))
    }

    @Test
    fun routineDataValidityServiceStatusSmsAreSilencedWhileBankAmountStillAnnounces() {
        // 1. Detector correctly flags routine telecom status dumps and ignores ordinary texts.
        assertTrue(ServiceInfoDetector.isServiceInfo("Pack Valid till 10 Dec 2026. Remaining SMS:569 Bal:Rs.0.00."))
        assertTrue(ServiceInfoDetector.isServiceInfo("Dear customer, you have used 2.5 GB. 2 GB data left. Validity till 31-Dec."))
        assertTrue(ServiceInfoDetector.isServiceInfo("Your talktime balance is Rs.0. Recharge now."))
        assertFalse("A friend's casual message must never be treated as service noise", ServiceInfoDetector.isServiceInfo("Your plan is valid till Friday, let's catch up!"))
        assertFalse("A real bank credit is not a service-status dump", ServiceInfoDetector.isServiceInfo("Your A/c XX4821 credited by Rs.500.00 Avl Bal Rs.24800.00"))

        // 2. Service-status message: SILENT when the owner filter is ON, read when OFF.
        val statusBody = "Pack Valid till 10 Dec 2026. Remaining SMS:569 Bal:Rs.0.00."
        val airtelSender = SenderIdentityEngine.identify("JM-AIRTEL-S", statusBody)
        val statusOtp = OtpDetector.analyze(statusBody)
        val statusFin = FinanceLedgerEngine.parseSingleSms(7001L, 1_761_300_000_000L, statusBody, airtelSender, statusOtp.isOtp)

        val mutedTa = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = statusBody, sender = airtelSender, otp = statusOtp, links = emptyList(), finance = statusFin,
            settings = AnnouncementSettingsSnapshot(tamil = true, voiceEnabled = true, muteServiceInfoVoice = true),
        )
        val allowedTa = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = statusBody, sender = airtelSender, otp = statusOtp, links = emptyList(), finance = statusFin,
            settings = AnnouncementSettingsSnapshot(tamil = true, voiceEnabled = true, muteServiceInfoVoice = false),
        )
        assertNull("Routine data/validity status SMS must stay silent when the filter is ON", mutedTa)
        assertNotNull("Owner can still opt to hear service-status SMS", allowedTa)

        // 3. A genuine bank credit STILL announces the exact received amount (never silenced by the filter).
        val creditBody = "Your A/c XX4821 credited by Rs.500.00 on 10Oct26 by UPI Ref 629512349876"
        val sbiSender = SenderIdentityEngine.identify("JT-SBIINB-T", creditBody)
        val creditOtp = OtpDetector.analyze(creditBody)
        val creditFin = FinanceLedgerEngine.parseSingleSms(7002L, 1_761_300_000_000L, creditBody, sbiSender, creditOtp.isOtp)
        val creditVoice = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = creditBody, sender = sbiSender, otp = creditOtp, links = emptyList(), finance = creditFin,
            settings = AnnouncementSettingsSnapshot(tamil = false, voiceEnabled = true, muteServiceInfoVoice = true),
        )
        assertNotNull(creditVoice)
        assertTrue(creditVoice!!.contains("500"))
        assertTrue(creditVoice.contains("received"))
    }

    @Test
    fun fullReadoutNeverSpeaksAvailableBalanceOnlyTheRelevantAmount() {
        // Non-financial service sender + a body carrying both a spent amount and an available balance:
        // full-readout must keep the amount but drop the "Available balance Rs.24800" clause entirely.
        val body = "Your last UPI payment of Rs.250.00 was successful. Available balance Rs.24,800.00 in your wallet."
        val sender = SenderIdentityEngine.identify("AD-WALLET-S", body)
        val otp = OtpDetector.analyze(body)
        val fin = FinanceLedgerEngine.parseSingleSms(7100L, 1_761_300_000_000L, body, sender, otp.isOtp)

        val speechEn = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = body, sender = sender, otp = otp, links = emptyList(), finance = fin,
            settings = AnnouncementSettingsSnapshot(tamil = false, voiceEnabled = true, readFullNormalSmsEnabled = true),
        )
        assertNotNull(speechEn)
        assertTrue("The transaction amount must still be spoken", speechEn!!.contains("250"))
        assertFalse("The words 'available balance' must never be spoken", speechEn.contains("balance", ignoreCase = true))
        assertFalse("The balance figure must never be spoken", speechEn.contains("24,800") || speechEn.contains("24800"))
    }

    @Test
    fun bareCodePhrasingIsTreatedAsOtpAndNeverSpoken() {
        // "your code is 482910" (no qualified keyword) must STILL be detected as an OTP so RULE 1 keeps it silent.
        val body = "Hi, your code is 482910. It expires in 10 minutes. Do not share it with anyone."
        val otp = OtpDetector.analyze(body)
        assertTrue("A bare one-time code must be recognised as an OTP", otp.isOtp)
        assertEquals("482910", otp.code)

        val sender = SenderIdentityEngine.identify("AD-GENERL-T", body)
        val speech = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = body,
            sender = sender,
            otp = otp,
            links = emptyList(),
            finance = FinanceLedgerEngine.parseSingleSms(8001L, 1L, body, sender, otp.isOtp),
            settings = AnnouncementSettingsSnapshot(tamil = false, voiceEnabled = true),
        )
        assertNull("A one-time code must NEVER be spoken aloud", speech)

        // Ordinary 'code' usage with no nearby digits must NOT be treated as an OTP.
        assertFalse(OtpDetector.analyze("Enter your area code to continue browsing the shop.").isOtp)
    }

    @Test
    fun otpSenderAnnouncementIsExplicitOptInAndNeverContainsTheVerificationCode() {
        val body = "Your ICICI Bank OTP is 482910. Do not share it."
        val otp = OtpDetector.analyze(body)
        assertTrue(otp.isOtp)
        assertEquals("482910", otp.code)
        val sender = SenderIdentityEngine.identify("AD-ICICIB-T", body)
        val finance = FinanceLedgerEngine.parseSingleSms(8010L, 2L, body, sender, otp.isOtp)

        val defaultSpeech = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = body,
            sender = sender,
            otp = otp,
            links = emptyList(),
            finance = finance,
            settings = AnnouncementSettingsSnapshot(tamil = false, voiceEnabled = true),
        )
        assertNull("OTP sender voice must be opt-in", defaultSpeech)

        val english = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = body,
            sender = sender,
            otp = otp,
            links = emptyList(),
            finance = finance,
            settings = AnnouncementSettingsSnapshot(
                tamil = false,
                voiceEnabled = true,
                announceOtpSenderOnly = true,
            ),
        )
        assertNotNull(english)
        val englishScript = requireNotNull(english)
        assertTrue(englishScript.contains("ICICI", ignoreCase = true))
        assertFalse("The verification digits must never enter the speech script", englishScript.contains("482910"))
        assertFalse("OTP body must never be read", englishScript.contains("Do not share"))

        val tamil = SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = body,
            sender = sender,
            otp = otp,
            links = emptyList(),
            finance = finance,
            settings = AnnouncementSettingsSnapshot(
                tamil = true,
                voiceEnabled = true,
                announceOtpSenderOnly = true,
            ),
        )
        assertNotNull(tamil)
        val tamilScript = requireNotNull(tamil)
        assertFalse("Tamil speech must also omit the verification digits", tamilScript.contains("482910"))
    }

    @Test
    fun serviceHeaderNamingABankCannotAnnounceFakeMoneyButTransactionalCan() {
        val body = "Federal Bank: Your A/c XX1234 credited Rs.500.00 on 23Oct26"
        val tHeader = SenderIdentityEngine.identify("XY-ZZWFT-T", body)
        val sHeader = SenderIdentityEngine.identify("XY-ZZWFT-S", body)

        // A genuine -T transactional header from an (unlisted) bank is still trusted.
        assertTrue(tHeader.isVerifiedFinancialSender)
        // A -S service header merely NAMING a bank must NOT be treated as a real money sender (anti-spoof).
        assertFalse(sHeader.isVerifiedFinancialSender)

        // Therefore the -S fake-money message is NOT recorded as a genuine credit...
        val finS = FinanceLedgerEngine.parseSingleSms(8002L, 1L, body, sHeader, isOtp = false)
        assertEquals(SpoofClassification.NOT_FINANCIAL, finS.classification)
        // ...while the -T one IS a genuine credit.
        val finT = FinanceLedgerEngine.parseSingleSms(8003L, 1L, body, tHeader, isOtp = false)
        assertEquals(SpoofClassification.GENUINE_BANK_OR_UPI, finT.classification)
    }

    @Test
    fun liveOnlineNeuralStreamingSynthesizesRealMp3WhenNetworkAvailable() {
        val reachable = runCatching {
            java.net.InetAddress.getByName("speech.platform.bing.com")
            true
        }.getOrDefault(false)
        assertTrue("Live probe DNS unavailable: synthesis was not executed; no successful audio claimed", reachable)

        val voicesToVerify = listOf(
            "ta-IN-ValluvarNeural" to "வணக்கம்! குறுஞ்செய்தி காவலன் தயாராக உள்ளது.",
            "ta-IN-PallaviNeural" to "ஸ்டேட் பேங்க் ஆஃப் இந்தியா. ரூபாய் 500 வரவு.",
            "en-IN-PrabhatNeural" to "State Bank of India. Rupees 500 received.",
            "en-IN-NeerjaNeural" to "SMS Guard online voice protection active.",
        )
        for ((voiceId, sampleText) in voicesToVerify) {
            val diagnostics = java.util.concurrent.CopyOnWriteArrayList<String>()
            val mp3 = com.universalrp.smsguard.service.NeuralCloudVoiceEngine.synthesizeRawMp3(
                text = sampleText,
                voiceName = voiceId,
                timeoutMs = 20000L,
                diagnostic = { diagnostics.add(it) },
            )
            assertTrue(
                "Expected valid streaming MP3 audio (>1500 bytes) from $voiceId, got ${mp3?.size}; diagnostics=${diagnostics.joinToString(" | ")}",
                mp3 != null && mp3.size > 1500,
            )
        }
    }

    // --- Bill / payment-due reminders ---

    private val billToday: LocalDate = LocalDate.of(2026, 10, 24)

    private fun billNowMs(): Long =
        billToday.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    private fun bill(
        id: String,
        due: LocalDate,
        paid: Boolean = false,
        announcedEpochDay: Long? = null,
        biller: String = "X",
        voice: Boolean = true,
        notify: Boolean = true,
    ): BillReminder = BillReminder(
        id = id,
        contentKey = "$id|key",
        billerEn = biller,
        billerTa = biller,
        amountInr = 500.0,
        dueEpochDay = due.toEpochDay(),
        category = BillCategory.OTHER,
        sourceMessageId = 1L,
        isPaid = paid,
        voiceEnabled = voice,
        notifyEnabled = notify,
        createdAtMs = billNowMs(),
        lastAnnouncedEpochDay = announcedEpochDay,
    )

    @Test
    fun billDueSmsAreParsedIntoRemindersWhileSpoofsAndReceiptsAreRejected() {
        val tneb = SenderIdentityEngine.identify(
            "AT-TANGED-G",
            "TNEB: Electricity bill of Rs.850.00 for Oct 2026 is due on 27-10-2026. Pay before due date to avoid disconnection. -TANGEDCO",
        )
        val eb = BillReminderEngine.parseBillSms(
            "TNEB: Electricity bill of Rs.850.00 for Oct 2026 is due on 27-10-2026. Pay before due date to avoid disconnection. -TANGEDCO",
            tneb, false, billNowMs(), zone,
        )
        assertNotNull("A genuine TNEB bill must parse", eb)
        assertEquals(850.0, eb!!.amountInr!!, 0.001)
        assertEquals(LocalDate.of(2026, 10, 27), eb.dueDate)
        assertEquals(BillCategory.ELECTRICITY, eb.category)

        // Credit card: total amount due must win over the smaller minimum due.
        val ccBody = "HDFC Bank Credit Card XX5521 statement generated. Total amount due Rs.12,450. Minimum amount due Rs.620. Payment due date 28-10-2026."
        val hdfc = SenderIdentityEngine.identify("AD-HDFCBK-T", ccBody)
        val cc = BillReminderEngine.parseBillSms(ccBody, hdfc, false, billNowMs(), zone)
        assertNotNull(cc)
        assertEquals(12450.0, cc!!.amountInr!!, 0.001)
        assertEquals(BillCategory.CREDIT_CARD, cc.category)

        // Postpaid mobile with "DD Mon YYYY".
        val airBody = "Dear Customer, your Airtel postpaid bill of Rs.399 is due on 26 Oct 2026. Pay now to avoid late fee."
        val airtel = SenderIdentityEngine.identify("JM-AIRTEL-S", airBody)
        val air = BillReminderEngine.parseBillSms(airBody, airtel, false, billNowMs(), zone)
        assertNotNull(air)
        assertEquals(LocalDate.of(2026, 10, 26), air!!.dueDate)
        assertEquals(BillCategory.MOBILE, air.category)

        // Indane gas with "DD-Mon-YY".
        val gasBody = "Indane gas refill due date 27-Oct-26. Amount Rs.805. Book your cylinder."
        val indane = SenderIdentityEngine.identify("AT-INDGAS-G", gasBody)
        val gas = BillReminderEngine.parseBillSms(gasBody, indane, false, billNowMs(), zone)
        assertNotNull(gas)
        assertEquals(LocalDate.of(2026, 10, 27), gas!!.dueDate)
        assertEquals(BillCategory.GAS, gas.category)

        // Anti-spoof: personal number, promo, paid-receipt, OTP and plain bank transactions are all rejected.
        val personal = SenderIdentityEngine.identify("+919876543210", "Your electricity bill Rs.850 due on 27-10-2026 pay now")
        assertNull(BillReminderEngine.parseBillSms("Your electricity bill Rs.850 due on 27-10-2026 pay now", personal, false, billNowMs(), zone))

        val promo = SenderIdentityEngine.identify("JD-LOANAD-P", "Bill due Rs.500 pay by 27-10-2026")
        assertNull(BillReminderEngine.parseBillSms("Bill due Rs.500 pay by 27-10-2026", promo, false, billNowMs(), zone))

        val receipt = "Your HDFC credit card bill of Rs.12000 has been paid. Thank you for your payment."
        assertNull(BillReminderEngine.parseBillSms(receipt, hdfc, false, billNowMs(), zone))
        assertNull(BillReminderEngine.parseBillSms("Payment of Rs.12000 received towards your credit card bill.", hdfc, false, billNowMs(), zone))

        val otp = OtpDetector.analyze("482910 is your OTP for bill payment of Rs.850")
        assertNull(BillReminderEngine.parseBillSms("482910 is your OTP for bill payment of Rs.850", tneb, otp.isOtp, billNowMs(), zone))

        val debit = "Your A/c XX4821 is debited by Rs.500.00 on 24-10-2026 Ref 629512349876 Avl Bal Rs.24800"
        val sbi = SenderIdentityEngine.identify("JT-SBIINB-T", debit)
        assertNull("A bank debit is a transaction, never a bill reminder", BillReminderEngine.parseBillSms(debit, sbi, false, billNowMs(), zone))
    }

    @Test
    fun nearDueBillsAlwaysAnnounceWhileRoutineBillsAreCappedAndDeDuplicated() {
        val reminders = listOf(
            bill("u_today", billToday, biller = "today"),
            bill("u_tmr", billToday.plusDays(1), biller = "tomorrow"),
            bill("overdue", billToday.minusDays(3), biller = "overdue"),
            bill("r4", billToday.plusDays(4), biller = "routine4"),
            bill("r7", billToday.plusDays(7), biller = "routine7"),
            bill("far", billToday.plusDays(20), biller = "far"),
            bill("paid", billToday.plusDays(1), paid = true, biller = "paid"),
        )

        val plan = BillReminderEngine.planDailyAnnouncements(reminders, billToday, routineAlreadySpokenToday = 0, maxDailyRoutine = 2)
        assertEquals(setOf("u_today", "u_tmr", "overdue"), plan.urgentBills.map { it.id }.toSet())
        assertEquals(setOf("r4", "r7"), plan.routineBills.map { it.id }.toSet())

        // Routine is capped: once 2 routine announcements already happened today, routine is empty but urgent still fires.
        val capped = BillReminderEngine.planDailyAnnouncements(reminders, billToday, routineAlreadySpokenToday = 2, maxDailyRoutine = 2)
        assertTrue(capped.routineBills.isEmpty())
        assertEquals(setOf("u_today", "u_tmr", "overdue"), capped.urgentBills.map { it.id }.toSet())

        // A bill already announced today is de-duplicated (not re-announced).
        val todayEd = billToday.toEpochDay()
        val dedup = BillReminderEngine.planDailyAnnouncements(
            listOf(bill("done", billToday.plusDays(1), announcedEpochDay = todayEd, biller = "done")),
            billToday, routineAlreadySpokenToday = 0, maxDailyRoutine = 2,
        )
        assertTrue(dedup.urgentBills.isEmpty())
    }

    @Test
    fun billAnnouncementScriptsAndNotificationsAreBilingualAndNeverSpeakAccountDigits() {
        val eb = bill("eb", billToday.plusDays(1), biller = "TNEB Electricity").copy(
            billerTa = "தமிழ்நாடு மின்சாரம்",
            amountInr = 850.0,
        )
        val urgentTa = BillReminderEngine.buildUrgentScript(listOf(eb), billToday, tamil = true)
        val urgentEn = BillReminderEngine.buildUrgentScript(listOf(eb), billToday, tamil = false)
        assertTrue(urgentTa.contains("தமிழ்நாடு மின்சாரம்"))
        assertTrue(urgentTa.contains("ரூபாய் 850"))
        assertTrue(urgentTa.contains("நாளை"))
        assertTrue(urgentEn.contains("TNEB Electricity"))
        assertTrue(urgentEn.contains("Rupees 850"))
        assertTrue(urgentEn.contains("tomorrow"))

        val (titleTa, bodyTa) = BillReminderEngine.buildUrgentNotification(listOf(eb), billToday, tamil = true)
        val (titleEn, bodyEn) = BillReminderEngine.buildUrgentNotification(listOf(eb), billToday, tamil = false)
        assertTrue(titleTa.contains("பில்"))
        assertTrue(bodyTa.contains("தமிழ்நாடு மின்சாரம்"))
        assertTrue(titleEn.contains("bill"))

        // Routine script for a bill due in 5 days.
        val r = bill("r", billToday.plusDays(5), biller = "Airtel")
        val routineEn = BillReminderEngine.buildRoutineScript(listOf(r), billToday, tamil = false)
        assertTrue(routineEn.contains("Airtel"))
        assertTrue(routineEn.contains("in 5 days"))
    }
}
