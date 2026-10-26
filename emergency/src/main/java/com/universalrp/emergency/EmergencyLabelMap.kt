package com.universalrp.emergency

/**
 * The sounds that mean somebody needs help, with the Tamil the family will read.
 *
 * YAMNet names 521 AudioSet classes. Most of them - "Silence", "Typing", "Wind" - are of no use
 * here, and a classifier that reports every class it is even slightly confident about would fire
 * on a ceiling fan. So this map is the short list, and only these classes can raise an alert.
 *
 * Every key is an exact AudioSet display name from the shipped `yamnet_labels.txt`, never an
 * index. `tools/verify_vasanthi_sound_labels.py` checks that, and it now checks this module's copy
 * too, because a map keyed on a name the model does not emit is a map that silently never fires.
 *
 * Severity is what the alert says and how loud it says it. [Severity.FIRE] and [Severity.HELP] are
 * the ones worth waking somebody for; [Severity.DOOR] is a doorbell, which is worth a notice and
 * not worth a siren.
 */
object EmergencyLabelMap {

    enum class Severity {
        FIRE,
        HELP,
        DOOR,
    }

    data class Emergency(
        val englishLabel: String,
        val tamilLabel: String,
        /** What the family is told, in Tamil, in one line. */
        val alertText: String,
        val severity: Severity,
    )

    private val emergencies: List<Emergency> = listOf(
        // ------------------------------------------------------------------ fire and smoke
        Emergency(
            "Smoke detector, smoke alarm",
            "புகை எச்சரிக்கை",
            "புகை எச்சரிக்கை ஒலி கேட்கப்படுகிறது. உடனே பாருங்கள்.",
            Severity.FIRE,
        ),
        Emergency(
            "Fire alarm",
            "தீ எச்சரிக்கை",
            "தீ எச்சரிக்கை ஒலி கேட்கப்படுகிறது. உடனே பாருங்கள்.",
            Severity.FIRE,
        ),
        Emergency(
            "Fire",
            "தீ",
            "தீ எரியும் ஒலி கேட்கப்படுகிறது.",
            Severity.FIRE,
        ),
        Emergency(
            "Siren",
            "சைரன்",
            "சைரன் ஒலி கேட்கப்படுகிறது.",
            Severity.FIRE,
        ),
        Emergency(
            "Civil defense siren",
            "பேரழிவ்ச்சை சைரன்",
            "பேரழிவ்ச்சை சைரன் ஒலி கேட்கப்படுகிறது.",
            Severity.FIRE,
        ),
        Emergency(
            "Fire engine, fire truck (siren)",
            "தீ அணியன் வாகனம்",
            "தீ அணியன் வாகனம் வருகிறது.",
            Severity.FIRE,
        ),
        // ------------------------------------------------------------------ somebody needs help
        Emergency(
            "Screaming",
            "கத்தும் ஒலி",
            "யாரோ கத்தும் ஒலி கேட்கப்படுகிறது. உடனே பாருங்கள்.",
            Severity.HELP,
        ),
        Emergency(
            "Shout",
            "கூச்சல்",
            "யாரோ கூச்சலிடும் ஒலி கேட்கப்படுகிறது.",
            Severity.HELP,
        ),
        Emergency(
            "Yell",
            "அலறும் ஒலி",
            "யாரோ அலறும் ஒலி கேட்கப்படுகிறது.",
            Severity.HELP,
        ),
        Emergency(
            "Children shouting",
            "குழந்தைகள் கூச்சல்",
            "குழந்தைகள் கூச்சலிடும் ஒலி கேட்கப்படுகிறது.",
            Severity.HELP,
        ),
        Emergency(
            "Crying, sobbing",
            "அழும் ஒலி",
            "யாரோ அழும் ஒலி கேட்கப்படுகிறது.",
            Severity.HELP,
        ),
        Emergency(
            "Baby cry, infant cry",
            "குழந்தை அழுகை",
            "குழந்தை அழும் ஒலி கேட்கப்படுகிறது.",
            Severity.HELP,
        ),
        Emergency(
            "Shatter",
            "உடைந்து விழும் ஒலி",
            "ஏதோ உடைந்து விழும் ஒலி கேட்கப்படுகிறது.",
            Severity.HELP,
        ),
        Emergency(
            "Glass",
            "கண்ணாடி உடைவு",
            "கண்ணாடி உடைந்து விழும் ஒலி கேட்கப்படுகிறது.",
            Severity.HELP,
        ),
        Emergency(
            "Gunshot, gunfire",
            "துப்பாக்கி ஒலி",
            "துப்பாக்கி சொடுக்கும் ஒலி கேட்கப்படுகிறது. உடனே பாருங்கள்.",
            Severity.HELP,
        ),
        // ------------------------------------------------------------------ the door
        Emergency(
            "Doorbell",
            "கதவு மணி",
            "கதவு மணி ஒலி கேட்கப்படுகிறது.",
            Severity.DOOR,
        ),
        Emergency(
            "Ding-dong",
            "டிங்-டாங்",
            "கதவு மணி ஒலி கேட்கப்படுகிறது.",
            Severity.DOOR,
        ),
        Emergency(
            "Ding",
            "மணி ஒலி",
            "மணி ஒலி கேட்கப்படுகிறது.",
            Severity.DOOR,
        ),
    )

    private val byEnglish: Map<String, Emergency> = emergencies.associateBy { it.englishLabel }

    /** The Tamil name for an AudioSet class, or null if it is not an emergency this app reports. */
    fun tamilFor(englishLabel: String): String? = byEnglish[englishLabel]?.tamilLabel

    /** The full record for an AudioSet class, or null if it is not an emergency. */
    fun forEnglish(englishLabel: String): Emergency? = byEnglish[englishLabel]

    /** Every emergency this app can report, in the order they are listed above. */
    fun all(): List<Emergency> = emergencies

    /**
     * How confident the model has to be before a class is believed.
     *
     * Higher than the general sound-naming threshold, because a false alert wakes a family at
     * three in the morning and a missed sound name does not. A smoke alarm is a distinctive,
     * narrow-band sound that YAMNet is good at; a shout is not, and is correspondingly more likely
     * to be wrong.
     */
    fun thresholdFor(severity: Severity): Float = when (severity) {
        Severity.FIRE -> 0.45f
        Severity.HELP -> 0.55f
        Severity.DOOR -> 0.40f
    }
}
