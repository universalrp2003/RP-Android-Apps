package com.universalrp.cleansweep.data

import java.time.ZoneId
import java.util.Locale

/** Source-grounded selection. Quotas are ceilings, never a reason to invent or recycle news. */
object BalancedNewsPolicy {
    enum class Category(val quota: Int) { STATE(3), INDIA(1), WORLD(1), SCIENCE(1), CYBER(1), ENVIRONMENT(1) }
    private val junk = Regex(
        "today.?s? headlines|top \\d+ news|headlines?\\s*\\d*\\s*(?:am|pm)|" +
            "தலைப்புச்? செய்திகள்|தலைப்புச்செய்திகள்|செய்தித் தொகுப்பு|" +
            "photo gallery|video gallery|watch live|live updates|live blog|" +
            "புகைப்படத் தொகுப்பு|வீடியோ தொகுப்பு|நேரலை|ராசிபலன்|ஜோதிடம்|" +
            "sponsored|partner content|deal of the day|buy now|amazon sale|flipkart sale|" +
            "சிறப்பு தள்ளுபடி|வாங்கிடுங்க|வாங்குங்கள்|நம்ப முடியாத|you won't believe|click here",
        RegexOption.IGNORE_CASE,
    )
    private val science = listOf("science", "scientist", "research", "discovery", "invention", "isro", "nasa", "space", "satellite", "அறிவியல்", "விஞ்ஞான", "ஆராய்ச்சி", "ஆய்வு", "கண்டுபிடி", "விண்வெளி", "இஸ்ரோ", "நாசா", "செயற்கைக்கோள்")
    private val cyber = listOf("phishing", "malware", "ransomware", "cyber", "security update", "security flaw", "otp", "சைபர்", "இணைய மோசடி", "ஆன்லைன் மோசடி", "யுபிஐ", "யு.பி.ஐ", "டிஜிட்டல் கைது", "digital arrest", "upi", "sms", "android security", "mobile scam", "online scam", "phone scam", "தீம்பொருள்", "பாதுகாப்பு புதுப்பிப்பு", "குறுஞ்செய்தி", "இணைய பாதுகாப்பு")
    private val environment = listOf("climate", "biodiversity", "wildlife", "conservation", "pollution", "environment", "forest", "flood", "cyclone", "காலநிலை", "சுற்றுச்சூழல்", "பல்லுயிர்", "வனவிலங்கு", "மாசு", "நீர்நிலை", "வெள்ளம்", "புயல்", "மழை")
    private val state = listOf("tamil nadu", "tamilnadu", "chennai", "pudukkottai", "tiruchi", "madurai", "coimbatore", "தமிழக", "தமிழ்நா", "சென்னை", "புதுக்கோட்டை", "திருச்சி", "மதுரை", "கோவை")
    private val india = listOf("india", "delhi", "supreme court", "parliament", "rbi", "இந்திய", "டெல்லி", "உச்சநீதிமன்ற", "உச்ச நீதிமன்ற", "நாடாளுமன்ற", "ரிசர்வ் வங்கி")
    private val world = listOf("united nations", "united states", "ukraine", "russia", "china", "japan", "sri lanka", "pakistan", "europe", "gaza", "israel", "அமெரிக்க", "ஐநா", "ஐ.நா", "உக்ரைன்", "ரஷ்ய", "சீனா", "ஜப்பான்", "இலங்கை", "பாகிஸ்தான்", "ஐரோப்ப", "காசா", "இஸ்ரேல்")
    private fun has(text: String, terms: List<String>) = terms.any { term ->
        if (term.all { it.code < 128 }) Regex("(?<![a-z])" + Regex.escape(term) + "(?![a-z])").containsMatchIn(text)
        else text.contains(term)
    }

    private val trustedAggregatedPublishers = setOf(
        "bbc", "bbc tamil", "bbc news", "bbc news தமிழ்", "bbc - homepage", "the hindu", "hindu tamil thisai",
        "hindutamil.in", "இந்து தமிழ் திசை", "தினமணி", "dinamani", "தினத்தந்தி", "dailythanthi", "daily thanthi",
        "தினமலர்", "dinamalar", "news18 tamil", "news18", "news18 தமிழ்", "tamil news18", "tamil.oneindia.com",
        "oneindia tamil", "oneindia", "tamil oneindia", "புதிய தலைமுறை", "puthiyathalaimurai", "puthiya thalaimurai",
        "vikatan", "விகடன்", "ndtv", "ndtv.com", "indian express", "the indian express", "the new indian express",
        "hindustan times", "india today", "times of india", "the times of india", "gizbot tamil", "tamil gizbot",
        "pib", "press information bureau", "down to earth", "mongabay india", "science daily", "sciencedaily",
    )
    fun trustedAggregatedPublisher(source: String): Boolean = source.trim().lowercase(Locale.ROOT) in trustedAggregatedPublishers

    fun isJunk(title: String): Boolean = junk.containsMatchIn(title) ||
        title.count { it == '|' } >= 2 || title.contains("…") || title.contains("...") ||
        title.trim().endsWith("?")

