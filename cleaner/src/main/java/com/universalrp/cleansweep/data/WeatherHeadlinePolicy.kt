package com.universalrp.cleansweep.data

import java.util.Locale

/**
 * Rejects weather "news" that cannot answer the only question it raises: **where** is it raining?
 *
 * The owner reported hearing this on the 11 November 2026 build, twice in one bulletin:
 *
 *   "Weather Update | தமிழ்நாட்டில் இன்று இந்த 9 மாவட்டங்களில் மழை வெளுக்கும்..
 *    எங்கெல்லாம் தெரியுமா? வானிலை அப்டேட்.!"
 *
 *   → "Today in these 9 districts of Tamil Nadu it will rain... do you know where all? Weather update!"
 *
 * It is a clickbait tease: it promises a list of districts and then withholds it. Spoken aloud it
 * is worse than saying nothing, because a listener cannot scroll to find out. The second item
 * ("Today's rain in the 9 districts of Tamil Nadu..? Weather App" in English) has the same defect.
 *
 * Two rules, both deliberately narrow so ordinary weather reporting survives:
 *
 * 1. A weather headline that *asks* the listener a question is out. The bulletin has no
 *    follow-up turn, so a question is a promise it cannot keep.
 * 2. A weather headline that counts districts as a teaser ("these 9 districts") without naming
 *    even one is out. If it names a district, it is real information and is kept — the listener
 *    hears the name, which is the whole point.
 *
 * Non-weather headlines are never touched here. Pure and deterministic, so both rules are
 * regression-tested on the JVM.
 */
object WeatherHeadlinePolicy {

    /** Weather words, English and Tamil, that make a headline a weather story. */
    private val WEATHER = Regex(
        "(?i)(?:weather|rainfall|rain\\s+update|rains?\\b|monsoon|cyclone|thunderstorm|heat\\s?wave|" +
            "மழை|வானிலை|புயல்|கனமழை|சாரல்|இடி\\s*மின்னல்|வெப்ப\\s*அலை)"
    )

    /** A question the bulletin cannot answer. Tamil question-shaped teasers included. */
    private val ASKS_LISTENER = Regex(
        "(?i)(?:\\?|？|" +
            "தெரியுமா|எங்கெல்லாம்|எங்கே\\s*மழை|எங்கு\\s*மழை|" +
            "which\\s+districts|where\\s+(?:is|will)\\s+it\\s+rain|do\\s+you\\s+know\\s+where|" +
            "know\\s+where\\s+it\\s+will\\s+rain)"
    )

    /** "9 districts" / "இந்த 9 மாவட்டங்கள்" — a count used as the teaser. */
    // "9 மாவட்டங்களில்" carries a locative ending, so match the stem "மாவட்டங்கள" too.
    private val DISTRICT_COUNT = Regex(
        "(?i)(?:\\d{1,2}\\s*(?:districts?|மாவட்டங்கள|மாவட்டம்)|" +
            "these\\s+\\d{1,2}|இந்த\\s*\\d{1,2})"
    )

