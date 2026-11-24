package com.universalrp.vasanthi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A picture for every sound the app can name, and no picture for a sound it cannot: the two maps
 * are kept in step here rather than by remembering to edit two files together.
 */
class SoundCueTest {

    @Test
    fun everyTranslatedSoundHasAPictureAndNothingElseDoes() {
        assertEquals(SoundLabelMap.translatedLabels(), SoundCue.labels())
        for (label in SoundCue.labels()) {
            assertTrue("empty picture for $label", SoundCue.emojiFor(label).isNotEmpty())
        }
    }

    @Test
    fun thePictureNamesTheSound() {
        assertEquals("🔔", SoundCue.emojiFor("Doorbell"))
        assertEquals("🐕", SoundCue.emojiFor("Bark"))
        assertEquals("🐕", SoundCue.emojiFor("Dog"))
        assertEquals("", SoundCue.emojiFor("Zzz not a real AudioSet class"))
    }

    @Test
    fun aSavedTamilLineFindsItsPictureThoughTheHistoryKeepsOnlyTheTamil() {
        assertEquals("🔔", SoundCue.emojiForTamil("கதவு மணி"))
        assertEquals("🐕", SoundCue.emojiForTamil("நாய் ஓட்டம்"))
        assertEquals("", SoundCue.emojiForTamil("not a sound name"))
    }

    @Test
    fun twoClassesSharingATamilNameShareAPicture() {
        // The history looks pictures up by Tamil name, so one name can never mean two pictures.
        for (label in SoundLabelMap.translatedLabels()) {
            val tamil = SoundLabelMap.tamilFor(label)!!
            assertEquals("mixed pictures for $tamil", SoundCue.emojiFor(label), SoundCue.emojiForTamil(tamil))
        }
    }

    @Test
    fun theCueGoesInFrontOfTheName() {
        assertEquals("🔔 கதவு மணி", SoundCue.withCue("Doorbell", "கதவு மணி"))
        assertEquals("பேச்சு", SoundCue.withCue("Zzz not a real AudioSet class", "பேச்சு"))
    }
}
