package com.universalrp.cleansweep.voice

import android.content.Context
import com.universalrp.cleansweep.ai.AiProvider
import com.universalrp.cleansweep.ai.AiRequestPolicy
import com.universalrp.cleansweep.ai.AiSettings
import com.universalrp.cleansweep.ai.RequestBudget
import com.universalrp.cleansweep.data.BalancedNewsPolicy
import com.universalrp.cleansweep.data.NewsHeadline
import com.universalrp.cleansweep.data.NewsPolicy
import com.universalrp.cleansweep.data.TamilInfoStripRepo
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneId

/** Replenish category candidates before editing; translations are fallback, never replacements for good Tamil. */
object NewsBulletinPipeline {
    private val mutableReport = MutableStateFlow<Report?>(null)
    val report = mutableReport.asStateFlow()
    private val specialists = setOf(BalancedNewsPolicy.Category.SCIENCE, BalancedNewsPolicy.Category.CYBER, BalancedNewsPolicy.Category.ENVIRONMENT)
    data class Row(val category: BalancedNewsPolicy.Category, val fresh: Int, val eligible: Int, val afterEditing: Int, val english: Int, val translated: Int, val chosen: Int) {
        fun label(tamil: Boolean): String = if (tamil) when (category) {
            BalancedNewsPolicy.Category.STATE -> "தமிழ்நாடு"
            BalancedNewsPolicy.Category.INDIA -> "இந்தியா"
            BalancedNewsPolicy.Category.WORLD -> "உலகம்"
            BalancedNewsPolicy.Category.SCIENCE -> "அறிவியல்"
            BalancedNewsPolicy.Category.CYBER -> "இணையப் பாதுகாப்பு"
            BalancedNewsPolicy.Category.ENVIRONMENT -> "இயற்கை"
        } else when (category) {
            BalancedNewsPolicy.Category.STATE -> "Tamil Nadu"
            BalancedNewsPolicy.Category.INDIA -> "India"
            BalancedNewsPolicy.Category.WORLD -> "World"
            BalancedNewsPolicy.Category.SCIENCE -> "Science"
            BalancedNewsPolicy.Category.CYBER -> "Cyber safety"
            BalancedNewsPolicy.Category.ENVIRONMENT -> "Nature"
        }
        fun reason(tamil: Boolean): String = when {
            chosen > 0 -> if (tamil) "தேர்ந்தெடுக்கப்பட்டது" else "Selected"
            english > 0 -> if (tamil) "மொழிபெயர்ப்பு கிடைக்கவில்லை அல்லது ஏற்கப்படவில்லை" else "Translation unavailable or rejected"
            fresh == 0 -> if (tamil) "புதிய ஆதாரச் செய்தி இல்லை" else "No fresh source item"
            eligible == 0 -> if (tamil) "வடிகட்டப்பட்டது அல்லது ஏற்கனவே படிக்கப்பட்டது" else "Filtered out or already read"
            afterEditing == 0 -> if (tamil) "தொகுப்பின்போது நிராகரிக்கப்பட்டது" else "Rejected during editing"
            else -> if (tamil) "ஒத்த செய்தி அல்லது ஏற்கனவே படிக்கப்பட்டது" else "Duplicate or already read"
        }
    }
    data class Report(val atMs: Long, val rows: List<Row>, val items: List<NewsHeadline>, val translationAttempted: Boolean)

    suspend fun prepare(context: Context, items: List<NewsHeadline>, tamil: Boolean, announced: Set<String>, recent: List<String>, rotation: Int): List<NewsHeadline> {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val nativePool = BalancedNewsPolicy.select(items, announced, recent, tamil, now, zone, rotation, perCategoryMultiplier = 3)
        var edited = nativePool
        var english = emptyList<NewsHeadline>()
        var translated = emptyList<NewsHeadline>()
        var attempted = false
        val config = AiSettings.load(context)
        // One outer deadline covers the entire enhanced path, including fallback feed fetching.
        val budget = RequestBudget(AiRequestPolicy.Use.NEWS)
        withTimeoutOrNull(AiRequestPolicy.Use.NEWS.budgetMs) {
            edited = NewsBulletinEditor.edit(context, nativePool, tamil, budget)
            val nativeSelected = BalancedNewsPolicy.select(edited, announced, recent, tamil, System.currentTimeMillis(), ZoneId.systemDefault(), rotation)
            val missing = specialists.filter { category -> nativeSelected.none { BalancedNewsPolicy.category(it) == category } }.toSet()
            if (tamil && missing.isNotEmpty() && config.ready && config.provider != AiProvider.FREE && budget.remainingMs() >= 8_000L) {
                attempted = true
                val fetched = TamilInfoStripRepo.getEnglishSpecialistsForTranslation(context)
                english = BalancedNewsPolicy.select(fetched, announced, recent, false, System.currentTimeMillis(), ZoneId.systemDefault(), rotation, perCategoryMultiplier = 3)
                    .filter { BalancedNewsPolicy.category(it) in missing }
                    .groupBy { BalancedNewsPolicy.category(it) }.values.flatMap { it.take(2) }
                translated = NewsTranslator.translate(config, english, budget)
            }
        }
        currentCoroutineContext().ensureActive()
        val readyAt = System.currentTimeMillis()
        // Date is evaluated again after all networking. Valid AI rejections are never resurrected by a later timeout.
        val final = BalancedNewsPolicy.select(edited + translated, announced, recent, tamil, readyAt, ZoneId.systemDefault(), rotation)
        fun count(list: List<NewsHeadline>, category: BalancedNewsPolicy.Category) = list.count { BalancedNewsPolicy.category(it) == category && NewsPolicy.isToday(it.publishedAtMs, readyAt, ZoneId.systemDefault()) }
        mutableReport.value = Report(readyAt, BalancedNewsPolicy.Category.values().map { category ->
            Row(category, count(items, category), count(nativePool, category), count(edited, category), count(english, category), count(translated, category), count(final, category))
        }, final, attempted)
        return final
    }
}
