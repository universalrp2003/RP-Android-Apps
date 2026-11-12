package com.universalrp.tamilanstore

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** One explicit user-started transfer. Android owns transport, never installation. */
internal class ManagedApkDownload(private val context: Context) {
    private val manager = checkNotNull(context.getSystemService(DownloadManager::class.java))
    private val prefs = context.getSharedPreferences("active_download_v1", Context.MODE_PRIVATE)
    data class Saved(val app: StoreApp, val id: Long, val fileName: String)
    data class Progress(val status: Int, val bytes: Long)

    fun restore(): Saved? = runCatching {
        val text = prefs.getString("catalog", null) ?: return null
        val app = Catalog.parse(text).single { it.id == prefs.getString("app", null) }
        val name = prefs.getString("file", null) ?: return null
        require(name.matches(Regex("[a-f0-9-]+\\.transfer")))
        Saved(app, prefs.getLong("id", -1), name)
    }.getOrNull()

    fun begin(app: StoreApp, catalog: List<StoreApp>): Saved {
        cancel()
        require(app in catalog)
        val entries = JSONArray()
        catalog.forEach { a -> entries.put(JSONObject().put("id", a.id).put("name", a.name)
            .put("tamilName", a.tamilName).put("packageName", a.packageName).put("version", a.version)
            .put("url", a.url).put("size", a.size).put("sha256", a.sha256).put("releaseUrl", a.releaseUrl)) }
        val snapshot = JSONObject().put("schema", 1).put("apps", entries).toString()
        require(Catalog.parse(snapshot).contains(app))
        val name = UUID.randomUUID().toString() + ".transfer"
        val request = DownloadManager.Request(Uri.parse(app.url))
            .setTitle(app.name)
            .setDescription("Tamilan Store · user-requested download")
            .setMimeType("application/octet-stream")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, name)
        val id = manager.enqueue(request)
        if (!prefs.edit().putString("catalog", snapshot).putString("app", app.id)
                .putString("file", name).putLong("id", id).commit()) {
            manager.remove(id)
            error("Could not save download state")
        }
        return Saved(app, id, name)
    }

    fun progress(saved: Saved): Progress {
        manager.query(DownloadManager.Query().setFilterById(saved.id)).use { cursor ->
            check(cursor != null && cursor.moveToFirst()) { "System download no longer exists" }
            return Progress(cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)))
        }
    }

    fun file(saved: Saved): File = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        ?: error("Download storage unavailable"), saved.fileName)

    // Read the system-owned completed request through its provider descriptor, not a
    // second external-storage/FUSE path view. The caller still checks size/hash/signer.
    fun openCompleted(saved: Saved): java.io.InputStream =
        android.os.ParcelFileDescriptor.AutoCloseInputStream(manager.openDownloadedFile(saved.id))

    fun completed(saved: Saved) {
        // First persist recovery of the private verified copy, then discard untrusted transport.
        check(prefs.edit().putLong("id", -1).commit())
        manager.remove(saved.id)
        file(saved).delete()
    }

    fun cancel() {
        restore()?.let { saved ->
            if (saved.id >= 0) manager.remove(saved.id)
            file(saved).delete()
        }
        check(prefs.edit().clear().commit())
    }
}
