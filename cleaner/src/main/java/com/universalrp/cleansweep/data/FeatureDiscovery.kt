package com.universalrp.cleansweep.data

import android.content.Context

/** Local, compiled feature introductions. No remote flags, notification spam or permission grants. */
object FeatureDiscovery {
    const val PREFS = "feature_discovery_v1"
    enum class Mode { DEFAULT_ON_NOTICE, ASK_FIRST }
    enum class Choice { ACKNOWLEDGE, ENABLE, DISABLE, NOT_NOW }
    // Stable IDs, not version numbers: acknowledged introductions never reappear every update.
    enum class Feature(val key: String, val mode: Mode) {
        DISASTER_NEWS("disaster_news", Mode.DEFAULT_ON_NOTICE),
        SECURITY_HISTORY("security_review_changes", Mode.ASK_FIRST),
        DISASTER_NOTIFICATIONS("disaster_silent_notifications", Mode.ASK_FIRST),
        NO_SIGNAL_BEEP("no_signal_beep", Mode.ASK_FIRST)
    }
    fun defaultEnabled(mode: Mode) = mode == Mode.DEFAULT_ON_NOTICE
    fun resolve(mode: Mode, current: Boolean, choice: Choice): Boolean {
        require(if (mode == Mode.ASK_FIRST) choice in setOf(Choice.ENABLE, Choice.NOT_NOW)
            else choice in setOf(Choice.ACKNOWLEDGE, Choice.ENABLE, Choice.DISABLE))
        return when (choice) { Choice.ENABLE -> true; Choice.DISABLE -> false; else -> current }
    }
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun seen(c: Context, feature: Feature) = prefs(c).getBoolean(feature.key, false)
    fun next(c: Context): Feature? = Feature.values().firstOrNull { !seen(c, it) }
    fun enabled(c: Context, feature: Feature): Boolean = when (feature) {
        Feature.DISASTER_NEWS -> NewsDisasterUpdates.enabled(c)
        Feature.SECURITY_HISTORY -> SecurityHistoryRepo.enabled(c)
        Feature.NO_SIGNAL_BEEP -> com.universalrp.cleansweep.notify.NoSignalBeep.enabled(c)
        Feature.DISASTER_NOTIFICATIONS -> com.universalrp.cleansweep.notify.DisasterUpdateNotifier.enabled(c)
    }
    /** IO only: acknowledge durably before dismissing. Never touches the master voice/GPS/permissions. */
    fun respond(c: Context, feature: Feature, choice: Choice): Boolean {
        val current = enabled(c, feature)
        val next = resolve(feature.mode, current, choice)
        if (feature == Feature.SECURITY_HISTORY && !SecurityHistoryRepo.setEnabledDurably(c, next)) return false
        if (feature == Feature.DISASTER_NOTIFICATIONS && !com.universalrp.cleansweep.notify.DisasterUpdateNotifier.setEnabledDurably(c, next)) return false
        if (feature == Feature.NO_SIGNAL_BEEP && !com.universalrp.cleansweep.notify.NoSignalBeep.setEnabledDurably(c, next)) return false
        if (next != current) when (feature) {
            Feature.DISASTER_NEWS -> NewsDisasterUpdates.setEnabled(c, next)
            Feature.SECURITY_HISTORY, Feature.DISASTER_NOTIFICATIONS, Feature.NO_SIGNAL_BEEP -> Unit // Already committed durably above.
        }
        return prefs(c).edit().putBoolean(feature.key, true).commit()
    }
}
