package com.oorarivippu.voiceannouncer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A5 - the delivery report rules: what an announcement attempt is recorded as, how the stored form
 * survives a malformed line, and what the summary card is allowed to claim.
 */
class AnnouncementDeliveryReportTest {

    private fun item(
        title: String,
        text: String = "spoken",
        outcome: String = AnnouncementDeliveryReport.Outcome.SPOKEN.name,
    ) = AnnouncementLogItem("10:15:00 AM", title, text, outcome)

    @Test
    fun everyRowKeepsItsOwnOutcomeInsteadOfCollapsingToSpoken() {
        val history = listOf(
            item("Alangudi"),
            item("Pudukkottai", outcome = AnnouncementDeliveryReport.Outcome.NO_INTERNET.name),
            item("Trichy", outcome = AnnouncementDeliveryReport.Outcome.VOICE_UNAVAILABLE.name),
            item("Thanjavur", outcome = AnnouncementDeliveryReport.Outcome.QUEUED_CALL.name),
            item("Karur", outcome = AnnouncementDeliveryReport.Outcome.NO_GPS.name),
            item("Namakkal", outcome = AnnouncementDeliveryReport.Outcome.SKIPPED.name),
        )
        val summary = AnnouncementDeliveryReport.summarize(history)
        assertEquals(6, summary.total)
        assertEquals(1, summary.spoken)
        assertEquals(5, summary.notSpoken)
        assertEquals(1, summary.queuedDuringCall)
        assertEquals(1, summary.noInternet)
        assertEquals(1, summary.noGps)
        assertEquals(1, summary.voiceUnavailable)
        assertEquals(1, summary.skipped)
        assertEquals(6, summary.distinctPlaces)
        assertEquals(17, summary.deliveredPercent)
        assertFalse(summary.isEmpty)
    }

    @Test
    fun onlySpokenCountsAsDeliveredEverythingElseIsANonDelivery() {
        val spoken = item("A", outcome = AnnouncementDeliveryReport.Outcome.SPOKEN.name)
        assertTrue(AnnouncementDeliveryReport.summarize(listOf(spoken)).let { it.spoken == 1 && it.notSpoken == 0 })
        for (outcome in AnnouncementDeliveryReport.Outcome.values()) {
            if (outcome == AnnouncementDeliveryReport.Outcome.SPOKEN) continue
            val summary = AnnouncementDeliveryReport.summarize(
                listOf(item("A", outcome = outcome.name))
            )
            assertEquals("outcome $outcome must not read as delivered", 0, summary.spoken)
            assertEquals(1, summary.notSpoken)
        }
    }

    @Test
    fun anEmptyHistorySummarisesToZeroAndNeverClaimsAPercent() {
        val summary = AnnouncementDeliveryReport.summarize(emptyList())
        assertTrue(summary.isEmpty)
        assertEquals(0, summary.total)
        assertEquals(0, summary.distinctPlaces)
        assertEquals(0, summary.deliveredPercent)
        assertEquals("", AnnouncementDeliveryReport.toPlainText(emptyList(), summary))
    }

    @Test
    fun theSamePlaceAnnouncedTwiceIsStillOnePlace() {
        val history = listOf(
            item("Alangudi (Kallakudi)"),
            item("Alangudi (Kallakudi)"),
            item("  Alangudi (Kallakudi)  "),
        )
        // Trimmed, so the third row (leading/trailing whitespace from a stored line) is the same place.
        assertEquals(1, AnnouncementDeliveryReport.summarize(history).distinctPlaces)
    }

    @Test
    fun aRowWithNoOutcomeIsReadAsSpokenBecauseTheOldAppOnlyLoggedSpokenRows() {
        val legacy = AnnouncementLogItem("9:00:00 AM", "Alangudi", "spoken text")
        assertEquals(
            AnnouncementDeliveryReport.Outcome.SPOKEN,
            AnnouncementDeliveryReport.outcomeOf(legacy)
        )
        assertEquals(1, AnnouncementDeliveryReport.summarize(listOf(legacy)).spoken)
    }

