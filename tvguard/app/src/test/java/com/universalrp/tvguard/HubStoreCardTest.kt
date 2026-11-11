package com.universalrp.tvguard

import com.universalrp.tvguard.web.LocalWebTransferServer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The hub links to the hosted app store. These checks exist because the card is a plain string
 * built in Kotlin: a broken quote or a stale address would still compile, and the phone would only
 * find out when the page rendered wrong.
 */
class HubStoreCardTest {
    private val card = LocalWebTransferServer.storeCardHtml()

    @Test
    fun linksToTheOfficialHttpsStoreInANewTab() {
        assertTrue(card.contains("""href="https://universalrp2003.github.io/RP-Android-Apps/""""))
        assertTrue(card.contains("""target="_blank""""))
        // Without noopener the opened page can reach back into the hub tab.
        assertTrue(card.contains("""rel="noopener""""))
        assertFalse(card, card.contains("http://"))
    }

    @Test
    fun doesNotPromiseAnInstallerItCannotBe() {
        // Android requires a per-APK user confirmation, so the card must not claim one-tap installs
        // from the web page; it points at the file list, where the TV's own installer runs.
        val text = card.lowercase()
        assertTrue(text.contains("install prompt"))
        listOf("one-tap", "one tap", "no confirmation", "silently").forEach {
            assertFalse(it, text.contains(it))
        }
    }

    @Test
    fun isASingleClosedCardNextToTheOthers() {
        assertTrue(card.startsWith("""<div class="card">"""))
        assertTrue(card.endsWith("</div>"))
        assertTrue(card.contains("RP App Store"))
        // The heading uses an HTML card, not a page-level document.
        assertFalse(card.contains("<html"))
        assertFalse(card.contains("<body"))
    }
}