    /**
     * The 38 districts of Tamil Nadu, English and Tamil spellings. Kept here so the policy can
     * answer "did this headline actually name a place?" without a network call.
     */
    private val DISTRICTS = listOf(
        "ariyalur", "chengalpattu", "chennai", "coimbatore", "cuddalore", "dharmapuri",
        "dindigul", "erode", "kallakurichi", "kancheepuram", "kanchipuram", "kanyakumari",
        "karur", "krishnagiri", "madurai", "mayiladuthurai", "nagapattinam", "namakkal",
        "nilgiris", "perambalur", "pudukkottai", "ramanathapuram", "ranipet",
        "salem", "sivagangai", "tenkasi", "thanjavur", "theni", "thoothukudi",
        "tiruchirappalli", "tirunelveli", "tirupathur", "tiruppur", "tiruvallur", "tiruvannamalai",
        "tiruvarur", "vellore", "viluppuram", "virudhunagar", "அரியலூர்", "செங்கல்பட்டு",
        "சென்னை", "கோயம்புத்தூர்", "கடலூர்", "தர்மபுரி", "திண்டுக்கல்",
        "ஈரோடு", "கள்ளக்குறிச்சி", "காஞ்சிபுரம்", "கன்னியாகுமரி", "கரூர்", "கிருஷ்ணகிரி",
        "மதுரை", "மயிலாடுதுறை", "நாகப்பட்டினம்", "நாமக்கல்", "நீலகிரி", "பெரம்பலூர்",
        "புதுக்கோட்டை", "ராமநாதபுரம்", "ராணிப்பேட்டை", "சேலம்", "சிவகங்கை", "தென்காசி",
        "தஞ்சாவூர்", "தேனி", "தூத்துக்குடி", "திருச்சி", "திருநெல்வேலி", "திருப்பத்தூர்",
        "திருப்பூர்", "திருவள்ளூர்", "திருவண்ணாமலை", "திருவாரூர்", "வேலூர்", "விழுப்புரம்",
        "விருதுநகர்", "பல்லடம்", "பொள்ளாச்சி", "திருச்சிராப்பள்ளி", "நெல்லை", "குமரி",
    ).map { it.lowercase(Locale.ROOT) }

    /** True when the text is a weather story at all. */
    fun isWeather(headline: String): Boolean = WEATHER.containsMatchIn(headline)

    /**
     * Tamil attaches case endings to the stem: a rain report says "சென்னையில்" or
     * "காஞ்சிபுரத்தில்", never the bare dictionary form "சென்னை" / "காஞ்சிபுரம்". Matching only the
     * bare form silently failed on real reports (caught by the JVM tests), so each Tamil name also
     * matches its stem. English needs no stemming.
     */
    private fun stems(name: String): List<String> {
        val tamil = name.any { it.code in 0x0B80..0x0BFF }
        if (!tamil) return listOf(name)
        val stem = when {
            name.endsWith("ம்") -> name.dropLast(2)
            name.last() in "ைிு" -> name.dropLast(1)
            else -> name
        }
        return if (stem.length >= 3 && stem != name) listOf(name, stem) else listOf(name)
    }

    /** True when at least one district or city is named, so the listener learns where. */
    fun namesAPlace(headline: String): Boolean = findDistricts(headline).isNotEmpty()

    private fun findDistricts(headline: String): List<Pair<Int, String>> {
        val lower = headline.lowercase(Locale.ROOT)
        val found = ArrayList<Pair<Int, String>>()
        for (district in DISTRICTS) {
            // Prefer the bare name; fall back to the stem so inflected place names still count.
            val hit = stems(district).firstNotNullOfOrNull { form -> lower.indexOf(form).takeIf { it >= 0 } }
                ?: continue
            found.add(hit to district)
        }
        return found
    }

    /**
     * True when a weather headline teases information it never gives: it asks the listener a
     * question, or it counts districts without naming one.
     */
    fun isUnanswerable(headline: String): Boolean {
        if (!isWeather(headline)) return false
        if (ASKS_LISTENER.containsMatchIn(headline)) return true
        if (DISTRICT_COUNT.containsMatchIn(headline) && !namesAPlace(headline)) return true
        return false
    }

    /**
     * The owner-facing explanation shown in Settings when a weather slot is empty for this reason.
     * Kept beside the rule so the words can never drift from the behaviour they describe.
     */
    fun rejectionReason(tamil: Boolean): String = if (tamil)
        "மழை எங்கே என்று சொல்லாத வானிலை செய்தி நிராகரிக்கப்பட்டது"
    else "Weather item rejected: it never said where the rain is"

    /**
     * The district names this headline actually promises rain for, in reading order, deduplicated.
     * Empty when the headline names none — which is exactly the case [isUnanswerable] rejects.
     */
    fun districtsNamed(headline: String): List<String> =
        findDistricts(headline).sortedBy { it.first }.map { it.second }.distinct()
}