    @Test
    fun anUnknownOutcomeStringFallsBackToSpokenRatherThanCrashing() {
        val odd = item("Alangudi", outcome = "SOMETHING_FROM_THE_FUTURE")
        assertEquals(
            AnnouncementDeliveryReport.Outcome.SPOKEN,
            AnnouncementDeliveryReport.outcomeOf(odd)
        )
    }

    @Test
    fun encodeThenDecodeRoundTripsEveryField() {
        val history = listOf(
            item("Alangudi (Kallakudi)", "அலங்குடி கல்லக்குடி நுழைந்திருக்கிறீர்கள்",
                AnnouncementDeliveryReport.Outcome.NO_GPS.name),
            item("Trichy", "Tiruchirappalli", AnnouncementDeliveryReport.Outcome.SPOKEN.name),
        )
        assertEquals(history, AnnouncementDeliveryReport.decodeAll(AnnouncementDeliveryReport.encodeAll(history)))
    }

    @Test
    fun spokenTextContainingTheFieldSeparatorSurvives() {
        // A place name built from a control character is the only way this can happen, and it must
        // not silently eat the rest of the row.
        val entry = item(
            "Alangudi",
            "before\u0001after",
            AnnouncementDeliveryReport.Outcome.SPOKEN.name
        )
        val decoded = AnnouncementDeliveryReport.decode(AnnouncementDeliveryReport.encode(entry))
        assertEquals(entry, decoded)
    }

    @Test
    fun decodeDropsBlankAndShortLinesButKeepsTheGoodRows() {
        val stored = listOf(
            "",
            "no separators at all",
            "10:15:00 AM\u0001Alangudi",
            "10:16:00 AM\u0001Trichy\u0001text\u0001" + AnnouncementDeliveryReport.Outcome.SPOKEN.name,
        ).joinToString("\n")
        val decoded = AnnouncementDeliveryReport.decodeAll(stored)
        assertEquals(1, decoded.size)
        assertEquals("Trichy", decoded[0].title)
    }

    @Test
    fun aLineWithNoTimestampIsDropped() {
        assertNull(
            AnnouncementDeliveryReport.decode(
                "\u0001Trichy\u0001text\u0001" + AnnouncementDeliveryReport.Outcome.SPOKEN.name
            )
        )
    }

    @Test
    fun theStoredHistoryIsCappedSoPrefsCannotGrowForever() {
        val many = (1..80).map { item("Place $it", outcome = AnnouncementDeliveryReport.Outcome.SPOKEN.name) }
        val decoded = AnnouncementDeliveryReport.decodeAll(AnnouncementDeliveryReport.encodeAll(many))
        assertEquals(AnnouncementDeliveryReport.MAX_HISTORY, decoded.size)
        assertEquals("Place 1", decoded.first().title)
    }

    @Test
    fun thePlainTextExportCarriesTheTimeThePlaceAndTheOutcome() {
        val history = listOf(
            item("Alangudi", outcome = AnnouncementDeliveryReport.Outcome.SPOKEN.name),
            item("Pudukkottai", outcome = AnnouncementDeliveryReport.Outcome.NO_INTERNET.name),
        )
        val text = AnnouncementDeliveryReport.toPlainText(
            history,
            AnnouncementDeliveryReport.summarize(history)
        )
        assertTrue(text.contains("10:15:00 AM - Alangudi - spoken"))
        assertTrue(text.contains("10:15:00 AM - Pudukkottai - no internet"))
        assertEquals(2, text.lines().size)
    }

    @Test
    fun everyOutcomeHasALabelSoTheExportNeverShowsARawEnumName() {
        for (outcome in AnnouncementDeliveryReport.Outcome.values()) {
            val label = AnnouncementDeliveryReport.labelFor(outcome)
            assertTrue("label for $outcome is blank", label.isNotBlank())
            assertFalse("label for $outcome is the raw name", label == outcome.name)
        }
    }
}
