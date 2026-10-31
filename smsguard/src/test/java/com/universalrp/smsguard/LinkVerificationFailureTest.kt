package com.universalrp.smsguard

import com.universalrp.smsguard.engine.LinkSecurityScanner
import com.universalrp.smsguard.engine.LinkVerdict
import org.junit.Assert.*
import org.junit.Test

class LinkVerificationFailureTest {
    private class Transport(
        private val reachable: Boolean = true,
        private val status: Int = 200,
        private val dns: LinkSecurityScanner.ThreatDnsVerdict = LinkSecurityScanner.ThreatDnsVerdict.CLEAN,
        private val browsing: Boolean? = false,
    ) : LinkSecurityScanner.NetworkTransport {
        var browsingCalls = 0
        var probeCalls = 0
        var dnsCalls = 0
        override fun resolveRedirectsHeaderOnly(startUrl: String, maxHops: Int): LinkSecurityScanner.RedirectProbeResult {
            probeCalls++
            return LinkSecurityScanner.RedirectProbeResult(reachable, startUrl, status, "text/html", null)
        }
        override fun queryDohThreatIntel(host: String): LinkSecurityScanner.ThreatDnsVerdict {
            dnsCalls++
            return dns
        }
        override fun querySafeBrowsingOptional(url: String, apiKey: String): Boolean? {
            browsingCalls++
            return browsing
        }
    }
    private fun scan(transport: Transport, key: String? = null) = LinkSecurityScanner.scanSingleUrl(
        "https://www.rbi.org.in/", internetAllowed = true, hasValidatedNetwork = true,
        safeBrowsingApiKey = key, transport = transport)

    @Test fun ordinaryUnreachableUrlIsNotClearedByCleanDns() {
        assertEquals(LinkVerdict.PENDING_OFFLINE, scan(Transport(reachable = false, status = 0)).verdict)
    }
    @Test fun errorAndUnsupportedHeadResponsesNeverGetGreenVerdicts() {
        for (code in listOf(0, 400, 401, 403, 404, 405, 429, 500, 503)) {
            assertEquals("HTTP $code", LinkVerdict.PENDING_OFFLINE, scan(Transport(status = code)).verdict)
        }
    }
    @Test fun incompleteRedirectChainCannotBeCalledSafe() {
        assertEquals(LinkVerdict.PENDING_OFFLINE, scan(Transport(status = 302)).verdict)
        assertEquals(LinkVerdict.PENDING_OFFLINE, scan(Transport(reachable = false, status = 307)).verdict)
    }
    @Test fun configuredBrowsingFailureIsNotANegativeThreatMatch() {
        assertEquals(LinkVerdict.PENDING_OFFLINE, scan(Transport(browsing = null), "owner-key").verdict)
    }
    @Test fun configuredBrowsingPositiveMatchStillBlocks() {
        assertEquals(LinkVerdict.UNSAFE, scan(Transport(browsing = true), "owner-key").verdict)
    }
    @Test fun completedChecksCanClearButNeverGuaranteeSafety() {
        val result = scan(Transport(), "owner-key")
        assertEquals(LinkVerdict.SAFE, result.verdict)
        assertTrue(result.reasonEn.contains("not a safety guarantee"))
        assertFalse(result.reasonEn.contains("Verified safe"))
    }
    @Test fun unavailableThreatIntelligenceNeverClearsALink() {
        assertEquals(LinkVerdict.PENDING_OFFLINE, scan(Transport(
            dns = LinkSecurityScanner.ThreatDnsVerdict.NETWORK_UNAVAILABLE)).verdict)
    }
    @Test fun optionalBrowsingIsNotContactedWithoutAnOwnerKey() {
        val transport = Transport(browsing = null)
        assertEquals(LinkVerdict.SAFE, scan(transport).verdict)
        assertEquals(0, transport.browsingCalls)
    }
    @Test fun internetOffStillDoesNotContactTransport() {
        val transport = Transport()
        val result = LinkSecurityScanner.scanSingleUrl("https://www.rbi.org.in/",
            internetAllowed = false, hasValidatedNetwork = true, transport = transport)
        assertEquals(LinkVerdict.PENDING_INTERNET_OFF, result.verdict)
        assertEquals(0, transport.browsingCalls)
        assertEquals(0, transport.probeCalls)
        assertEquals(0, transport.dnsCalls)
    }
}
