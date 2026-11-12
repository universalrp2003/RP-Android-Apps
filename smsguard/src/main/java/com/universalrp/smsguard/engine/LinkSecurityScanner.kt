package com.universalrp.smsguard.engine

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.IDN
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

enum class LinkVerdict {
    SAFE,
    UNSAFE,
    PENDING_OFFLINE,
    PENDING_INTERNET_OFF,
}

data class LinkScanResult(
    val originalUrl: String,
    val normalizedUrl: String,
    val finalUrl: String,
    val displayDomain: String,
    val verdict: LinkVerdict,
    val reasonEn: String,
    val reasonTa: String,
    val checkedAtMs: Long = System.currentTimeMillis(),
)

/**
 * Multi-layer SMS URL scanner (online checks disclose URLs/request metadata):
 *  - Does not upload the complete SMS body. URLs may themselves contain personal tokens.
 *  - Inspects only extracted URL hostnames, HTTP redirect headers (zero JS/WebView),
 *    encrypted DNS-over-HTTPS threat intelligence (Cloudflare Security),
 *    and optional user-supplied Google Safe Browsing API v4 key.
 */
object LinkSecurityScanner {

    interface NetworkTransport {
        fun resolveRedirectsHeaderOnly(startUrl: String, maxHops: Int = 5): RedirectProbeResult
        fun queryDohThreatIntel(host: String): ThreatDnsVerdict
        fun querySafeBrowsingOptional(url: String, apiKey: String): Boolean?
    }

    data class RedirectProbeResult(
        val reachable: Boolean,
        val finalUrl: String,
        val statusCode: Int,
        val contentType: String?,
        val contentDisposition: String?,
        val tlsFailed: Boolean = false,
        val errorMessage: String? = null,
    )

    enum class ThreatDnsVerdict {
        CLEAN,
        BLOCKED_MALWARE_OR_PHISHING,
        DOMAIN_DOES_NOT_EXIST,
        NETWORK_UNAVAILABLE,
    }

    private val DANGEROUS_EXTENSIONS = setOf(
        ".apk", ".xapk", ".apkm", ".apks", ".dex", ".exe", ".msi", ".bat", ".cmd", ".scr", ".vbs", ".jar",
    )

    private val HIGH_ABUSE_TLDS = setOf(
        "top", "icu", "xyz", "buzz", "monster", "rest", "cfd", "sbs", "cyou", "vip", "loan", "click", "tk", "ml", "ga", "cf", "gq",
    )

