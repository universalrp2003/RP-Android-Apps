package com.universalrp.smsguard

import com.universalrp.smsguard.engine.*
import com.universalrp.smsguard.service.NeuralCloudVoiceEngine
import org.junit.Assert.*
import org.junit.Test

class SpeechPrivacyTest {
    @Test fun secretClassificationPrecedesTruncationAndHandlesUnicode() {
        for (body in listOf(
            "Your OTP is 482910", "PIN 4321", "card CVV 123", "password hunter-test",
            "482910 " + "ordinary text ".repeat(250) + " verification code",
            "ＯＴＰ ４８２９１０", "O\u200bTP ௪௮௨௯௧௦", "G-482910",
            "code: 482910", "security code 482910", "கடவுச்சொல் 1234",
        )) assertNull(body.take(40), SpeechPrivacy.prepare(body))
    }

    @Test fun numbersAccountsLinksAreMaskedButLabelledAmountsSurvive() {
        val safe = requireNotNull(SpeechPrivacy.prepare(
            "A/c XX1234 credited Rs.1234567.50; account number 9988776655; card ending 4821; call 98765 43210 https://example.com/secret?token=abcdef"))
        for (secret in listOf("1234", "9988776655", "4821", "98765", "example.com")) {
            // The amount deliberately contains 1234: inspect suffixes independently below.
            if (secret != "1234") assertFalse(safe, safe.contains(secret))
        }
        assertTrue(safe, safe.contains("Rs.1234567.50"))
        assertFalse(safe, safe.contains("XX1234"))
        assertTrue(safe, safe.contains("[private]"))
        assertEquals(safe, SpeechPrivacy.prepare(safe))
        assertNull(SpeechPrivacy.prepare("x".repeat(40_000)))
        val longDigits = requireNotNull(SpeechPrivacy.prepare("1".repeat(20_000)))
        assertEquals("[private]", longDigits)
        assertEquals("Lunch costs ₹50", SpeechPrivacy.prepare("Lunch costs ₹50"))
        assertFalse(requireNotNull(SpeechPrivacy.prepare("Call ９８７６５４３２１０")).contains("9876543210"))
        assertFalse(requireNotNull(SpeechPrivacy.prepare("Rs 1234567890123456")).contains("1234567890123456"))
        assertEquals("MONEYTOKENAEND Rs 500", SpeechPrivacy.prepare("MONEYTOKENAEND Rs 500"))
    }

    @Test fun everyConsentInternetAndConnectivityCombinationFailsClosed() {
        for (internet in listOf(false, true)) for (consent in listOf(false, true)) for (online in listOf(false, true)) {
            assertEquals(internet && consent && online, SpeechPrivacy.cloudAllowed(internet, consent, online))
        }
    }

    @Test fun cloudEntryPointsRejectSecretsAndRevokedConsentWithoutNetwork() {
        assertNull(NeuralCloudVoiceEngine.synthesizeRawMp3("OTP 482910", "en-IN-NeerjaNeural", networkAllowed = { true }))
        assertNull(NeuralCloudVoiceEngine.synthesizeRawMp3("A normal preview", "en-IN-NeerjaNeural", networkAllowed = { false }))
        assertNull(NeuralCloudVoiceEngine.synthesizeToFileOrNull(null, "A normal preview", "en-IN-NeerjaNeural", 1, networkAllowed = { false }))
    }

    @Test fun plannerScreensFullBodyEvenWhenCallerSuppliesFalseOtpFlagAndShortMode() {
        val body = "482910 " + "ordinary text ".repeat(250) + " verification code"
        val sender = SenderIdentityEngine.identify("+919000000001", body)
        val finance = FinanceLedgerEngine.parseSingleSms(1L, 1L, body, sender, false)
        assertNull(SmsAnnouncementPlanner.buildIncomingAnnouncement(body, sender, OtpResult(false, null), emptyList(), finance,
            AnnouncementSettingsSnapshot(tamil = false, voiceEnabled = true, readFullNormalSmsEnabled = false, shortEveryMessageEnabled = true)))
    }

    @Test fun optedInSenderOnlyPrivateMessageNoticeSurvivesLastBoundary() {
        val body = "Your OTP is 482910"
        val sender = SenderIdentityEngine.identify("AD-ICICIB-T", body)
        val finance = FinanceLedgerEngine.parseSingleSms(1L, 1L, body, sender, true)
        for (tamil in listOf(false, true)) {
            val script = requireNotNull(SmsAnnouncementPlanner.buildIncomingAnnouncement(body, sender, OtpDetector.analyze(body), emptyList(), finance,
                AnnouncementSettingsSnapshot(tamil = tamil, voiceEnabled = true, announceOtpSenderOnly = true)))
            val safe = requireNotNull(SpeechPrivacy.prepare(script))
            assertFalse(safe.contains("482910"))
        }
    }
}
