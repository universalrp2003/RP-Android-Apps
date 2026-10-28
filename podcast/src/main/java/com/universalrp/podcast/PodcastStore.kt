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
        if (!PlaybackPolicy.isWebAudio(clean)) return false
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

    data class QueueEntry(val url: String, val title: String)

    var continuous: Boolean
        get() = preferences.getBoolean("continuous", true)
        set(value) { preferences.edit().putBoolean("continuous", value).apply() }
    val queueOfflineOnly: Boolean get() = preferences.getBoolean("queue_offline", false)
    fun saveQueue(entries: List<QueueEntry>, offlineOnly: Boolean) {
        val array = JSONArray()
        entries.filter { PlaybackPolicy.isWebAudio(it.url) }.distinctBy { it.url }.take(1000).forEach {
            array.put(JSONObject().put("url", it.url).put("title", it.title))
        }
        preferences.edit().putString("play_queue", array.toString()).putBoolean("queue_offline", offlineOnly).apply()
    }
    fun queue(): List<QueueEntry> = runCatching {
        val array = JSONArray(preferences.getString("play_queue", "[]"))
        (0 until array.length().coerceAtMost(1000)).map { array.getJSONObject(it) }
            .map { QueueEntry(it.getString("url"), it.getString("title")) }
            .filter { PlaybackPolicy.isWebAudio(it.url) }.distinctBy { it.url }
    }.getOrDefault(emptyList())

    data class Bookmark(val url: String, val title: String, val positionMs: Int)

    fun bookmark(): Bookmark? {
        val url = lastPlayed()?.takeIf { PlaybackPolicy.isWebAudio(it) } ?: return null
        return Bookmark(url, preferences.getString("last_title", "").orEmpty(), preferences.getInt("last_position", 0).coerceAtLeast(0))
    }

    fun saveBookmark(url: String, title: String, positionMs: Int) {
        if (!PlaybackPolicy.isWebAudio(url)) return
        preferences.edit().putString(KEY_LAST, url).putString("last_title", title)
            .putInt("last_position", positionMs.coerceAtLeast(0)).apply()
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