    private val SHORTENER_HOSTS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "t.ly", "cutt.ly", "rb.gy", "is.gd", "v.gd", "shorturl.at",
        "tiny.cc", "ow.ly", "rebrand.ly", "goo.su", "s.id", "tr.ee", "fkrt.it", "amzn.in", "g.co",
        "youtu.be", "vm.tiktok.com", "lnkd.in", "airtl.in", "jio.com", "bharatpe.in",
    )

    // Official root domains for protected Indian banking, UPI, telecom & government brands.
    // Any URL impersonating these brand names on a DIFFERENT domain is flagged immediately as phishing!
    private val PROTECTED_BRAND_ROOTS: Map<String, Set<String>> = mapOf(
        "sbi" to setOf("sbi.co.in", "onlinesbi.sbi", "sbi", "sbicard.com", "sbigeneral.in", "sbimf.com", "yonobusiness.sbi"),
        "onlinesbi" to setOf("onlinesbi.sbi", "sbi.co.in"),
        "yono" to setOf("onlinesbi.sbi", "sbi.co.in", "yonobusiness.sbi"),
        "hdfc" to setOf("hdfcbank.com", "hdfc.com", "hdfclife.com", "hdfcergo.com", "hdfcbank.co.in", "pz.in"),
        "icici" to setOf("icicibank.com", "iciciprulife.com", "icicilombard.com", "icicidirect.com"),
        "axisbank" to setOf("axisbank.com", "axisbank.co.in", "axismf.com"),
        "canarabank" to setOf("canarabank.com", "canarabank.in"),
        "indianbank" to setOf("indianbank.in", "indianbank.net.in"),
        "iob" to setOf("iob.in", "iobnet.co.in"),
        "kvb" to setOf("kvb.co.in", "kvbin.com"),
        "kotak" to setOf("kotak.com", "kotak811.com", "kotaksecurities.com"),
        "indusind" to setOf("indusind.com"),
        "idfcfirst" to setOf("idfcfirstbank.com"),
        "paytm" to setOf("paytm.com", "paytmbank.com", "p-y.tm"),
        "phonepe" to setOf("phonepe.com"),
        "googlepay" to setOf("google.com", "pay.google.com", "g.co"),
        "gpay" to setOf("google.com", "pay.google.com", "g.co"),
        "npci" to setOf("npci.org.in", "bhimupi.org.in"),
        "aadhaar" to setOf("uidai.gov.in", "myaadhaar.uidai.gov.in", "gov.in"),
        "uidai" to setOf("uidai.gov.in", "gov.in"),
        "incometax" to setOf("incometax.gov.in", "incometaxindia.gov.in", "gov.in"),
        "parivahan" to setOf("parivahan.gov.in", "gov.in", "nic.in"),
        "echallan" to setOf("parivahan.gov.in", "gov.in", "nic.in"),
        "indiapost" to setOf("indiapost.gov.in", "ippbonline.com", "gov.in"),
        "tangedco" to setOf("tangedco.org", "tangedco.gov.in", "tnpdcl.org", "tnebnet.org"),
        "tneb" to setOf("tnebnet.org", "tangedco.org", "tangedco.gov.in", "tnpdcl.org"),
        "irctc" to setOf("irctc.co.in", "indianrail.gov.in"),
        "epfo" to setOf("epfindia.gov.in", "gov.in"),
    )

    private val EXPLICIT_URL_REGEX = Regex(
        "\\bhttps?://[^\\s<>\"'()\\[\\]]+",
        RegexOption.IGNORE_CASE,
    )

    // Scheme-less domains & bare .apk malware traps in SMS (e.g. bit.ly/abc12, SBI_KYC_Update.apk, 103.21.44.9/login)
    private val BARE_DOMAIN_REGEX = Regex(
        "(?<![@A-Za-z0-9._-])(?:(?:[a-zA-Z0-9](?:[a-zA-Z0-9_-]{0,61}[a-zA-Z0-9])?\\.)+(?:[a-zA-Z]{2,12}|xn--[a-zA-Z0-9]{2,})|(?:[0-9]{1,3}\\.){3}[0-9]{1,3})(?::[0-9]{2,5})?(?:/[^\\s<>\"'()\\[\\]]*)?",
    )

    private val NON_DOMAIN_TOKENS = setOf(
        "rs", "inr", "mr", "mrs", "ms", "dr", "st", "no", "ac", "avl", "bal", "ref", "utr", "txn", "id",
        "am", "pm", "min", "hrs", "eg", "ie", "ext", "ver", "ph", "mob", "tel", "dt", "dtd",
    )

    private val VALID_COMMON_TLDS = setOf(
        "in", "com", "org", "net", "gov", "nic", "edu", "ac", "co", "sbi", "bank", "io", "me", "ly", "gl",
        "it", "to", "cc", "gy", "gd", "at", "su", "id", "ee", "app", "page", "dev", "info", "biz", "online",
        "site", "store", "tech", "cloud", "ai", "uk", "us", "au", "ca", "sg", "ae", "apk",
        "top", "icu", "xyz", "buzz", "monster", "rest", "cfd", "sbs", "cyou", "vip", "loan", "click", "link",
        "shop", "live", "pro", "support", "help", "tk", "ml", "ga", "cf", "gq",
    )

    val defaultNetworkTransport: NetworkTransport = LiveHttpDohTransport

    /**
     * Extracts genuine links from SMS text while strictly rejecting amounts (Rs.500.00),
     * masked accounts (A/c.XX1234), UPI IDs (ramesh@oksbi) and timestamps (11.30am).
     */
    fun extractUrls(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val safeText = if (text.length > 2000) text.substring(0, 2000) else text
        val found = LinkedHashSet<String>()
        val explicitRanges = ArrayList<IntRange>()

        for (match in EXPLICIT_URL_REGEX.findAll(safeText)) {
            explicitRanges.add(match.range)
            cleanTrailingPunctuation(match.value).takeIf { it.isNotBlank() }?.let { found.add(it) }
        }

        for (match in BARE_DOMAIN_REGEX.findAll(safeText)) {
            if (explicitRanges.any { match.range.first >= it.first && match.range.last <= it.last }) continue
            val rawCandidate = cleanTrailingPunctuation(match.value)
            if (rawCandidate.isBlank()) continue
            // Reject if immediately followed by '@' (left side of an email/UPI address like john.doe@oksbi)
            val nextIdx = match.range.first + rawCandidate.length
            if (nextIdx < safeText.length && safeText[nextIdx] == '@') continue

            if (isGenuineBareUrl(rawCandidate)) {
                found.add(rawCandidate)
            }
        }
        return found.toList()
    }

    private fun isGenuineBareUrl(candidate: String): Boolean {
        val hostPart = candidate.substringBefore('/').substringBefore('?').substringBefore(':')
        val labels = hostPart.split('.')
        if (labels.size < 2) return false

        // Raw IPv4 address (require a path or port to avoid matching dates like 22.10.20.26 unless valid octets with path)
        if (labels.size == 4 && labels.all { it.toIntOrNull() in 0..255 }) {
            return '/' in candidate || ':' in candidate
        }

        val firstLabel = labels.first().lowercase(Locale.US)
        val lastLabel = labels.last().lowercase(Locale.US)
        if (firstLabel in NON_DOMAIN_TOKENS && '/' !in candidate) return false
        if (labels.any { it.isEmpty() }) return false
        if (firstLabel.all { it.isDigit() }) return false

        // Stand-alone ".apk" file mention in phishing text ("Download RTO_Challan.apk") OR valid TLD domain
        if (lastLabel == "apk" || lastLabel == "xapk") return true
        return lastLabel in VALID_COMMON_TLDS
    }

    private fun cleanTrailingPunctuation(raw: String): String {
        var result = raw.trim()
        while (result.isNotEmpty() && result.last() in charArrayOf('.', ',', ';', ':', '!', ')', ']', '>', '"', '\'')) {
            result = result.dropLast(1)
        }
        return result
    }

    fun scanAllInMessage(
        body: String,
        sender: SenderIdentity,
        internetAllowed: Boolean,
        hasValidatedNetwork: Boolean,
        safeBrowsingApiKey: String? = null,
        transport: NetworkTransport = defaultNetworkTransport,
        deferOnlineChecks: Boolean = false,
    ): List<LinkScanResult> {
        return extractUrls(body).map { url ->
            scanSingleUrl(
                rawUrl = url,
                sender = sender,
                internetAllowed = internetAllowed,
                hasValidatedNetwork = hasValidatedNetwork,
                safeBrowsingApiKey = safeBrowsingApiKey,
                messageBody = body,
                transport = transport,
                deferOnlineChecks = deferOnlineChecks,
            )
        }
    }

    fun scanSingleUrl(
        rawUrl: String,
        sender: SenderIdentity? = null,
        internetAllowed: Boolean,
        hasValidatedNetwork: Boolean,
        safeBrowsingApiKey: String? = null,
        messageBody: String = "",
        transport: NetworkTransport = defaultNetworkTransport,
        deferOnlineChecks: Boolean = false,
    ): LinkScanResult {
        val trimmed = cleanTrailingPunctuation(rawUrl)
        val hasExplicitScheme = trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        val normalizedUrl = if (hasExplicitScheme) trimmed else "https://$trimmed"

        // Direct bare ".apk" file trap without domain (e.g. "SBI_KYC_Update.apk")
        if (!hasExplicitScheme && trimmed.lowercase(Locale.US).endsWith(".apk") && trimmed.count { it == '.' } == 1) {
            return LinkScanResult(
                originalUrl = rawUrl,
                normalizedUrl = normalizedUrl,
                finalUrl = normalizedUrl,
                displayDomain = trimmed,
                verdict = LinkVerdict.UNSAFE,
                reasonEn = "Dangerous .APK app file trap detected — never install apps sent by SMS!",
                reasonTa = "ஆபத்தான .APK செயலி கோப்பு! குறுஞ்செய்தியில் வரும் செயலியை ஒருபோதும் நிறுவாதீர்கள் — தொடாதீர்கள்!",
            )
        }

        // Layer 1: Instant local cyber-forensics on the initial URL (works 100% offline too!)
        inspectUrlLocally(
            candidateUrl = normalizedUrl,
            originalExplicitHttp = trimmed.startsWith("http://", true),
            sender = sender,
            messageBody = messageBody,
        )?.let {
            return it.copy(originalUrl = rawUrl)
        }

        // Layer 2: Respect user's Internet Access switch & real device connectivity honestly
        val initialHost = extractHost(normalizedUrl) ?: trimmed
        if (!internetAllowed) {
            return LinkScanResult(
                originalUrl = rawUrl,
                normalizedUrl = normalizedUrl,
                finalUrl = normalizedUrl,
                displayDomain = initialHost,
                verdict = LinkVerdict.PENDING_INTERNET_OFF,
                reasonEn = "Internet access is turned OFF in SMS Guard — cannot verify online right now. Do not touch yet.",
                reasonTa = "குறுஞ்செய்தி காவலனில் இணைய அணுகல் நிறுத்தப்பட்டுள்ளது — இப்போது சரிபார்க்க முடியவில்லை. தொடாதீர்கள்.",
            )
        }

        if (!hasValidatedNetwork) {
            return LinkScanResult(
                originalUrl = rawUrl,
                normalizedUrl = normalizedUrl,
                finalUrl = normalizedUrl,
                displayDomain = initialHost,
                verdict = LinkVerdict.PENDING_OFFLINE,
                reasonEn = "No internet right now — cannot verify this link yet. Will auto-verify once internet returns.",
                reasonTa = "இப்போது இணைய இணைப்பு இல்லை — சரிபார்க்க முடியவில்லை. இணையம் வந்தவுடன் தானாகச் சரிபார்த்து சொல்கிறேன்.",
            )
        }

        if (deferOnlineChecks) {
            return LinkScanResult(
                originalUrl = rawUrl, normalizedUrl = normalizedUrl, finalUrl = normalizedUrl,
                displayDomain = initialHost, verdict = LinkVerdict.PENDING_OFFLINE,
                reasonEn = "Queued for background verification. No completed online verdict yet; do not touch.",
                reasonTa = "பின்னணிச் சரிபார்ப்பிற்குக் காத்திருக்கிறது. இணையச் சோதனை இன்னும் முடியவில்லை; தொடாதீர்கள்.",
            )
        }

        // Layer 3: Online header-only unshortener + content-type inspection + threat-intel DNS + Safe Browsing
        val redirectProbe = transport.resolveRedirectsHeaderOnly(normalizedUrl)
        val effectiveFinalUrl = redirectProbe.finalUrl.ifBlank { normalizedUrl }
        val finalHost = extractHost(effectiveFinalUrl) ?: initialHost

        // Re-run full local brand-impersonation / APK / homograph / chat-lure checks against UNSHORTENED destination!
        inspectUrlLocally(
            candidateUrl = effectiveFinalUrl,
            originalExplicitHttp = effectiveFinalUrl.startsWith("http://", ignoreCase = true),
            sender = sender,
            messageBody = messageBody,
        )?.let {
            return it.copy(originalUrl = rawUrl, normalizedUrl = normalizedUrl, finalUrl = effectiveFinalUrl)
        }

        // Catch stealth APK drops served behind innocent links via Content-Type / Content-Disposition headers!
        val cType = redirectProbe.contentType?.lowercase(Locale.US).orEmpty()
        val cDisp = redirectProbe.contentDisposition?.lowercase(Locale.US).orEmpty()
        if ("application/vnd.android.package-archive" in cType ||
            DANGEROUS_EXTENSIONS.any { cDisp.contains(it) }
        ) {
            return LinkScanResult(
                originalUrl = rawUrl,
                normalizedUrl = normalizedUrl,
                finalUrl = effectiveFinalUrl,
                displayDomain = finalHost,
                verdict = LinkVerdict.UNSAFE,
                reasonEn = "Hidden malicious APK download disguised inside link ($finalHost) — DO NOT TOUCH!",
                reasonTa = "இணைப்பிற்குள் மறைக்கப்பட்ட ஆபத்தான APK வைரஸ் கோப்பு ($finalHost) — தயவுசெய்து தொடாதீர்கள்!",
            )
        }

        if (redirectProbe.tlsFailed) {
            return LinkScanResult(
                originalUrl = rawUrl,
                normalizedUrl = normalizedUrl,
                finalUrl = effectiveFinalUrl,
                displayDomain = finalHost,
                verdict = LinkVerdict.UNSAFE,
                reasonEn = "Invalid or forged HTTPS security certificate on $finalHost — unsafe, do not touch!",
                reasonTa = "$finalHost தளத்தின் பாதுகாப்புச் சான்றிதழ் போலியானது அல்லது காலாவதியானது — தொடாதீர்கள்!",
            )
        }

        // Query Cloudflare Security on BOTH initial & final host; unavailable is not clean.
        val hostsToVerify = linkedSetOf(initialHost.lowercase(Locale.US), finalHost.lowercase(Locale.US))
        for (host in hostsToVerify) {
            when (transport.queryDohThreatIntel(host)) {
                ThreatDnsVerdict.BLOCKED_MALWARE_OR_PHISHING -> {
                    return LinkScanResult(
                        originalUrl = rawUrl,
                        normalizedUrl = normalizedUrl,
                        finalUrl = effectiveFinalUrl,
                        displayDomain = finalHost,
                        verdict = LinkVerdict.UNSAFE,
                        reasonEn = "Blocked as known phishing / malware threat ($host) — DO NOT TOUCH!",
                        reasonTa = "சைபர் பாதுகாப்புத் தரவுத்தளத்தால் தடுக்கப்பட்ட ஆபத்தான மோசடி இணைப்பு ($host) — தொடாதீர்கள்!",
                    )
                }
                ThreatDnsVerdict.DOMAIN_DOES_NOT_EXIST -> {
                    return LinkScanResult(
                        originalUrl = rawUrl,
                        normalizedUrl = normalizedUrl,
                        finalUrl = effectiveFinalUrl,
                        displayDomain = finalHost,
                        verdict = LinkVerdict.UNSAFE,
                        reasonEn = "Suspicious dead or unregistered domain ($host) — do not touch.",
                        reasonTa = "பதிவு செய்யப்படாத சந்தேகத்திற்குரிய இணையதளம் ($host) — தொடாதீர்கள்.",
                    )
                }
                ThreatDnsVerdict.NETWORK_UNAVAILABLE -> {
                    return LinkScanResult(
                        originalUrl = rawUrl,
                        normalizedUrl = normalizedUrl,
                        finalUrl = effectiveFinalUrl,
                        displayDomain = finalHost,
                        verdict = LinkVerdict.PENDING_OFFLINE,
                        reasonEn = "Internet dropped during check — queued to auto-verify once connection stabilizes.",
                        reasonTa = "சோதனையின் போது இணையம் துண்டிக்கப்பட்டது — இணையம் சீரானதும் தானாகச் சரிபார்க்கப்படும்.",
                    )
                }
                ThreatDnsVerdict.CLEAN -> Unit
            }
        }

        // Optional user-configured Google Safe Browsing API v4 secondary check
        val apiKey = safeBrowsingApiKey?.trim()?.takeIf { it.isNotEmpty() }
        if (apiKey != null) {
            val browsing = transport.querySafeBrowsingOptional(effectiveFinalUrl, apiKey)
            if (browsing == null) {
                return LinkScanResult(rawUrl, normalizedUrl, effectiveFinalUrl, finalHost,
                    LinkVerdict.PENDING_OFFLINE,
                    "Configured Safe Browsing check unavailable — no safety verdict. Do not open yet.",
                    "தேர்ந்தெடுத்த பாதுகாப்புச் சோதனை கிடைக்கவில்லை — பாதுகாப்பை உறுதிப்படுத்த முடியாது. இப்போது திறக்காதீர்கள்.")
            }
            if (browsing) {
                return LinkScanResult(
                    originalUrl = rawUrl,
                    normalizedUrl = normalizedUrl,
                    finalUrl = effectiveFinalUrl,
                    displayDomain = finalHost,
                    verdict = LinkVerdict.UNSAFE,
                    reasonEn = "Flagged by Google Safe Browsing as deceptive/dangerous ($finalHost) — DO NOT TOUCH!",
                    reasonTa = "கூகுள் பாதுகாப்புச் சோதனையில் ஆபத்தான மோசடி தளமாகக் கண்டறியப்பட்டது ($finalHost) — தொடாதீர்கள்!",
                )
            }
        }

        // Failure is not a clean scan: DNS resolution alone cannot clear a failed web probe.
        if (!redirectProbe.reachable || redirectProbe.statusCode !in 200..299) {
            return LinkScanResult(rawUrl, normalizedUrl, effectiveFinalUrl, finalHost,
                LinkVerdict.PENDING_OFFLINE,
                "Web or redirect check incomplete — no safety verdict. Do not open yet.",
                "இணையதள அல்லது வழிமாற்றுச் சோதனை முடியவில்லை — பாதுகாப்பை உறுதிப்படுத்த முடியாது. இப்போது திறக்காதீர்கள்.")
        }

        // Plain unencrypted HTTP final page without HTTPS upgrade is unsafe for entering any credentials
        if (effectiveFinalUrl.startsWith("http://", ignoreCase = true)) {
            return LinkScanResult(
                originalUrl = rawUrl,
                normalizedUrl = normalizedUrl,
                finalUrl = effectiveFinalUrl,
                displayDomain = finalHost,
                verdict = LinkVerdict.UNSAFE,
                reasonEn = "Unencrypted HTTP page ($finalHost) — no security lock, do not touch!",
                reasonTa = "பாதுகாப்பு பூட்டு (HTTPS) இல்லாத பாதுகாப்பற்ற தளம் ($finalHost) — தொடாதீர்கள்!",
            )
        }

        val domainSummary = if (!initialHost.equals(finalHost, ignoreCase = true)) {
            "$initialHost → $finalHost"
        } else {
            finalHost
        }
        return LinkScanResult(
            originalUrl = rawUrl,
            normalizedUrl = normalizedUrl,
            finalUrl = effectiveFinalUrl,
            displayDomain = finalHost,
            verdict = LinkVerdict.SAFE,
            reasonEn = "No threat detected by completed checks ($domainSummary). This is not a safety guarantee.",
            reasonTa = "முடிந்த சோதனைகளில் அச்சுறுத்தல் கண்டறியப்படவில்லை ($domainSummary). இது பாதுகாப்பு உத்தரவாதம் அல்ல.",
        )
    }

    private fun inspectUrlLocally(
        candidateUrl: String,
        originalExplicitHttp: Boolean,
        sender: SenderIdentity?,
        messageBody: String = "",
    ): LinkScanResult? {
        val uri = runCatching { URI(candidateUrl) }.getOrNull()
        val rawAuthority = uri?.rawAuthority.orEmpty()
        val host = (uri?.host ?: extractHost(candidateUrl)).orEmpty().lowercase(Locale.US).trimEnd('.')

        if (host.isBlank()) {
            return LinkScanResult(
                originalUrl = candidateUrl,
                normalizedUrl = candidateUrl,
                finalUrl = candidateUrl,
                displayDomain = candidateUrl.take(32),
                verdict = LinkVerdict.UNSAFE,
                reasonEn = "Malformed or disguised link format — unsafe, do not touch!",
                reasonTa = "சிதைக்கப்பட்ட அல்லது மறைக்கப்பட்ட ஆபத்தான இணைப்பு வடிவம் — தொடாதீர்கள்!",
            )
        }

        // 1. Userinfo "@" trick (e.g. https://sbi.co.in@evil.xyz/login)
        if ("@" in rawAuthority) {
            return LinkScanResult(
                originalUrl = candidateUrl,
                normalizedUrl = candidateUrl,
                finalUrl = candidateUrl,
                displayDomain = host,
                verdict = LinkVerdict.UNSAFE,
                reasonEn = "Deceptive '@' disguised address redirecting to $host — PHISHING TRAP, DO NOT TOUCH!",
                reasonTa = "போலி முகவரி மறைப்பு தந்திரம் ($host) — நிச்சயமான மோசடி இணைப்பு, தொடாதீர்கள்!",
            )
        }

        // 2. Raw IP address link (banks and legitimate companies never send http://103.x.x.x links!)
        if (host.matches(Regex("^([0-9]{1,3}\\.){3}[0-9]{1,3}$")) || host.startsWith("[")) {
            return LinkScanResult(
                originalUrl = candidateUrl,
                normalizedUrl = candidateUrl,
                finalUrl = candidateUrl,
                displayDomain = host,
                verdict = LinkVerdict.UNSAFE,
                reasonEn = "Raw IP server link ($host) — classic malware/phishing server, DO NOT TOUCH!",
                reasonTa = "நேரடி IP எண் இணைப்பு ($host) — ஆபத்தான மோசடி சர்வர், தயவுசெய்து தொடாதீர்கள்!",
            )
        }

        // 3. Direct dangerous executable / Android package file (.apk, .xapk, .exe)
        val pathLower = (uri?.rawPath.orEmpty() + "?" + uri?.rawQuery.orEmpty()).lowercase(Locale.US)
        if (DANGEROUS_EXTENSIONS.any { ext ->
                pathLower.endsWith(ext) || "$ext?" in pathLower || "$ext&" in pathLower || "/$ext" in pathLower
            }
        ) {
            return LinkScanResult(
                originalUrl = candidateUrl,
                normalizedUrl = candidateUrl,
                finalUrl = candidateUrl,
                displayDomain = host,
                verdict = LinkVerdict.UNSAFE,
                reasonEn = "Malicious app installer (.APK / executable) from $host — NEVER INSTALL, DO NOT TOUCH!",
                reasonTa = "ஆபத்தான வைரஸ் செயலி (.APK) பதிவிறக்க இணைப்பு ($host) — ஒருபோதும் தொடாதீர்கள்!",
            )
        }

        // 3b. Messenger / Chat / Form Redirect Abuse (api.whatsapp.com/send?phone=..., wa.me, chat.whatsapp.com, Telegram t.me, Google Forms):
        // Catches DLT template-stuffing job/salary/part-time/loan/KYC scams redirecting victims to unknown WhatsApp/Telegram numbers!
        val isWhatsAppRedirect = host == "wa.me" ||
            host == "api.whatsapp.com" ||
            host == "chat.whatsapp.com" ||
            (host.endsWith("whatsapp.com") && ("/send" in pathLower || "phone=" in pathLower))
        val isTelegramRedirect = host == "t.me" || host == "telegram.me" || host == "telegram.dog"
        val isFormCollectionLink = host == "forms.gle" || (host == "docs.google.com" && "/forms/" in pathLower)

        if (isWhatsAppRedirect || isTelegramRedirect || isFormCollectionLink) {
            val isSavedTrustedContact = sender?.trustLevel == SenderTrustLevel.PERSONAL_CONTACT
            val extractedChatPhone = Regex("(?:phone=|wa\\.me/)\\+?([0-9]{8,15})", RegexOption.IGNORE_CASE)
                .find(candidateUrl)?.groupValues?.get(1)
            val phoneTag = extractedChatPhone?.let { "+$it" } ?: host
            val bodyLower = messageBody.lowercase(Locale.US)
            val looksJobOrLoanLure = Regex(
                "\\b(salary|staff|hiring|vacancy|part[-\\s]?time|work\\s+from\\s+home|wfh|daily\\s+earn|earn\\s+rs|" +
                    "task|bonus|lottery|loan|investment|trading|vip|kyc|block|suspend|pan\\s+update)\\b",
                RegexOption.IGNORE_CASE,
            ).containsMatchIn(bodyLower)

            if (!isSavedTrustedContact || looksJobOrLoanLure || isFormCollectionLink) {
                val appLabel = when {
                    isWhatsAppRedirect -> "WhatsApp ($phoneTag)"
                    isTelegramRedirect -> "Telegram ($host)"
                    else -> "External Form ($host)"
                }
                return LinkScanResult(
                    originalUrl = candidateUrl,
                    normalizedUrl = candidateUrl,
                    finalUrl = candidateUrl,
                    displayDomain = appLabel,
                    verdict = LinkVerdict.UNSAFE,
                    reasonEn = if (looksJobOrLoanLure) {
                        "Scam Alert: Fake job/salary/offer lure redirecting to unknown $appLabel — DO NOT TOUCH!"
                    } else {
                        "Caution: Unverified external chat/form redirect to unknown $appLabel — potential scam, do not touch!"
                    },
                    reasonTa = if (looksJobOrLoanLure) {
                        "சைபர் மோசடி எச்சரிக்கை: வேலை/சம்பள ஆசை காட்டி அறிமுகமில்லாத $appLabel எண்ணுக்கு அழைக்கும் மோசடி இணைப்பு — தொடாதீர்கள்!"
                    } else {
                        "எச்சரிக்கை: அறிமுகமில்லாத $appLabel எண்ணிற்கு அழைத்துச் செல்லும் இணைப்பு — மோசடியாக இருக்கலாம், தொடாதீர்கள்!"
                    },
                )
            }
        }

        // 4. Punycode IDN lookalike homograph attack (xn--...)
        val asciiHost = runCatching { IDN.toASCII(host) }.getOrDefault(host).lowercase(Locale.US)
        if ("xn--" in asciiHost) {
            return LinkScanResult(
                originalUrl = candidateUrl,
                normalizedUrl = candidateUrl,
                finalUrl = candidateUrl,
                displayDomain = host,
                verdict = LinkVerdict.UNSAFE,
                reasonEn = "Lookalike Punycode character spoofing detected ($asciiHost) — DO NOT TOUCH!",
                reasonTa = "எழுத்து மாற்று போலித் தள தந்திரம் ($asciiHost) — ஆபத்தான இணைப்பு, தொடாதீர்கள்!",
            )
        }

        // 5. Protected Brand Impersonation Detector (catches 0-minute freshly registered bank/KYC phishing domains!)
        for ((brandToken, allowedRoots) in PROTECTED_BRAND_ROOTS) {
            if (brandToken in asciiHost) {
                val matchesOfficial = allowedRoots.any { root ->
                    asciiHost == root || asciiHost.endsWith(".$root")
                }
                if (!matchesOfficial) {
                    return LinkScanResult(
                        originalUrl = candidateUrl,
                        normalizedUrl = candidateUrl,
                        finalUrl = candidateUrl,
                        displayDomain = host,
                        verdict = LinkVerdict.UNSAFE,
                        reasonEn = "FAKE '${brandToken.uppercase(Locale.US)}' lookalike phishing site ($host) — NOT official! DO NOT TOUCH!",
                        reasonTa = "போலியான '${brandToken.uppercase(Locale.US)}' மோசடி இணையதளம் ($host) — உண்மையான வங்கி/அரசு தளம் அல்ல! தொடாதீர்கள்!",
                    )
                }
            }
        }

        // 6. High-abuse scam TLD (.top, .icu, .xyz, .buzz, .vip, .loan, .apk, etc.) especially from personal SIM or HTTP
        val tld = asciiHost.substringAfterLast('.', "")
        if (tld in HIGH_ABUSE_TLDS && (sender == null || sender.isPersonalNumber || sender.dltCategory == DltCategory.PROMOTIONAL || originalExplicitHttp)) {
            return LinkScanResult(
                originalUrl = candidateUrl,
                normalizedUrl = candidateUrl,
                finalUrl = candidateUrl,
                displayDomain = host,
                verdict = LinkVerdict.UNSAFE,
                reasonEn = "High-risk scam domain extension (.$tld on $host) — unsafe, do not touch!",
                reasonTa = "அதிக ஆபத்துள்ள மோசடி இணையதள முகவரி ($host) — தயவுசெய்து தொடாதீர்கள்!",
            )
        }

        return null
    }

    fun extractHost(url: String): String? {
        return runCatching {
            val withScheme = if ("://" in url) url else "https://$url"
            URI(withScheme).host?.trimEnd('.')
        }.getOrNull()
    }

    /**
     * Production HEAD-only unshortener and encrypted DNS threat-intelligence checks.
     */
    object LiveHttpDohTransport : NetworkTransport {

        override fun resolveRedirectsHeaderOnly(startUrl: String, maxHops: Int): RedirectProbeResult {
            var current = startUrl
            var lastStatus = 0
            var contentType: String? = null
            var contentDisp: String? = null
            for (hop in 0..maxHops) {
                var conn: HttpURLConnection? = null
                try {
                    val u = URL(current)
                    // Recheck every hop before visiting it, not only the final response.
                    if (u.protocol !in listOf("http", "https") ||
                        inspectUrlLocally(current, u.protocol == "http", null) != null) {
                        return RedirectProbeResult(false, current, 0, null, null)
                    }
                    conn = (u.openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = false
                        requestMethod = "HEAD"
                        connectTimeout = 4500
                        readTimeout = 4500
                        setRequestProperty("User-Agent", "SMSGuard-LinkShield/1.0")
                        setRequestProperty("Accept", "*/*")
                    }
                    lastStatus = conn.responseCode
                    contentType = conn.contentType
                    contentDisp = conn.getHeaderField("Content-Disposition")
                    if (lastStatus in listOf(301, 302, 303, 307, 308)) {
                        val location = conn.getHeaderField("Location")?.trim()
                        if (!location.isNullOrEmpty()) {
                            // Handle Android App Link intent:// redirects (e.g. maps.app.goo.gl -> intent://...S.browser_fallback_url=https%3A...)
                            if (location.startsWith("intent://", ignoreCase = true)) {
                                val fallbackEncoded = Regex("S\\.browser_fallback_url=([^;]+)")
                                    .find(location)?.groupValues?.get(1)
                                if (!fallbackEncoded.isNullOrEmpty()) {
                                    current = java.net.URLDecoder.decode(fallbackEncoded, "UTF-8")
                                    continue
                                }
                                val hostPart = location.removePrefix("intent://").substringBefore("#Intent")
                                current = "https://$hostPart"
                                return RedirectProbeResult(
                                    reachable = false,
                                    finalUrl = current,
                                    statusCode = lastStatus,
                                    contentType = contentType,
                                    contentDisposition = contentDisp,
                                )
                            }
                            current = URL(u, location).toExternalForm()
                            continue
                        }
                    }
                    return RedirectProbeResult(
                        reachable = true,
                        finalUrl = current,
                        statusCode = lastStatus,
                        contentType = contentType,
                        contentDisposition = contentDisp,
                    )
                } catch (ssl: javax.net.ssl.SSLException) {
                    return RedirectProbeResult(
                        reachable = false,
                        finalUrl = current,
                        statusCode = 0,
                        contentType = null,
                        contentDisposition = null,
                        tlsFailed = true,
                        errorMessage = ssl.message,
                    )
                } catch (e: Exception) {
                    return RedirectProbeResult(
                        reachable = false,
                        finalUrl = current,
                        statusCode = lastStatus,
                        contentType = contentType,
                        contentDisposition = contentDisp,
                        tlsFailed = false,
                        errorMessage = e.message,
                    )
                } finally {
                    // HEAD probes never open/read a response body or fall back to GET.
                    conn?.disconnect()
                }
            }
            return RedirectProbeResult(
                reachable = false, // Redirect limit exhausted: destination is not verified.
                finalUrl = current,
                statusCode = lastStatus,
                contentType = contentType,
                contentDisposition = contentDisp,
            )
        }

        @Volatile
        private var directDohUnreachableUntilMs: Long = 0L

        private fun isSinkholeIp(addr: java.net.InetAddress): Boolean {
            val s = addr.hostAddress.orEmpty()
            return addr.isAnyLocalAddress ||
                addr.isLoopbackAddress ||
                s == "0.0.0.0" ||
                s == "::" ||
                s == "0:0:0:0:0:0:0:0" ||
                s == "146.112.61.104" ||
                s == "146.112.61.106"
        }

        override fun queryDohThreatIntel(host: String): ThreatDnsVerdict {
            // 1. Primary System / Router DNS Check (works natively in <25ms with NextDNS, AdGuard, Private DNS, BSNL/Jio/Airtel
            //    WITHOUT triggering NextDNS "Blocked by Bypass Methods" alerts on dns.google / cloudflare-dns.com / dns.quad9.net!):
            val osAddresses = try {
                java.net.InetAddress.getAllByName(host)
            } catch (_: java.net.UnknownHostException) {
                null
            } catch (_: Exception) {
                null
            }

            if (osAddresses == null || osAddresses.isEmpty()) {
                // Distinguish genuine dead/NextDNS-blocked domain vs phone completely offline:
                val internetAlive = try {
                    !java.net.InetAddress.getAllByName("www.google.com").isNullOrEmpty()
                } catch (_: Exception) {
                    false
                }
                return if (internetAlive) {
                    ThreatDnsVerdict.DOMAIN_DOES_NOT_EXIST
                } else {
                    ThreatDnsVerdict.NETWORK_UNAVAILABLE
                }
            }

            // Catch sinkhole responses (0.0.0.0 / :: / 127.0.0.1) returned when NextDNS / AdGuard / Pi-hole blocks a malicious domain!
            if (osAddresses.any { isSinkholeIp(it) }) {
                return ThreatDnsVerdict.BLOCKED_MALWARE_OR_PHISHING
            }

            // Respect the network's DNS/filtering policy; never evade it with raw-IP HTTPS.
            // Failed/malformed threat intelligence cannot be treated as a clean result.
            val now = System.currentTimeMillis()
            if (now >= directDohUnreachableUntilMs) {
                val encoded = URLEncoder.encode(host, "UTF-8")
                val cfSec = fetchDohJson("https://security.cloudflare-dns.com/dns-query?name=$encoded&type=A", timeoutMs = 1200)
                if (cfSec == null) {
                    directDohUnreachableUntilMs = now + 10 * 60_000L
                } else if (isCloudflareSecurityBlocked(cfSec)) {
                    return ThreatDnsVerdict.BLOCKED_MALWARE_OR_PHISHING
                } else if (cfSec.optInt("Status", -1) == 3) {
                    return ThreatDnsVerdict.DOMAIN_DOES_NOT_EXIST
                } else if (cfSec.optInt("Status", -1) == 0 &&
                    (cfSec.optJSONArray("Answer")?.length() ?: 0) > 0) {
                    return ThreatDnsVerdict.CLEAN
                }
            }

            return ThreatDnsVerdict.NETWORK_UNAVAILABLE
        }

        private fun isCloudflareSecurityBlocked(sec: JSONObject): Boolean {
            val comments: Any? = sec.opt("Comment")
            when (comments) {
                is String -> if ("Filtered" in comments || "Malware" in comments || "Phishing" in comments) return true
                is JSONArray -> {
                    for (i in 0 until comments.length()) {
                        val c = comments.optString(i)
                        if ("Filtered" in c || "Malware" in c || "Phishing" in c) return true
                    }
                }
            }
            val answers = sec.optJSONArray("Answer") ?: return false
            for (i in 0 until answers.length()) {
                val data = answers.optJSONObject(i)?.optString("data").orEmpty()
                if (data == "0.0.0.0" || data == "::") return true
            }
            return false
        }

        private fun fetchDohJson(endpoint: String, timeoutMs: Int = 2000): JSONObject? {
            var conn: HttpURLConnection? = null
            return try {
                conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = timeoutMs
                    readTimeout = timeoutMs
                    setRequestProperty("Accept", "application/dns-json")
                }
                if (conn.responseCode !in 200..299) return null
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(text)
            } catch (_: Exception) {
                null
            } finally {
                conn?.disconnect()
            }
        }

        override fun querySafeBrowsingOptional(url: String, apiKey: String): Boolean? {
            var conn: HttpURLConnection? = null
            return try {
                val endpoint = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=" +
                    URLEncoder.encode(apiKey, "UTF-8")
                val payload = JSONObject().apply {
                    put("client", JSONObject().put("clientId", "rp-ideas-smsguard").put("clientVersion", "1.0.0"))
                    put(
                        "threatInfo",
                        JSONObject()
                            .put("threatTypes", JSONArray(listOf("MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE")))
                            .put("platformTypes", JSONArray(listOf("ANY_PLATFORM", "ANDROID")))
                            .put("threatEntryTypes", JSONArray(listOf("URL")))
                            .put("threatEntries", JSONArray().put(JSONObject().put("url", url))),
                    )
                }.toString()

                conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 4500
                    readTimeout = 4500
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                if (conn.responseCode !in 200..299) return null
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                if (json.has("error")) return null
                (json.optJSONArray("matches")?.length() ?: 0) > 0
            } catch (_: Exception) {
                null
            } finally {
                conn?.disconnect()
            }
        }
    }
}