    /** Mixed feeds are never assigned WORLD/INDIA just because of their publisher. */
    fun category(item: NewsHeadline): Category? {
        val text = item.title.lowercase(Locale.ROOT)
        val feed = item.category.lowercase(Locale.ROOT)
        // A specialist feed is still checked for actual topic evidence; gadget ads aren't discoveries.
        if (has(text, cyber)) return Category.CYBER
        if (has(text, environment)) return Category.ENVIRONMENT
        if (has(text, science)) return Category.SCIENCE
        if (feed in setOf("science", "cyber", "environment")) return null
        if (has(text, state) || feed in setOf("தமிழ்நாடு", "tamil nadu")) return Category.STATE
        if (has(text, india) || feed in setOf("இந்தியா", "india")) return Category.INDIA
        if (has(text, world) || feed in setOf("சர்வதேசம்", "world")) return Category.WORLD
        return null // uncertain is better than confidently mislabelled
    }

    /** Stable event-level approximation, including across publishers; no numeric facts are discarded. */
    fun sameEvent(a: String, b: String): Boolean {
        if (NewsPolicy.titleKey(a) == NewsPolicy.titleKey(b)) return true
        fun tokens(s: String) = s.lowercase(Locale.ROOT).split(Regex("[^\\p{L}\\p{M}\\p{N}]+"))
            .filter { it.length > 2 && it !in setOf("the", "and", "for", "with", "from", "after", "news", "today") }.toSet()
        val x = tokens(a); val y = tokens(b)
        if (minOf(x.size, y.size) < 4) return false
        // Different numbers can be a material update; do not merge those automatically.
        fun numbers(s: String) = Regex("\\d+").findAll(s).map { it.value }.toSet()
        if (numbers(a) != numbers(b)) return false
        return x.intersect(y).size.toDouble() / x.union(y).size.coerceAtLeast(1) >= 0.65
    }

    /** Rotate preferences inside specialist categories, not the mandatory geographic mix. */
    fun subtopicScore(item: NewsHeadline, rotation: Int): Int {
        val groups = when (category(item)) {
            Category.SCIENCE -> listOf(listOf("space", "isro", "nasa", "விண்வெளி", "இஸ்ரோ"), listOf("invention", "கண்டுபிடி"), listOf("research", "ஆய்வு", "ஆராய்ச்சி"))
            Category.CYBER -> listOf(listOf("sms", "phishing", "குறுஞ்செய்தி"), listOf("android", "malware", "ஆண்ட்ராய்டு", "தீம்பொருள்"), listOf("upi", "otp", "யுபிஐ", "ஓடிபி"), listOf("privacy", "தனியுரிமை"))
            Category.ENVIRONMENT -> listOf(listOf("water", "நீர்"), listOf("wildlife", "biodiversity", "பல்லுயிர்", "வனவிலங்கு"), listOf("climate", "காலநிலை"), listOf("pollution", "மாசு"))
            else -> return 0
        }
        return if (has(item.title.lowercase(Locale.ROOT), groups[Math.floorMod(rotation, groups.size)])) 1 else 0
    }

    fun reserveCategories(items: List<NewsHeadline>, limit: Int): List<NewsHeadline> {
        val reserved = Category.values().flatMap { c -> items.filter { category(it) == c }.take(10) }
        return (reserved + items).distinctBy { it.key }.take(limit)
    }

    fun select(
        items: List<NewsHeadline>, announced: Set<String>, recentTitles: List<String>, tamil: Boolean,
        nowMs: Long, zone: ZoneId, rotation: Int,
    ): List<NewsHeadline> {
        val valid = items.map { it.copy(title = NewsSpeechSanitizer.cleanHeadline(it.title, it.source)) }
            .filter { NewsPolicy.isToday(it.publishedAtMs, nowMs, zone) &&
                NewsPolicy.canonicalLink(it.link) != null &&
                NewsSpeechSanitizer.isCompleteSpeakableHeadline(it.title, tamil) &&
                it.key !in announced && recentTitles.none { old -> sameEvent(old, it.title) } }
        val picked = ArrayList<NewsHeadline>(8)
        val publishers = mutableMapOf<String, Int>()
        for (bucket in Category.values()) {
            val candidates = valid.filter { category(it) == bucket }
            // Prefer rotation, then source diversity, then freshness within today's pool.
            val ordered = candidates.sortedWith(compareByDescending<NewsHeadline> { subtopicScore(it, rotation) }
                .thenBy { publishers[it.source] ?: 0 }.thenByDescending { it.publishedAtMs })
            for (item in ordered) {
                if (picked.count { category(it) == bucket } >= bucket.quota) break
                if (picked.any { sameEvent(it.title, item.title) || NewsPolicy.canonicalLink(it.link) == NewsPolicy.canonicalLink(item.link) }) continue
                picked.add(item)
                publishers[item.source] = (publishers[item.source] ?: 0) + 1
            }
        }
        return picked
    }

    /** AI can choose an exact source sentence, never supply arbitrary generated facts. */
    fun evidenceOptions(item: NewsHeadline, tamil: Boolean): List<String> {
        val options = listOf(item.title) + item.description.split(Regex("(?<=[.!?])\\s+"))
        val titleTokens = item.title.lowercase(Locale.ROOT).split(Regex("[^\\p{L}\\p{M}\\p{N}]+"))
            .filter { it.length > 3 }.toSet()
        return options.map { NewsSpeechSanitizer.cleanHeadline(it, item.source) }
            .filter { NewsSpeechSanitizer.isCompleteSpeakableHeadline(it, tamil) }
            .filter { candidate -> candidate == item.title || titleTokens.count { candidate.lowercase(Locale.ROOT).contains(it) } >= 2 }
            .distinct().take(4)
    }
}
