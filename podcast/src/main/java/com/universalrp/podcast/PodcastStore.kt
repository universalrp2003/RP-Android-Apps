package com.universalrp.podcast

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * What the user has added and what they have downloaded, on disk.
 *
 * The same shape Vasanthi AI learned the hard way: read the file once into memory, keep it there,
 * and write it on a single background thread. Vasanthi AI 1.0 crashed because it re-read and
 * re-wrote the whole file for every entry on the main thread.
 */
class PodcastStore(context: Context) {

    private val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** URLs the user added, in the order they added them. */
    fun userFeedUrls(): List<String> =
        preferences.getString(KEY_FEEDS, null)
            ?.split(SEP)
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    fun addFeedUrl(url: String): Boolean {
        val clean = url.trim()
        if (!clean.startsWith("http", ignoreCase = true)) return false
        val existing = userFeedUrls()
        if (existing.any { it.equals(clean, ignoreCase = true) }) return false
        preferences.edit().putString(KEY_FEEDS, (existing + clean).joinToString(SEP)).apply()
        return true
    }

    fun removeFeedUrl(url: String) {
        val remaining = userFeedUrls().filterNot { it.equals(url, ignoreCase = true) }
        preferences.edit().putString(KEY_FEEDS, remaining.joinToString(SEP)).apply()
    }

    /** Local file names of episodes saved for offline listening. */
    fun downloads(): List<String> =
        preferences.getString(KEY_DOWNLOADS, null)
            ?.split(SEP)
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    fun addDownload(name: String) {
        if (downloads().contains(name)) return
        preferences.edit().putString(KEY_DOWNLOADS, (downloads() + name).joinToString(SEP)).apply()
    }

    fun removeDownload(name: String) {
        val remaining = downloads().filterNot { it == name }
        preferences.edit().putString(KEY_DOWNLOADS, remaining.joinToString(SEP)).apply()
    }

    /** The last episode played, so reopening the app lands where the user left off. */
    fun lastPlayed(): String? = preferences.getString(KEY_LAST, null)

    fun setLastPlayed(audioUrl: String) {
        preferences.edit().putString(KEY_LAST, audioUrl).apply()
    }

    companion object {
        private const val FILE = "podcast"
        private const val KEY_FEEDS = "feeds"
        private const val KEY_DOWNLOADS = "downloads"
        private const val KEY_LAST = "last"
        private const val SEP = "\n"

        /** Serialises a show list for the widget-free, test-only path. */
        fun encode(shows: List<PodcastShow>): String {
            val array = JSONArray()
            for (show in shows) {
                array.put(
                    JSONObject()
                        .put("id", show.id)
                        .put("title", show.title)
                        .put("url", show.feedUrl)
                        .put("builtIn", show.builtIn),
                )
            }
            return array.toString()
        }
    }
}
