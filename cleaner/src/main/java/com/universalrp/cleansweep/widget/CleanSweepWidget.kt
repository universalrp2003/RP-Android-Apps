package com.universalrp.cleansweep.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.universalrp.cleansweep.MainActivity
import com.universalrp.cleansweep.R
import com.universalrp.cleansweep.data.*
import com.universalrp.cleansweep.work.WeatherWatchWorker
import kotlinx.coroutines.*

/** Launcher widget only; the separate floating status pill retains its existing watts-only charging mode. */
open class CleanSweepWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { refresh(context) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) { refresh(context) }
    override fun onDeleted(context: Context, ids: IntArray) {
        val edit = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        ids.forEach { edit.remove("mode_$it"); edit.remove("profile_$it") }; edit.apply()
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(ACTION_REFRESH_WIDGET, ACTION_MODE, AppWidgetManager.ACTION_APPWIDGET_UPDATE, AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED)) {
            val manualWeatherRefresh = intent.action == ACTION_REFRESH_WIDGET
            if (intent.action == ACTION_MODE) {
                val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                if (id !in ownedIds(context).toSet()) return
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val profile = profile(context, id).next()
                prefs.edit().putString("profile_$id", profile.name).apply()
            }
            val pending = goAsync()
            val finished = java.util.concurrent.atomic.AtomicBoolean(false)
            fun finish() { if (finished.compareAndSet(false, true)) runCatching { pending.finish() } }
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ finish() }, 8_000)
            if (manualWeatherRefresh) {
                val app = context.applicationContext
                scope.launch {
                    try {
                        val anyWeatherVisible = ownedIds(app).any { profile(app, it) == CompactWidgetPolicy.Profile.WEATHER }
                        if (anyWeatherVisible) {
                            runCatching { WeatherWatchWorker.executeCheck(app, speakIfTriggered = false) }
                        }
                        refresh(app).join()
                    } finally {
                        finish()
                    }
                }
            } else {
                refresh(context).invokeOnCompletion { finish() }
            }
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val ACTION_CLEAN = "com.universalrp.cleansweep.WIDGET_CLEAN"
        const val ACTION_SPEAK = "com.universalrp.cleansweep.WIDGET_SPEAK" // Retain legacy constant; no unsolicited widget speech.
        const val ACTION_DETAILS = "com.universalrp.cleansweep.WIDGET_DETAILS"
        const val ACTION_REFRESH_WIDGET = "com.universalrp.cleansweep.WIDGET_REFRESH"
        const val ACTION_MODE = "com.universalrp.cleansweep.WIDGET_MODE"
        const val EXTRA_TARGET_SCREEN = "widget_target_screen"
        private const val PREFS = "cleansweep_widget_views"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var refreshJob: Job? = null
        @Volatile private var anotherFrame = false

        private fun ownedIds(context: Context): IntArray {
            val manager = AppWidgetManager.getInstance(context)
            return listOf(
                CleanSweepWidget::class.java,
                CleanSweepDataWidget::class.java,
                CleanSweepPhoneWidget::class.java,
                CleanSweepWeatherWidget::class.java,
            ).flatMap { manager.getAppWidgetIds(ComponentName(context, it)).toList() }.toIntArray()
        }

        private fun profile(context: Context, id: Int): CompactWidgetPolicy.Profile {
            val component = AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.provider?.className.orEmpty()
            return CompactWidgetPolicy.parse(
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("profile_$id", null),
                CompactWidgetPolicy.defaultFor(component),
            )
        }

        /** Coalesced IO snapshots; never do battery/stats/storage work on Compose or receiver's main thread. */
        @Synchronized fun refresh(context: Context): Job {
            if (refreshJob?.isActive == true) { anotherFrame = true; return refreshJob!! }
            val app = context.applicationContext
            val job = scope.launch(start = CoroutineStart.LAZY) {
                try {
                    for (frame in 0..1) {
                        synchronized(CleanSweepWidget.Companion) { anotherFrame = false }
                        refreshNow(app)
                        if (!synchronized(CleanSweepWidget.Companion) { anotherFrame }) break
                    }
                } catch (e: CancellationException) { throw e } catch (_: Exception) { /* An unavailable launcher/widget must not break monitoring. */ } finally {
                    val running = currentCoroutineContext()[Job]
                    val followUp = synchronized(CleanSweepWidget.Companion) {
                        if (refreshJob == running) {
                            refreshJob = null; anotherFrame
                        } else false
                    }
                    if (followUp) scope.launch { delay(100); refresh(app) }
                }
            }
            refreshJob = job; job.start()
            return job
        }

        private fun refreshNow(context: Context) {
            val ids = ownedIds(context); if (ids.isEmpty()) return
            val manager = AppWidgetManager.getInstance(context)
            val battery = runCatching { BatteryReader.read(context) }.getOrNull()
            val checked = System.currentTimeMillis()
            val storage = runCatching { StorageInfoProvider.read() }.getOrNull()
            val data = runCatching { DataUsageTracker.getUsageInfo(context) }.getOrNull()
            val weather = runCatching { WeatherAlertStore.readCachedForWidget(context) }.getOrNull()
            ids.forEach { id ->
                manager.updateAppWidget(id, build(context, id, manager.getAppWidgetOptions(id), battery, storage, data, weather, checked))
            }
        }

        private fun build(
            context: Context,
            id: Int,
            options: Bundle,
            battery: BatteryReading?,
            storage: StorageInfo?,
            data: DataUsageTracker.UsageInfo?,
            weather: WeatherAlertStore.CachedWidgetWeather?,
            checked: Long,
        ): RemoteViews {
            val tamil = Lang.languageIn(context) == AppLang.TA
            fun label(en: String, ta: String) = if (tamil) ta else en
            val profile = profile(context, id)
            val views = RemoteViews(context.packageName, R.layout.widget_cleansweep_compact)
            val percent = WidgetReadingPolicy.percent(battery?.percent, battery?.reportedLevel, battery?.reportedScale)
            val plan = runCatching { PlanStore.load(context) }.getOrNull()
            val planSet = plan != null && (plan.mode == PlanDataMode.TOTAL || plan.mode == PlanDataMode.DAILY) && plan.startDay != null
            val planLine = when {
                !planSet -> label("Set your plan in the app", "ஆப்பில் திட்டத்தை அமைக்கவும்")
                else -> PlanPolicy.remainingText(plan!!, java.time.ZonedDateTime.now(), tamil)
                    ?: label("Plan dates are set", "திட்டத் தேதிகள் அமைக்கப்பட்டுள்ளன")
            }
            val leftLine = when {
                data == null -> label("Data reading unavailable", "தரவு வாசிப்பு இல்லை")
                !data.readingAvailable -> label("Set Usage access for data counts", "தரவு எண்ணிக்கைக்கு பயன்பாட்டு அணுகலை அமைக்கவும்")
                !DataUsageTracker.hasConfiguredPlan(context) -> label("Set the plan limit", "திட்ட வரம்பை அமைக்கவும்")
                else -> data.formattedRemaining
            }

            val weatherClock = weather?.lastCheckedAtMs?.takeIf { it > 0L }?.let { DisplayTime.clock(it) }.orEmpty()
            val urgentWeatherBrief = weather?.let {
                CompactWidgetPolicy.urgentBriefOverride(
                    tamil = tamil,
                    enabled = it.enabled,
                    placeName = it.placeName,
                    severityName = it.severityName,
                    minutesUntilRain = it.minutesUntilRain,
                    lastCheckedAtMs = it.lastCheckedAtMs,
                    nowMs = checked,
                    clockLabel = weatherClock,
                )
            }

            when (profile) {
                CompactWidgetPolicy.Profile.BATTERY -> {
                    views.setTextViewText(R.id.widget_title, label("Battery & plan", "பேட்டரி & திட்டம்"))
                    views.setTextViewText(R.id.widget_battery, percent)
                    val unplug = LastUnplugRepo.load(context)
                    val stateLine = when {
                        battery == null -> label("Not reported", "தகவல் இல்லை")
                        battery.powerConnected && battery.charging -> label("Charging", "சார்ஜ் ஆகிறது")
                        battery.powerConnected -> label("Connected", "இணைந்துள்ளது")
                        unplug != null && LastUnplugPolicy.usable(unplug, checked) ->
                            label("Unplugged at ", "சார்ஜர் நீக்கியது ") + CompactWidgetPolicy.wholePercent(unplug.percent)
                        else -> label("On battery", "பேட்டரியில் இயங்குகிறது")
                    }
                    views.setTextViewText(R.id.widget_state, "$stateLine · ${DisplayTime.clock(checked)}")
                    views.setTextViewText(R.id.widget_data,
                        (if (leftLine.isNotBlank()) (label("Left ", "மீதி ") + leftLine) else leftLine) + " · " + planLine)
                    views.setTextViewText(R.id.widget_brief, urgentWeatherBrief ?: (
                        label("Storage free ", "காலி சேமிப்பு ") + (storage?.free?.formatBytes() ?: "—") + " · " + WidgetReadingPolicy.temperature(battery?.temperatureC)))
                }

                CompactWidgetPolicy.Profile.DATA -> {
                    views.setTextViewText(R.id.widget_title, label("Plan & days", "திட்டம் & நாட்கள்"))
                    views.setTextViewText(R.id.widget_battery, if (leftLine.isNotBlank()) leftLine else "—")
                    views.setTextViewText(R.id.widget_state, planLine)
                    val pack = plan?.let {
                        if (it.mode == PlanDataMode.DAILY) PlanPolicy.formatMb(it.dailyMb) + label(" / day", " / நாள்")
                        else if (it.totalUnlimited) label("Unlimited total", "மொத்தம் வரம்பற்றது")
                        else PlanPolicy.formatMb(PlanPolicy.effectiveTotalMb(it)) + label(" pack", " பேக்")
                    } ?: ""
                    val total = data?.let { label("Today ", "இன்று ") + it.formattedToday } ?: label("Today —", "இன்று —")
                    views.setTextViewText(R.id.widget_data, listOf(pack, total).filter { it.isNotBlank() }.joinToString(" · "))
                    views.setTextViewText(R.id.widget_brief, urgentWeatherBrief ?: label("Open the app to see the full split", "முழு விவரத்திற்கு ஆப்பைத் திறக்கவும்"))
                }

                CompactWidgetPolicy.Profile.PHONE -> {
                    val review = PermissionReviewStore.load(context)?.takeIf { it.valid() && !it.isFuture(checked) }
                    val high = review?.high ?: 0
                    views.setTextViewText(R.id.widget_title, label("Security", "பாதுகாப்பு"))
                    views.setTextViewText(R.id.widget_battery,
                        if (review != null) high.toString() + label(" high", " அதிக") else "—")
                    views.setTextViewText(R.id.widget_state, when {
                        review == null -> label("No saved review", "சேமித்த விவரம் இல்லை")
                        review.incomplete -> label("Review was incomplete", "விவரம் முழுமையில்லை")
                        high > 0 -> label("Fix these first", "இவற்றை முதலில் சரிசெய்யவும்")
                        review.medium > 0 -> label("Medium risks only", "நடுத்தர ஆபத்துகள் மட்டும்")
                        else -> label("Nothing standing out", "குறிப்பிடத்தகுந்தது இல்லை")
                    })
                    views.setTextViewText(R.id.widget_data, if (review != null)
                        label("Medium ", "நடுத்தர ") + review.medium + label(" · apps ", " · ஆப்கள் ") + review.appsChecked + " · " + DisplayTime.dated(review.atMs, tamil)
                    else label("Tap Open to run the permission review", "வேண்டுமென்றால் திற-ஐத் தட்டி சரிபார்க்கவும்"))
                    views.setTextViewText(R.id.widget_brief, urgentWeatherBrief ?: (
                        label("Storage free ", "காலி சேமிப்பு ") + (storage?.free?.formatBytes() ?: "—") + " · " + percent))
                }

                CompactWidgetPolicy.Profile.WEATHER -> {
                    val card = CompactWidgetPolicy.formatWeatherCard(
                        tamil = tamil,
                        enabled = weather?.enabled == true,
                        placeName = weather?.placeName.orEmpty(),
                        severityName = weather?.severityName.orEmpty(),
                        minutesUntilRain = weather?.minutesUntilRain,
                        temperatureC = weather?.temperatureC,
                        windGustKmh = weather?.windGustKmh ?: 0.0,
                        sourceName = weather?.sourceName.orEmpty(),
                        lastCheckedAtMs = weather?.lastCheckedAtMs ?: 0L,
                        clockLabel = weatherClock,
                        peakPrecipMmPerHour = weather?.peakPrecipMmPerHour ?: 0.0,
                        currentPrecipMmPerHour = weather?.currentPrecipMmPerHour ?: 0.0,
                    )
                    views.setTextViewText(R.id.widget_title, card.title)
                    views.setTextViewText(R.id.widget_battery, card.hero)
                    views.setTextViewText(R.id.widget_state, card.state)
                    views.setTextViewText(R.id.widget_data, card.details)
                    views.setTextViewText(R.id.widget_brief, card.brief)
                }
            }
            views.setViewVisibility(R.id.widget_brief, if (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 165) < 145) View.GONE else View.VISIBLE)
            views.setTextViewText(R.id.widget_mode, label("View", "காட்சி"))
            views.setTextViewText(R.id.widget_details, label("Open", "திற"))
            views.setTextViewText(R.id.widget_refresh, label("Refresh", "புதுப்பி"))
            views.setContentDescription(R.id.widget_mode, label("Switch battery, plan, security or weather view", "பேட்டரி, திட்டம், பாதுகாப்பு அல்லது வானிலைக் காட்சியை மாற்று"))
            views.setContentDescription(R.id.widget_battery, label("Main reading ", "முதன்மை அளவு "))
            val targetScreen = CompactWidgetPolicy.targetScreenName(profile)
            views.setOnClickPendingIntent(R.id.widget_mode, action(context, id, ACTION_MODE, false, null))
            views.setOnClickPendingIntent(R.id.widget_refresh, action(context, id, ACTION_REFRESH_WIDGET, false, null))
            views.setOnClickPendingIntent(R.id.widget_details, action(context, id, ACTION_DETAILS, true, targetScreen))
            views.setOnClickPendingIntent(R.id.widget_root, action(context, id, ACTION_DETAILS, true, targetScreen))
            return views
        }

        private fun action(context: Context, id: Int, action: String, activity: Boolean, targetScreen: String?): PendingIntent {
            val intent = if (activity) Intent(context, MainActivity::class.java) else Intent().setComponent(
                AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.provider ?: ComponentName(context, CleanSweepWidget::class.java))
            val uriSuffix = targetScreen?.let { "/$it" }.orEmpty()
            intent.setAction(action).setData(Uri.parse("liveguard-widget://view/$id/${action.substringAfterLast('.')}$uriSuffix"))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            if (targetScreen != null) intent.putExtra(EXTRA_TARGET_SCREEN, targetScreen)
            if (activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            return if (activity) PendingIntent.getActivity(context, id, intent, flags) else PendingIntent.getBroadcast(context, id, intent, flags)
        }
    }
}

/**
 * Separate launcher entries. Same class of card, four jobs (2.61):
 *  - [CleanSweepWidget]        battery details, with the pack's remaining data and days beside them
 *  - [CleanSweepDataWidget]    plan details: data left, days left, today's usage
 *  - [CleanSweepPhoneWidget]   security: the last saved permission review
 *  - [CleanSweepWeatherWidget] weather: 1-min rain head-start, temperature, wind and 24×7 storm alerts
 */
class CleanSweepDataWidget : CleanSweepWidget()
class CleanSweepPhoneWidget : CleanSweepWidget()
class CleanSweepWeatherWidget : CleanSweepWidget()
