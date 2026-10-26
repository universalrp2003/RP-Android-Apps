package com.universalrp.vasanthi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Tamil sound vocabulary is the part of this app a deaf Tamil reader actually depends on, so
 * it is pinned down rather than assumed.
 *
 * The tests here check behaviour, not the contents of the map: that every key is a real AudioSet
 * class and that every value is Tamil is what tools/verify_vasanthi_sound_labels.py enforces
 * against the shipped label file, and repeating it here would only drift.
 */
class SoundLabelMapTest {

    @Test
    fun aKnownClassComesBackInTamil() {
        // The doorbell is the single most useful thing this app can name.
        assertEquals("கதவு மணி", SoundLabelMap.tamilFor("Doorbell"))
        assertEquals("நாய் ஓட்டம்", SoundLabelMap.tamilFor("Bark"))
        assertEquals("அழுகை", SoundLabelMap.tamilFor("Crying, sobbing"))
    }

    @Test
    fun anUnknownClassHasNoTamilName() {
        assertNull(SoundLabelMap.tamilFor("Zzz not a real AudioSet class"))
    }

    @Test
    fun anUnknownClassIsNotShown() {
        // An untranslated class must be dropped, never shown as English.
        assertFalse(SoundLabelMap.isShown("Zzz not a real AudioSet class"))
    }

    @Test
    fun speechIsTranslatedBecauseItIsTheWholePoint() {
        assertNotNull(SoundLabelMap.tamilFor("Speech"))
        assertNotNull(SoundLabelMap.tamilFor("Conversation"))
    }

    @Test
    fun theDangerousSoundsAreMarkedUrgent() {
        assertTrue(SoundLabelMap.isUrgent("Doorbell"))
        assertTrue(SoundLabelMap.isUrgent("Smoke detector, smoke alarm"))
        assertTrue(SoundLabelMap.isUrgent("Baby cry, infant cry"))
        assertTrue(SoundLabelMap.isUrgent("Shatter"))
    }

    @Test
    fun ordinarySoundsAreNotMarkedUrgent() {
        // A phone that vibrates at everything gets switched off, and a switched-off phone
        // protects nobody.
        assertFalse(SoundLabelMap.isUrgent("Speech"))
        assertFalse(SoundLabelMap.isUrgent("Music"))
        assertFalse(SoundLabelMap.isUrgent("Typing"))
        assertFalse(SoundLabelMap.isUrgent("Rain"))
    }

    @Test
    fun everyUrgentSoundIsAlsoTranslated() {
        // An urgent sound with no Tamil name would vibrate the phone and then show nothing.
        val translated = SoundLabelMap.translatedLabels()
        val urgent = listOf(
            "Doorbell", "Telephone", "Ringtone", "Alarm clock",
            "Smoke detector, smoke alarm", "Screaming", "Crying, sobbing",
            "Baby cry, infant cry", "Shatter", "Bark", "Door", "Chime", "Church bell",
        )
        for (label in urgent) {
            assertTrue("urgent but untranslated: $label", label in translated)
        }
    }

    @Test
    fun theVocabularyIsWideEnoughToBeUseful() {
        // Not a target, a floor. Below this the app would name so little that it is not worth
        // running, and the owner's sister would be better served by looking at the room.
        assertTrue(
            "only ${SoundLabelMap.translatedLabels().size} classes translated",
            SoundLabelMap.translatedLabels().size >= 150,
        )
    }
}
