package com.universalrp.tamilnadufm.data

import java.net.URI
import java.util.Locale

/**
 * Owner reports Tamil80's provider serving an uninstall/use-our-app recording, 31 Oct2026.
 * Hold the shared psrlive provider family for review, NOT all stations with a Tamilradios logo.
 * No header impersonation, token refresh, announcement stripping or fabricated replacement URL.
 */
object RadioStreamPolicy {
    fun heldForReview(url: String): Boolean {
        val host = runCatching { URI(url.trim()).host?.lowercase(Locale.ROOT)?.trimEnd('.') }.getOrNull() ?: return false
        return host.endsWith(".listenon.in") && host.substringBefore('.').startsWith("psrlive")
    }
    fun canPlay(url: String): Boolean = !heldForReview(url)
    fun notice(tamil: Boolean): String = if (tamil)
        "இந்த வழங்குநரின் ஒலிபரப்பு பரிசீலனையில் உள்ளது. Tamil 80’s நிலையத்தில் வேறு செயலியைப் பயன்படுத்தும் அறிவிப்பு புகாரளிக்கப்பட்டது. வேறு நிலையத்தைத் தேர்ந்தெடுக்கவும்; செயலியை நீக்கத் தேவையில்லை."
    else "Provider stream held for review: Tamil 80’s was reported to play a use-another-app announcement. Choose another station; you do not need to uninstall this app."

    fun playableQueue(urls: List<String>, names: List<String>, index: Int): Triple<List<String>, List<String>, Int>? {
        val entries = urls.mapIndexedNotNull { at, url ->
            if (url.isNotBlank() && canPlay(url)) Triple(at, url, names.getOrElse(at) { "" }) else null
        }
        if (entries.isEmpty()) return null
        val selected = entries.indexOfFirst { it.first == index }.coerceAtLeast(0)
        return Triple(entries.map { it.second }, entries.map { it.third }, selected)
    }
}
