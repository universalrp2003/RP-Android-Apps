package com.universalrp.weeklymoney

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Public Downloads on Android 10+, persistent user-selected SAF folder on Android 8/9. */
class AutomaticBackup(private val context: Context) {
    fun write(encrypted: ByteArray, reason: String): Uri {
        val name = "WeeklyMoney-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}-$reason-${UUID.randomUUID().toString().take(8)}.vvasool"
        val resolver = context.contentResolver
        val uri = if (Build.VERSION.SDK_INT >= 29) {
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/WeeklyMoney")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }) ?: error("Cannot create Downloads backup")
        } else {
            val value = context.getSharedPreferences("preferences", Context.MODE_PRIVATE).getString("backupTree", null)
                ?: throw InputProblem("Settings → Backup folder: Downloads-ல் ஒரு folder தேர்ந்தெடுக்கவும் / Choose an external backup folder first")
            val tree = Uri.parse(value)
            DocumentsContract.createDocument(resolver,
                DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree)),
                "application/octet-stream", name) ?: error("Cannot create folder backup")
        }
        try {
            requireNotNull(resolver.openOutputStream(uri, "w")).use { it.write(encrypted); it.flush() }
            requireNotNull(resolver.openInputStream(uri)).use { input ->
                val buffer = ByteArray(8192)
                var offset = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    check(offset + count <= encrypted.size)
                    for (i in 0 until count) check(buffer[i] == encrypted[offset + i])
                    offset += count
                }
                check(offset == encrypted.size)
            }
            if (Build.VERSION.SDK_INT >= 29) check(resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null) == 1)
            return uri
        } catch (e: Exception) {
            runCatching { if (Build.VERSION.SDK_INT >= 29) resolver.delete(uri, null, null) else DocumentsContract.deleteDocument(resolver, uri) }
            throw e
        }
    }
}
