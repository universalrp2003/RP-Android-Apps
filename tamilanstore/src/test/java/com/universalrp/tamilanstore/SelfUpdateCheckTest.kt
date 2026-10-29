package com.universalrp.tamilanstore

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SelfUpdateCheckTest {
    private fun release(
        tag: String,
        draft: Boolean = false,
        prerelease: Boolean = false,
        assetName: String = "Tamilan-Store-v${tag.removePrefix("tamilan-store-v")}.apk"
    ) = JSONObject()
        .put("tag_name", tag)
        .put("draft", draft)
        .put("prerelease", prerelease)
        .put("assets", JSONArray().put(JSONObject().put("name", assetName)))

    @Test fun findsStoreReleaseWhenItAppearsOnLaterApiPage() {
        val firstPage = JSONArray().put(release("unrelated-v9.0"))
        val secondPage = JSONArray()
            .put(release("tamilan-store-v0.5"))
            .put(release("tamilan-store-v0.10"))
            .put(release("tamilan-store-v0.99", draft = true))

        val candidates = with(SelfUpdateCheck) {
            firstPage.parseStoreCandidates() + secondPage.parseStoreCandidates()
        }
        assertEquals("0.10", SelfUpdateCheck.newest(candidates)?.first)
        assertEquals(
            "https://github.com/universalrp2003/RP-Android-Apps/releases/tag/tamilan-store-v0.10",
            SelfUpdateCheck.newest(candidates)?.second
        )
    }

    @Test fun excludesDraftPrereleaseAndMissingExpectedApk() {
        val page = JSONArray()
            .put(release("tamilan-store-v9.0", draft = true))
            .put(release("tamilan-store-v8.0", prerelease = true))
            .put(release("tamilan-store-v7.0", assetName = "Tamilan-Store-v6.9.apk"))
            .put(release("tamilan-store-v6.0"))

        val candidates = with(SelfUpdateCheck) { page.parseStoreCandidates() }
        assertEquals(listOf("6.0"), candidates.map { it.first })
    }

    @Test fun versionOrderingIsNumericAndIgnoresOverflowingTags() {
        val candidates = listOf(
            "1.9" to "older",
            "1.10" to "newer",
            "1.999999999999999999999999" to "overflow"
        )
        assertEquals("1.10", SelfUpdateCheck.newest(candidates)?.first)
        assertNull(SelfUpdateCheck.newest(emptyList()))
    }

    @Test fun releaseLookupHasBoundedPagination() {
        assertEquals(100, SelfUpdateCheck.PAGE_SIZE)
        assertTrue(SelfUpdateCheck.MAX_RELEASE_PAGES in 1..10)
    }
}
