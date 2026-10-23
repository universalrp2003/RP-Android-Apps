package com.universalrp.smsguard.engine

import java.util.Locale

data class OtpResult(
    val isOtp: Boolean,
    val code: String?,
)

object OtpDetector {

    private val OTP_KEYWORDS = Regex(
        "\\b(otp|one[\\s-]?time[\\s-]?password|verification\\s+code|security\\s+code|" +
            "login\\s+code|auth(?:entication)?\\s+code|passcode|secret\\s+code|" +
            "verify\\s+your\\s+(?:login|account|mobile|number|identity)|mpin\\s+otp|aadhaar\\s+otp)\\b",
        RegexOption.IGNORE_CASE,
    )

    // Google style: "G-482910 is your Google verification code"
    private val GOOGLE_CODE = Regex("\\bG-([0-9]{6})\\b")

    // Bare verification-code phrasings without a qualified keyword, e.g. "your code is 482910",
    // "code: 482910", "code 482910". Biased toward detection: a false positive only silences a
    // non-secret message from voice, whereas a miss would SPEAK a real one-time code aloud.
    private val CODE_WITH_DIGITS = Regex("\\bcode\\b\\s*(?:is|:|=)?\\s*[0-9]{4,8}\\b", RegexOption.IGNORE_CASE)

    // Android SMS Retriever API auto-hash marker at end of message
    private val RETRIEVER_MARKER = Regex("<#>.*\\b([0-9]{4,8})\\b")

    // Scrub patterns that carry numbers which are NOT the OTP itself
    private val SCRUB_NON_OTP_NUMBERS = listOf(
        // Amounts: Rs. 1500.00, INR 500, ₹2500
        Regex("(?:rs\\.?|inr|₹)\\s*[0-9,]+(?:\\.[0-9]{1,2})?", RegexOption.IGNORE_CASE),
        // Masked accounts/cards: XX4821, XXXXX1234, A/c *1234, ending 9012
        Regex("(?:x+|\\*+)[0-9]{3,6}", RegexOption.IGNORE_CASE),
        Regex("(?:ending(?:\\s+with|\\s+in)?|a/c(?:\\s+no\\.?)?|card)\\s*[:\\-]?\\s*[0-9]{4,6}", RegexOption.IGNORE_CASE),
        // Validity durations: valid for 10 mins / 5 minutes / 300 seconds
        Regex("\\b[0-9]{1,3}\\s*(?:min|mins|minute|minutes|sec|secs|seconds|hr|hrs|hours)\\b", RegexOption.IGNORE_CASE),
        // Helpline / toll-free numbers & long 10-16 digit phones/UTRs
        Regex("\\b1800[0-9\\-]{6,10}\\b"),
        Regex("\\b[0-9]{9,18}\\b"),
    )

    private val CANDIDATE_DIGITS = Regex("\\b([0-9]{4,8})\\b")

    fun analyze(rawBody: String): OtpResult {
        if (rawBody.isBlank()) return OtpResult(isOtp = false, code = null)
        val body = if (rawBody.length > 2000) rawBody.substring(0, 2000) else rawBody

        GOOGLE_CODE.find(body)?.let { match ->
            return OtpResult(isOtp = true, code = match.groupValues[1])
        }

        val hasOtpKeyword = OTP_KEYWORDS.containsMatchIn(body) ||
            RETRIEVER_MARKER.containsMatchIn(body) ||
            CODE_WITH_DIGITS.containsMatchIn(body)
        if (!hasOtpKeyword) {
            return OtpResult(isOtp = false, code = null)
        }

        // Guard against false positive such as informational warning: "Never share OTP with anyone" inside a scam/bank advisory with no actual OTP code
        var scrubbed = body
        for (pattern in SCRUB_NON_OTP_NUMBERS) {
            scrubbed = pattern.replace(scrubbed, " ")
        }

        val candidates = CANDIDATE_DIGITS.findAll(scrubbed)
            .map { it.groupValues[1] }
            .filterNot { isLikelyYearOnly(it, body) }
            .toList()

        if (candidates.isEmpty()) {
            val lower = body.lowercase(Locale.US)
            val purelyAdvisory = ("never share" in lower || "do not share your pin/otp" in lower) &&
                !Regex("\\b(is your otp|otp is|otp:)", RegexOption.IGNORE_CASE).containsMatchIn(body)
            return if (purelyAdvisory) {
                OtpResult(isOtp = false, code = null)
            } else {
                OtpResult(isOtp = true, code = null)
            }
        }

        // Prefer 6-digit OTPs first, then 4-digit, then others
        val preferred = candidates.firstOrNull { it.length == 6 }
            ?: candidates.firstOrNull { it.length == 4 }
            ?: candidates.first()

        return OtpResult(isOtp = true, code = preferred)
    }

    private fun isLikelyYearOnly(digits: String, rawBody: String): Boolean {
        if (digits.length != 4) return false
        val num = digits.toIntOrNull() ?: return false
        if (num !in 2020..2035) return false
        // Only treat 2026 as a year if adjacent to a month/date token
        return Regex("\\b[0-9]{1,2}[/-\\s][A-Za-z0-9]{2,9}[/-\\s]$digits\\b").containsMatchIn(rawBody)
    }
}
