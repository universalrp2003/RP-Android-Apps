package com.universalrp.tamilanstore

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CatalogTest {
    private fun catalog() = File("src/main/assets/catalog.json").readText()
    @Test fun thirteenOfficialApps() {
        val apps = Catalog.parse(catalog())
        assertEquals(13, apps.size)
        assertTrue(apps.all { it.url.startsWith("https://github.com/universalrp2003/RP-Android-Apps/releases/download/") })
    }
    @Test fun foreignDownloadRejected() {
        try { Catalog.parse(catalog().replace("https://github.com/", "https://evil.example/")); fail("foreign URL accepted") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun httpAndMalformedHashRejected() {
        for (bad in listOf(catalog().replace("https://github.com/", "http://github.com/"), catalog().replaceFirst("\"sha256\": \"", "\"sha256\": \"X"))) {
            try { Catalog.parse(bad); fail("invalid catalog accepted") } catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun releaseTagVersionMustMatchDeclaredVersion() {
        val mismatched = catalog().replaceFirst("\"version\": \"1.1.9\"", "\"version\": \"1.1.10\"")
        try { Catalog.parse(mismatched); fail("catalog version did not match release tag") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun redirectHostsAreBounded() {
        assertFalse(Catalog.allowedDownloadHost("evil.github.com"))
        assertFalse(Catalog.allowedDownloadHost("github.com.evil.example"))
        assertTrue(Catalog.allowedDownloadHost("release-assets.githubusercontent.com"))
    }
}
