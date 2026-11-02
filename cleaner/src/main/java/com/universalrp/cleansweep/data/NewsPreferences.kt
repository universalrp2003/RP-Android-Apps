package com.universalrp.cleansweep.data

import android.content.Context

object NewsPreferences {
    enum class Language { APP, TAMIL_ONLY, TAMIL_TRANSLATED, ENGLISH }
    private fun prefs(context: Context) = context.getSharedPreferences("news_topics_283", Context.MODE_PRIVATE)
    fun selected(context: Context): Set<BalancedNewsPolicy.Category> = prefs(context).getStringSet("topics", emptySet()).orEmpty()
        .mapNotNull { name -> BalancedNewsPolicy.Category.values().firstOrNull { it.name == name } }.toSet()
    fun categories(context: Context) = selected(context).ifEmpty { BalancedNewsPolicy.DEFAULT_CATEGORIES }
    fun language(context: Context) = runCatching { Language.valueOf(prefs(context).getString("language", "APP")!!) }.getOrDefault(Language.APP)
    fun tamil(context: Context) = when (language(context)) {
        Language.TAMIL_ONLY -> true
        Language.TAMIL_TRANSLATED -> true
        Language.ENGLISH -> false
        Language.APP -> Lang.languageIn(context) == AppLang.TA
    }
    fun translate(context: Context) = tamil(context) && language(context) != Language.TAMIL_ONLY
    fun fingerprint(context: Context) = categories(context).map { it.name }.sorted().joinToString() + ":" + language(context).name
    fun save(context: Context, topics: Set<BalancedNewsPolicy.Category> = selected(context), lang: Language = language(context)) {
        prefs(context).edit().putStringSet("topics", topics.map { it.name }.toSet()).putString("language", lang.name).apply()
        // New choices must not wait for an old feed-cache interval; published/read history stays intact.
        context.getSharedPreferences("cleansweep_info_strip", Context.MODE_PRIVATE).edit()
            .remove("today_fetch_attempt").remove("today_fetch_attempt_en").apply()
        com.universalrp.cleansweep.voice.Announcer.stop()
    }
    fun label(category: BalancedNewsPolicy.Category, tamil: Boolean): String {
        val labels = when (category) {
            BalancedNewsPolicy.Category.STATE -> "Tamil Nadu" to "தமிழ்நாடு"
            BalancedNewsPolicy.Category.INDIA -> "India" to "இந்தியா"
            BalancedNewsPolicy.Category.WORLD -> "World" to "உலகம்"
            BalancedNewsPolicy.Category.SCIENCE -> "Science" to "அறிவியல்"
            BalancedNewsPolicy.Category.CYBER -> "Cyber safety" to "இணையப் பாதுகாப்பு"
            BalancedNewsPolicy.Category.ENVIRONMENT -> "Nature" to "இயற்கை"
            BalancedNewsPolicy.Category.AI -> "AI & technology" to "செயற்கை நுண்ணறிவு"
            BalancedNewsPolicy.Category.WEATHER -> "Weather reports" to "வானிலைச் செய்திகள்"
            BalancedNewsPolicy.Category.SPORTS -> "Sports" to "விளையாட்டு"
            BalancedNewsPolicy.Category.GOVERNMENT -> "Government announcements" to "அரசு அறிவிப்புகள்"
            BalancedNewsPolicy.Category.JOBS -> "Job announcements" to "வேலைவாய்ப்பு அறிவிப்புகள்"
            BalancedNewsPolicy.Category.ENTERTAINMENT -> "Entertainment" to "பொழுதுபோக்கு"
            BalancedNewsPolicy.Category.HUMOUR -> "Humour (not factual news)" to "நகைச்சுவை (உண்மைச் செய்தியல்ல)"
        }
        return if (tamil) labels.second else labels.first
    }
    fun query(category: BalancedNewsPolicy.Category, tamil: Boolean): String = when (category) {
        BalancedNewsPolicy.Category.AI -> if (tamil) "செயற்கை நுண்ணறிவு" else "artificial intelligence technology"
        BalancedNewsPolicy.Category.WEATHER -> if (tamil) "தமிழ்நாடு வானிலை மழை" else "Tamil Nadu weather IMD"
        BalancedNewsPolicy.Category.SPORTS -> if (tamil) "விளையாட்டு கிரிக்கெட்" else "sports India"
        BalancedNewsPolicy.Category.GOVERNMENT -> if (tamil) "அரசு அறிவிப்பு அரசாணை" else "government announcement PIB India"
        BalancedNewsPolicy.Category.JOBS -> if (tamil) "அரசு வேலைவாய்ப்பு அறிவிப்பு" else "government recruitment notification India"
        BalancedNewsPolicy.Category.ENTERTAINMENT -> if (tamil) "தமிழ் சினிமா" else "Tamil cinema entertainment"
        BalancedNewsPolicy.Category.HUMOUR -> if (tamil) "நகைச்சுவை" else "humour comedy"
        else -> label(category, tamil)
    }
}
