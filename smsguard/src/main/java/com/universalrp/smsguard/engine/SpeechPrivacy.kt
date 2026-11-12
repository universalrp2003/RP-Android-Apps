package com.universalrp.smsguard.engine

import java.text.Normalizer

/** Last common speech boundary, plus full-body screening before any summarization/truncation.
 * Conservative heuristics, not a universal secret detector: unlabelled words, novel formats or
 * mislabelled numbers can be indistinguishable from ordinary text. Consent is still necessary.
 */
object SpeechPrivacy {
    private val secrets = Regex("(?i)\\b(?:o[. ]*t[. ]*p|one[ -]?time[ -]?(?:password|code)|(?:verification|security|login|auth(?:entication)?|secret)[ -]?code|passcode|password|pin|mpin|cvv|cvc)\\b|\\bcode\\s*(?:is|:|=)?\\s*[0-9]{4,8}\\b|\\bG-[0-9]{6}\\b|<#>|கடவுச்சொல்|ஒருமுறை.*குறியீடு|ரகசிய.*எண்|ஓடிபி")
    private val account = Regex("(?i)((?:\\b(?:a/c|acct\\.?|account|card|iban|ending(?:\\s+(?:with|in))?)|கணக்கு(?:\\s*எண்)?|அட்டை(?:\\s*எண்)?)\\s*(?:(?:number|no)\\.?\\s*)?[:#=-]?\\s*)(?:[x*•]+\\s*)?(?:[0-9x*•][ -]?){3,40}")
    private val masked = Regex("(?i)(?<![a-z0-9])(?:x{2,}|\\*{2,}|•{2,})[ -]*[0-9]{2,8}(?![0-9])")
    // Redact unlabelled 4+ digit numbers too: account suffixes/short secrets/phones/references.
    private val number = Regex("(?<![\\p{L}0-9])[0-9][0-9 -]*[0-9](?![0-9])")
    private val money = Regex("(?i)(?:rupees|rs\\.?|inr|₹|ரூபாய்)\\s*[0-9,]+(?:\\.[0-9]{1,2})?")
    private val urls = Regex("(?i)https?://\\S+")

    fun prepare(raw: String): String? {
        if (raw.length > 32_768) return null // Fail closed on oversized input; never truncate then classify.
        val normalized = Normalizer.normalize(raw, Normalizer.Form.NFKC)
            .filterNot { Character.getType(it) == Character.FORMAT.toInt() }
            .map { if (Character.isDigit(it)) Character.digit(it, 10).let { n -> if (n >= 0) ('0'.code + n).toChar() else it } else it }
            .joinToString("")
        // Classify the ENTIRE original utterance, never just its first 180/1200 characters.
        if (secrets.containsMatchIn(normalized)) return null
        var text = urls.replace(normalized, "[link]")
        text = account.replace(text) { it.groupValues[1] + "[private] " }
        text = masked.replace(text, "[private]")
        // Protect bounded explicitly labelled amounts by source ranges, not magic placeholders
        // that could collide with arbitrary SMS text. Huge digit strings are not valid amounts.
        val amounts = money.findAll(text).filter { it.value.count(Char::isDigit) <= 11 }.map { it.range }.toList()
        text = number.replace(text) { match ->
            if (match.value.count(Char::isDigit) < 4 || amounts.any { match.range.first >= it.first && match.range.last <= it.last }) match.value else "[private]"
        }
        return text.trim().takeIf { it.isNotEmpty() }
    }

    fun cloudAllowed(internet: Boolean, consent: Boolean, online: Boolean): Boolean = internet && consent && online
}
