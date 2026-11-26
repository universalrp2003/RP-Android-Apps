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
    @Test fun eighteenOfficialEntries() {
        val apps = Catalog.parse(catalog())
        assertEquals(18, apps.size)
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
        val root = org.json.JSONObject(catalog())
        root.getJSONArray("apps").getJSONObject(0).put("version", "99.99")
        val mismatched = root.toString()
        try { Catalog.parse(mismatched); fail("catalog version did not match release tag") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun redirectHostsAreBounded() {
        assertFalse(Catalog.allowedDownloadHost("evil.github.com"))
        assertFalse(Catalog.allowedDownloadHost("github.com.evil.example"))
        assertTrue(Catalog.allowedDownloadHost("release-assets.githubusercontent.com"))
    }

    @Test fun bundledUpdatedIsDerivedFromTheCatalogAndRenderedInBothLanguages() {
        val text = catalog()
        // The bundled list's own day, not a typed one: these must move together when catalog.json is rebuilt.
        val iso = org.json.JSONObject(text).getString("updated")
        assertEquals(iso, Catalog.updated(text))
        val day = Catalog.catalogDay(iso)
        assertEquals(iso.substring(8, 10).toInt().toString() + " " + ENGLISH_MONTHS[iso.substring(5, 7).toInt() - 1] + " " + iso.substring(0, 4), day.en)
        assertTrue(day.ta.startsWith(iso.substring(8, 10) + " ") && day.ta.endsWith(iso.substring(0, 4)) && TAMIL_MONTHS.any { it in day.ta })
        try { Catalog.updated("""{"schema":1,"updated":"13 November 2026","apps":[]}"""); fail("a non-ISO day must be refused") }
        catch (e: Exception) { assertTrue(e is IllegalArgumentException || e is org.json.JSONException) }
        try { Catalog.catalogDay("2026-13-01"); fail("month 13 must be refused") } catch (e: IllegalArgumentException) { }
        try { Catalog.catalogDay("2026-02-32"); fail("day 32 must be refused") } catch (e: IllegalArgumentException) { }
    }
    @Test fun aShorterSavedSnapshotRestoresTheSameAppWithoutLooseningEntryValidation() {
        // The live feed may carry fewer entries than the bundled contract for a while; parseLive accepts that,
        // but every entry is still validated exactly as strictly as the bundled one.
        val text = catalog()
        val full = Catalog.parse(text)
        assertEquals(Catalog.EXPECTED_APPS, full.size)
        val root = org.json.JSONObject(text)
        val apps = root.getJSONArray("apps")
        for (keep in listOf(1, 8, full.size - 1)) {
            val shorter = org.json.JSONObject(text).put("apps", org.json.JSONArray().also { out -> for (i in 0 until keep) out.put(apps.getJSONObject(i)) })
            val parsed = Catalog.parseLive(shorter.toString())
            assertEquals(keep, parsed.size)
            assertEquals(full.take(keep).map { it.id }, parsed.map { it.id })
            try { Catalog.parse(shorter.toString()); fail("strict parse must still demand every bundled entry") } catch (e: IllegalArgumentException) { }
        }
        val broken = org.json.JSONObject(text).put("apps", org.json.JSONArray().also { out -> for (i in 0 until 5) out.put(apps.getJSONObject(i)) })
        broken.getJSONArray("apps").getJSONObject(2).put("url", "https://example.com/evil.apk")
        try { Catalog.parseLive(broken.toString()); fail("a foreign download host must still be refused") } catch (e: IllegalArgumentException) { }
        val tooMany = org.json.JSONObject(text).put("apps", org.json.JSONArray().also { out -> for (i in 0..full.size) out.put(apps.getJSONObject(i % apps.length())) })
        try { Catalog.parseLive(tooMany.toString()); fail("more entries than the contract must still be refused") } catch (e: IllegalArgumentException) { }
    }
    private companion object {
        val ENGLISH_MONTHS = listOf("January","February","March","April","May","June","July","August","September","October","November","December")
        val TAMIL_MONTHS = listOf("ஜனவரி","பிப்ரவரி","மார்ச்","ஏப்ரல்","மே","ஜூன்","ஜூலை","ஆகஸ்ட்","செப்டம்பர்","அக்டோபர்","நவம்பர்","டிசம்பர்")
    }
}
