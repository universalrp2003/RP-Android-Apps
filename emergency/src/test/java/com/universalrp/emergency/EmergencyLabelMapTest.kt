package com.universalrp.emergency

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The emergency vocabulary, checked against the label file that ships in the APK.
 *
 * The Python gate `tools/verify_vasanthi_sound_labels.py` checks the same thing at source level in
 * CI. These tests exist because a Kotlin-side check runs on every build and because a map that
 * drifts is the failure mode this app can least afford: a family told "the doorbell" when it was
 * a smoke alarm has been actively misled.
 */
class EmergencyLabelMapTest {

    /**
     * The label file, read from the module's own assets.
     *
     * Located by walking up from the test's working directory rather than by a hard-coded absolute
     * path, so it works on the CI runner and on a developer's machine alike.
     */
    private val labels: List<String> by lazy {
        var dir: File? = File("").absoluteFile
        var found: File? = null
        while (dir != null) {
            val candidate = File(dir, "emergency/src/main/assets/yamnet_labels.txt")
            if (candidate.isFile) {
                found = candidate
                break
            }
            dir = dir.parentFile
        }
        requireNotNull(found) {
            "yamnet_labels.txt not found; these tests would otherwise check nothing"
        }
        found.readLines().map { it.trim() }.filter { it.isNotEmpty() }
    }

    @Test
    fun everyKeyIsAnAudioSetClassTheModelActuallyPredicts() {
        val known = labels.toSet()
        val unknown = EmergencyLabelMap.all()
            .map { it.englishLabel }
            .filter { it !in known }
        assertEquals(
            "these emergency keys are not classes in the shipped label file, so the app can never " +
                "alert on them",
            emptyList<String>(),
            unknown,
        )
    }

    @Test
    fun everyKeyIsUnique() {
        val names = EmergencyLabelMap.all().map { it.englishLabel }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun everyTamilNameIsTamilScript() {
        val latinish = EmergencyLabelMap.all()
            .filter { !it.tamilLabel.any { ch -> ch.code in 0x0B80..0x0BFF } }
            .map { it.englishLabel }
        assertEquals(emptyList<String>(), latinish)
    }

    @Test
    fun everyAlertSentenceIsTamilScriptAndSaysSomething() {
        for (emergency in EmergencyLabelMap.all()) {
            val hasTamil = emergency.alertText.any { ch -> ch.code in 0x0B80..0x0BFF }
            assertTrue("${emergency.englishLabel} has no Tamil in its alert sentence", hasTamil)
            assertTrue(
                "${emergency.englishLabel} has an alert sentence that is too short to tell anybody " +
                    "anything",
                emergency.alertText.trim().length >= 8,
            )
            assertFalse(
                "${emergency.englishLabel}'s alert sentence is just the Tamil name repeated",
                emergency.alertText.trim() == emergency.tamilLabel,
            )
        }
    }

    @Test
    fun aClassThatIsNotAnEmergencyIsNotReported() {
        // The whole point of the short list: a ceiling fan must not wake a family.
        assertNull(EmergencyLabelMap.forEnglish("Ceiling fan"))
        assertNull(EmergencyLabelMap.forEnglish("Silence"))
        assertNull(EmergencyLabelMap.forEnglish("Typing"))
        assertNull(EmergencyLabelMap.tamilFor("Wind"))
    }

    @Test
    fun anEmergencyResolvesToItself() {
        val fire = EmergencyLabelMap.forEnglish("Fire alarm")
        assertNotNull(fire)
        requireNotNull(fire)
        assertEquals(EmergencyLabelMap.Severity.FIRE, fire.severity)
        assertEquals("தீ எச்சரிக்கை", fire.tamilLabel)
    }

    @Test
    fun allThreeSeveritiesAreRepresented() {
        val severities = EmergencyLabelMap.all().map { it.severity }.toSet()
        assertEquals(
            EmergencyLabelMap.Severity.entries.toSet(),
            severities,
        )
    }

    @Test
    fun theSoundsTheOwnerNamedAreAllCovered() {
        // The scope the owner chose was "hear the emergency sound". These are the classes that
        // scope means, and a regression that drops one of them must fail here rather than on
        // somebody's phone at three in the morning.
        val required = listOf(
            "Smoke detector, smoke alarm",
            "Fire alarm",
            "Siren",
            "Screaming",
            "Shout",
            "Children shouting",
            "Baby cry, infant cry",
            "Shatter",
            "Glass",
            "Doorbell",
            "Ding-dong",
        )
        val missing = required.filter { EmergencyLabelMap.forEnglish(it) == null }
        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun fireNeedsMoreConfidenceThanADoorbell() {
        // A doorbell is a distinctive, narrow-band sound that YAMNet is good at; a shout is not.
        // Requiring the same confidence for both would either miss every shout or fire on every
        // raised voice in an argument.
        assertTrue(
            EmergencyLabelMap.thresholdFor(EmergencyLabelMap.Severity.FIRE) <
                EmergencyLabelMap.thresholdFor(EmergencyLabelMap.Severity.HELP),
        )
        assertTrue(
            EmergencyLabelMap.thresholdFor(EmergencyLabelMap.Severity.HELP) >
                EmergencyLabelMap.thresholdFor(EmergencyLabelMap.Severity.DOOR),
        )
    }
}
