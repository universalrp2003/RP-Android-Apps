package com.universalrp.tamilanstore

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CatalogTest {
    private fun catalog() = File("src/main/assets/catalog.json").readText()
    @Test fun replacementCertificateMatchesEveryRotatedPackage() {
        val expected = "7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d"
        for (app in Catalog.parse(catalog()).filter { it.packageName != "com.rpideas.secretmessage" }) {
            assertEquals(app.packageName, expected, Catalog.signerFor(app.packageName))
            assertNotEquals("retired key must not authenticate new archives",
                "9e30f4eab8afce5e4ab444ff9578fec354d6d1414d245253590a9703d5985d55", Catalog.signerFor(app.packageName))
        }
        assertEquals("58e370e417cf6f33234689acd5bf379474f5a8c55f79181d9e892a321bc04f08", Catalog.signerFor("com.rpideas.secretmessage"))
    }
    @Test fun typedFailureStagesNeverCollapseIntoNetworkRetry() {
        for (reason in TransferFailure.entries) {
            verifyGate(true, reason)
            try { verifyGate(false, reason); fail("verification must fail closed") }
            catch (e: TransferVerificationException) { assertEquals(reason, e.reason) }
            assertTrue(reason.message(true).isNotBlank())
            assertTrue(reason.message(false).isNotBlank())
        }
        assertTrue(TransferFailure.INSTALLED_SIGNER.message(false).contains("Do not uninstall"))
    }
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
