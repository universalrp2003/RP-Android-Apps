package com.universalrp.tamilnadufm

import android.content.Context
import com.universalrp.weather.WeatherPlaceCandidate

/**
 * A4 - the owner's own list of places for rain & cyclone voice alerts.
 *
 * `SharedWeatherRuntime` keeps exactly one active place, and it is shared with Live Guard and Oor
 * Arivippu, so it is deliberately left alone: this list lives in Ramesh Radio's own preferences and
 * only ever *hands* a place to the shared runtime when the owner picks one. Switching places is
 * therefore one tap, and the other two apps see the new place exactly as they always did.
 *
 * Storage is a single newline-separated string, one line per place, fields separated by [FIELD_SEP]
 * (a character a place name cannot contain). No JSON dependency is needed and a malformed line is
 * ignored rather than crashing the list.
 */
data class SavedPlace(
    val name: String,
    val details: String,
    val postalCode: String,
    val latitude: Double,
    val longitude: Double,
) {
    /** Stable identity for dedupe and removal: the same coordinates is the same place. */
    val key: String get() = java.lang.String.format(
        java.util.Locale.ROOT,
        "%.5f,%.5f",
        latitude,
        longitude,
    )

    fun toCandidate(): WeatherPlaceCandidate = WeatherPlaceCandidate(
        name = name,
        details = details,
        postalCode = postalCode,
        latitude = latitude,
        longitude = longitude,
    )

    companion object {
        fun from(candidate: WeatherPlaceCandidate): SavedPlace = SavedPlace(
            name = candidate.name,
            details = candidate.details,
            postalCode = candidate.postalCode,
            latitude = candidate.latitude,
            longitude = candidate.longitude,
        )
    }
}

object SavedPlacesStore {

    private const val PREFS = "radio_saved_places"
    private const val KEY_LIST = "places"
    private const val KEY_ACTIVE = "active_key"

    /** Field separator: a control character no place name, detail line or PIN can contain. */
    private const val FIELD_SEP = "\u0001"

    /** Most places a person keeps by hand; the picker is not a database. */
    const val MAX_SAVED_PLACES = 12

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun all(context: Context): List<SavedPlace> = decode(prefs(context).getString(KEY_LIST, "").orEmpty())

    /** The place the owner last picked, or null when nothing has been chosen. */
    fun active(context: Context): SavedPlace? {
        val key = prefs(context).getString(KEY_ACTIVE, null) ?: return null
        return all(context).firstOrNull { it.key == key }
    }

    /**
     * Adds [place] and makes it active. Re-selecting a place already in the list moves it to the
     * front rather than adding a duplicate, so the list never grows from tapping the same chip.
     */
    fun add(context: Context, place: SavedPlace): List<SavedPlace> {
        val current = all(context).toMutableList()
        current.removeAll { it.key == place.key }
        current.add(0, place)
        val trimmed = current.take(MAX_SAVED_PLACES)
        persist(context, trimmed)
        setActiveKey(context, place.key)
        return trimmed
    }

    /** Removes one place. Removing the active place leaves nothing active until a new pick. */
    fun remove(context: Context, place: SavedPlace): List<SavedPlace> {
        val trimmed = all(context).filterNot { it.key == place.key }
        persist(context, trimmed)
        if (prefs(context).getString(KEY_ACTIVE, null) == place.key) {
            prefs(context).edit().remove(KEY_ACTIVE).apply()
        }
        return trimmed
    }

    fun setActive(context: Context, place: SavedPlace): List<SavedPlace> {
        // Selecting a place that is not in the list yet adds it, so the list always contains the
        // active place.
        val list = if (all(context).any { it.key == place.key }) all(context) else add(context, place)
        setActiveKey(context, place.key)
        return list
    }

    private fun setActiveKey(context: Context, key: String) {
        prefs(context).edit().putString(KEY_ACTIVE, key).apply()
    }

    private fun persist(context: Context, list: List<SavedPlace>) {
        prefs(context).edit().putString(KEY_LIST, encode(list)).apply()
    }

    // ------------------------------------------------------ pure encode/decode (unit tested)

    /** One line per place; [FIELD_SEP] between the five fields. */
    internal fun encode(list: List<SavedPlace>): String = list.joinToString("\n") { p ->
        listOf(p.name, p.details, p.postalCode, p.latitude.toString(), p.longitude.toString())
            .joinToString(FIELD_SEP)
    }

    /**
     * Parses the stored string back. A blank line, a line with the wrong field count, a blank name
     * or a non-finite coordinate is skipped: a half-written or hand-edited value must cost the
     * owner one place, not the whole list.
     */
    internal fun decode(raw: String): List<SavedPlace> {
        if (raw.isBlank()) return emptyList()
        return raw.split('\n').mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val f = line.split(FIELD_SEP)
            if (f.size < 5) return@mapNotNull null
            val lat = f[3].toDoubleOrNull() ?: return@mapNotNull null
            val lon = f[4].toDoubleOrNull() ?: return@mapNotNull null
            if (f[0].isBlank() || !lat.isFinite() || !lon.isFinite()) return@mapNotNull null
            SavedPlace(f[0], f[1], f[2], lat, lon)
        }
    }
}
