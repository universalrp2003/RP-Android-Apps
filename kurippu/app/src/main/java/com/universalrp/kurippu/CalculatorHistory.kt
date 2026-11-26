package com.universalrp.kurippu

import android.content.SharedPreferences
import org.json.JSONArray

/** Only completed calculations are stored locally, in display order. */
class CalculatorHistory(private val preferences: SharedPreferences) {
    fun load(): List<String> = try {
        val array = JSONArray(preferences.getString(KEY, "[]"))
        (0 until array.length()).mapNotNull { array.optString(it, "").takeIf(String::isNotEmpty) }.takeLast(6)
    } catch (_: Exception) { emptyList() }

    fun save(history: List<String>): Boolean = preferences.edit()
        .putString(KEY, JSONArray(history.takeLast(6)).toString()).commit()

    private companion object { const val KEY = "calculator_history_v1" }
}
