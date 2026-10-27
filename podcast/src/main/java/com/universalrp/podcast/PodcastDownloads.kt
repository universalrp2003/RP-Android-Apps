package com.universalrp.podcast

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** System-managed transfers survive closing the app. Only private app audio files are playable. */
class PodcastDownloads(private val context: Context) {
    data class Entry(val id: Long, val url: String, val title: String, val fileName: String)
    enum class Status { WAITING, DOWNLOADING, PAUSED, READY, FAILED }
    data class Download(val entry: Entry, val status: Status, val bytes: Long, val total: Long, val storedBytes: Long = 0)
    private val prefs = context.getSharedPreferences("podcast_offline", Context.MODE_PRIVATE)
    private val manager get() = context.getSystemService(DownloadManager::class.java)
    var wifiOnly: Boolean
        get() = prefs.getBoolean("wifi_only", true)
        set(value) { prefs.edit().putBoolean("wifi_only", value).apply() }

    private fun entries(): List<Entry> = runCatching {
        val array = JSONArray(prefs.getString("entries", "[]"))
        (0 until array.length()).mapNotNull { i ->
            val value = array.getJSONObject(i)
            val entry = Entry(value.getLong("id"), value.getString("url"), value.getString("title"), value.getString("file"))
            entry.takeIf { it.id >= 0 && PlaybackPolicy.isWebAudio(it.url) && OfflineAudioPolicy.safeFileName(it.fileName) }
        }
    }.getOrDefault(emptyList())

    private fun save(entries: List<Entry>) {
        val array = JSONArray()
        entries.forEach { array.put(JSONObject().put("id", it.id).put("url", it.url).put("title", it.title).put("file", it.fileName)) }
        check(prefs.edit().putString("entries", array.toString()).commit())
    }

    private fun file(entry: Entry): File? {
        if (!OfflineAudioPolicy.safeFileName(entry.fileName)) return null
        val root = context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS) ?: return null
        val directory = File(root, "offline").canonicalFile
        return File(directory, entry.fileName).canonicalFile.takeIf { it.parentFile == directory }
    }

    fun list(): List<Download> = entries().map { query(it).copy(storedBytes = file(it)?.length() ?: 0) }

    private fun query(entry: Entry): Download = runCatching {
        manager.query(DownloadManager.Query().setFilterById(entry.id))?.use { cursor ->
            if (!cursor.moveToFirst()) return@use Download(entry, Status.FAILED, file(entry)?.length() ?: 0, -1)
            fun number(column: String) = cursor.getLong(cursor.getColumnIndexOrThrow(column))
            val bytes = number(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR).coerceAtLeast(0)
            val total = number(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val status = when (number(DownloadManager.COLUMN_STATUS).toInt()) {
                DownloadManager.STATUS_PENDING -> Status.WAITING
                DownloadManager.STATUS_RUNNING -> Status.DOWNLOADING
                DownloadManager.STATUS_PAUSED -> Status.PAUSED
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val target = file(entry)
                    if (target != null && target.isFile && target.length() > 0 &&
                        target.inputStream().use { input ->
                            val prefix = ByteArray(16)
                            val count = input.read(prefix).coerceAtLeast(0)
                            OfflineAudioPolicy.looksLikeAudio(prefix.copyOf(count))
                        }) Status.READY else Status.FAILED
                }
                else -> Status.FAILED
            }
            Download(entry, status, bytes, total)
        } ?: Download(entry, Status.FAILED, 0, -1)
    }.getOrDefault(Download(entry, Status.FAILED, 0, -1))

    /** No web fallback: callers requesting offline playback receive an actual complete local file. */
    fun completedFile(url: String): File? {
        val entry = entries().firstOrNull { it.url == url } ?: return null
        return file(entry)?.takeIf { query(entry).status == Status.READY }
    }

    /** Call on an IO dispatcher, including preference commits and DownloadManager binder calls. */
    @Synchronized
    fun enqueue(url: String, title: String): Entry {
        require(PlaybackPolicy.isWebAudio(url))
        val existing = entries().firstOrNull { it.url == url }
        if (existing != null) {
            if (query(existing).status != Status.FAILED) return existing
            remove(existing)
        }
        val current = entries()
        check(current.size < OfflineAudioPolicy.MAX_ENTRIES)
        val name = "${UUID.randomUUID()}.audio"
        val pending = Entry(-1, url, title, name)
        val target = file(pending) ?: error("Offline storage is unavailable")
        check(target.parentFile?.let { it.mkdirs() || it.isDirectory } == true)
        val request = DownloadManager.Request(android.net.Uri.parse(url))
            .setTitle(title.replace('\n', ' ').take(120))
            .setDescription("கதை சொல்லி / Kadhai Solli")
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_PODCASTS, "offline/$name")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverRoaming(false)
            .setAllowedOverMetered(!wifiOnly)
            .setAllowedNetworkTypes(if (wifiOnly) DownloadManager.Request.NETWORK_WIFI else DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        val entry = pending.copy(id = manager.enqueue(request))
        try { save(current + entry) } catch (error: Exception) {
            manager.remove(entry.id)
            target.delete()
            throw error
        }
        return entry
    }

    @Synchronized
    fun remove(entry: Entry) {
        val stored = entries().firstOrNull { it.id == entry.id && it.fileName == entry.fileName } ?: return
        manager.remove(stored.id) // Cancels a running transfer and deletes its managed file.
        file(stored)?.let { check(!it.exists() || it.delete()) }
        save(entries().filterNot { it.id == stored.id })
    }
}
