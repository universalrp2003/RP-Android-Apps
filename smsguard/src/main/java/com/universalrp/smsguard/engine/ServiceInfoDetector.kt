package com.universalrp.smsguard.engine

import java.util.Locale

/**
 * Detects routine telecom "service status" messages — data balance, remaining SMS,
 * pack validity, talktime, etc. — that owners find annoying when read aloud on every
 * arrival (e.g. "Pack Valid till 10 Dec 2026. Remaining SMS:569 Bal:Rs.0.00.").
 *
 * These are intentionally kept OUT of the voice/notification stream (still shown in the
 * inbox), while genuine bank/UPI transactions, OTPs, scam links and fake-payment alerts
 * are NEVER silenced because the planner checks those categories first.
 *
 * Pure & unit-tested: only the message body is inspected, never the inbox or network.
 */
object ServiceInfoDetector {

    // Strong, telecom-specific status markers. A single match is enough. Deliberately narrow so
    // ordinary personal texts ("your plan is valid till Friday") and bank credits ("...credited,
    // Avl Bal Rs.24,500") are NOT treated as service-status noise.
    private val SERVICE_STATUS_PATTERNS: List<Regex> = listOf(
        Regex("\\bremaining\\s+sms\\b", RegexOption.IGNORE_CASE),
        Regex("\\bsms\\s*(?:left|balance|remaining)\\b", RegexOption.IGNORE_CASE),
        Regex("\\bpack\\s+valid\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:data|internet)\\s+(?:balance|pack|quota)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:remaining|left|balance)\\s+(?:data|internet)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\d+(?:\\.\\d+)?\\s*(?:gb|mb)\\b[^.]{0,24}\\b(?:left|remaining|balance)\\b", RegexOption.IGNORE_CASE),
        Regex("\\btalk\\s*time\\b", RegexOption.IGNORE_CASE),
        // Telecom-style "Bal:Rs.0.00" / "Bal: Rs 0.00" (zero-balance status dumps)
        Regex("\\bbal(?:ance)?\\s*:?\\s*(?:rs|inr|₹)\\.?\\s*0(?:\\.0+)?\\b", RegexOption.IGNORE_CASE),
        Regex("\\bvalidity\\s+(?:till|until|upto|up\\s+to|ends?|expires?)\\b", RegexOption.IGNORE_CASE),
        Regex("\\bunlimited\\s+(?:calls?|data|sms)\\b", RegexOption.IGNORE_CASE),
    )

    /**
     * True when [body] reads like a routine operator data/validity/balance status message.
     * A genuine transaction (credited/debited + amount) is never matched on its own, so the
     * finance announcer keeps announcing the exact received/spent amount.
     */
    fun isServiceInfo(body: String): Boolean {
        if (body.isBlank()) return false
        val bounded = if (body.length > 2000) body.substring(0, 2000) else body
        return SERVICE_STATUS_PATTERNS.any { it.containsMatchIn(bounded) }
    }

    /** Convenience for tests/debug: which pattern fired, or null. */
    fun matchedPattern(body: String): String? {
        if (body.isBlank()) return null
        val bounded = if (body.length > 2000) body.substring(0, 2000) else body
        return SERVICE_STATUS_PATTERNS.firstOrNull { it.containsMatchIn(bounded) }?.pattern
            ?.lowercase(Locale.US)
    }
}
