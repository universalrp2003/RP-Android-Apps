package com.universalrp.cleansweep.data

import android.app.Service
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.os.*

/** No network/storage permission: only the read-only FD supplied over Binder.
 * Native PDF parsing is isolated from the app's UID, preferences, location and API keys.
 */
class ImdPdfTextService : Service() {
    private lateinit var thread: HandlerThread
    override fun onCreate() { super.onCreate(); thread = HandlerThread("IMD PDF text").apply { start() } }
    @android.annotation.SuppressLint("NewApi")
    override fun onBind(intent: Intent?): IBinder = Messenger(Handler(thread.looper) { message ->
        val result = runCatching {
            require(Build.VERSION.SDK_INT >= 35)
            @Suppress("DEPRECATION") val fd = message.data.getParcelable<ParcelFileDescriptor>("pdf") ?: error("No PDF")
            fd.use {
                require(it.statSize in 1..2_000_000)
                PdfRenderer(it).use { pdf ->
                    require(pdf.pageCount in 1..40)
                    buildString {
                        for (i in 0 until minOf(2, pdf.pageCount)) pdf.openPage(i).use { page ->
                            for (part in page.textContents) {
                                require(length + part.text.length <= 60_000)
                                append(part.text); append(' ')
                            }
                        }
                    }
                }
            }
        }.getOrDefault("")
        runCatching { message.replyTo.send(Message.obtain().apply { data = Bundle().apply { putString("text", result) } }) }
        true
    }).binder
    override fun onUnbind(intent: Intent?): Boolean {
        // Also terminates a hung native parser after the caller's bounded timeout/unbind.
        Process.killProcess(Process.myPid())
        return false
    }
    override fun onDestroy() { thread.quitSafely(); super.onDestroy() }
}
