package com.universalrp.tvguard.settings

import android.content.Context

/** Separate from launcher favorites/hidden apps; changes never clear existing data. */
class TvFeaturePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): TvFeatureSettings = TvFeatureSettings(
        expertMode = prefs.getBoolean("expert_mode", false),
        choices = TvFeature.entries.filterTo(mutableSetOf()) { prefs.getBoolean(it.key, false) }
    )

    fun save(settings: TvFeatureSettings) {
        prefs.edit().apply {
            putBoolean("expert_mode", settings.expertMode)
            TvFeature.entries.forEach { putBoolean(it.key, settings.selected(it)) }
        }.apply()
    }

    companion object { const val PREFERENCES_NAME = "tvguard_optional_features" }
}
