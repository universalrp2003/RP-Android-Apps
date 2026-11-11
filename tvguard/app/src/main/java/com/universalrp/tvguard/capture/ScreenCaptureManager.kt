package com.universalrp.tvguard.capture

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import com.universalrp.tvguard.debloat.DebloatEngine
import com.universalrp.tvguard.web.shellQuote
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

object ScreenCaptureManager {
    fun getScreenshotDir(): File {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloads, "Screenshots").apply { if (!exists()) mkdirs() }
    }

    fun listScreenshots(): List<File> {
        val screenshotDir = getScreenshotDir()
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val list = mutableListOf<File>()
        fun scan(dir: File) {
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles { file ->
                    val name = file.name.lowercase(Locale.ROOT)
                    (name.startsWith("tvguard_") || name.contains("screenshot")) &&
                        (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"))
                }?.let { list.addAll(it) }
            }
        }
        scan(screenshotDir)
        scan(downloadsDir)
        return list.distinctBy { it.name }.sortedByDescending { it.lastModified() }
    }

    /**
     * Everything the Wi-Fi hub can serve: screenshots and uploads from Downloads.
     *
     * The share screen used to list only screenshots, so an APK uploaded through the browser
     * page never appeared and could not be installed from the TV. Uploads land in the public
     * Downloads folder, so they are listed here too, newest first.
     */
    fun listHubFiles(): List<File> {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val files = mutableListOf<File>()
        fun scan(dir: File) {
            if (!dir.exists() || !dir.isDirectory) return
            dir.listFiles()?.forEach { file ->
                val name = file.name.lowercase(Locale.ROOT)
                if (!file.isFile || name.startsWith(".tvguard-upload-")) return@forEach
                val isPicture = name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                val isInstallable = name.endsWith(".apk")
                if (isPicture || isInstallable) files.add(file)
            }
        }
        scan(getScreenshotDir())
        scan(downloads)
        return files.distinctBy { it.name }.sortedByDescending { it.lastModified() }
    }

    /**
     * Owned by the caller's coroutine, not a detached background scope. Cancellation
     * interrupts the bounded root attempt and discards incomplete files; a PixelCopy
     * callback which arrives after cancellation cannot start a new save.
     */
    suspend fun captureScreen(activity: Activity): Result<File> {
        var target: File? = null
        var completed = false
        try {
            withContext(Dispatchers.IO) {
                currentCoroutineContext().ensureActive()
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                // Assign inside the IO block so prompt cancellation on returning to
                // the caller cannot lose the file before the finally block sees it.
                target = File.createTempFile("TvGuard_Screenshot_${timestamp}_", ".png", getScreenshotDir())
            }
            val file = checkNotNull(target)
            val rootResult = runInterruptible(Dispatchers.IO) {
                val path = shellQuote(file.absolutePath)
                DebloatEngine.executeRootCommand("screencap -p $path && chmod 664 $path")
            }
            currentCoroutineContext().ensureActive()
            if (!(rootResult.isSuccess && file.length() > 1024)) {
                // Keep the bitmap inside this context so it is recycled even if
                // returning from withContext is cancelled before delivery.
                withContext(Dispatchers.Main) {
                    val bitmap = captureWindowBitmap(activity)
                    try {
                        withContext(Dispatchers.IO) {
                            currentCoroutineContext().ensureActive()
                            FileOutputStream(file).use { out ->
                                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "PNG encoding failed" }
                                out.flush()
                            }
                            currentCoroutineContext().ensureActive()
                        }
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
            currentCoroutineContext().ensureActive()
            notifyMediaScanner(activity, file)
            completed = true
            return Result.success(file)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            if (!completed) target?.delete()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun captureWindowBitmap(activity: Activity): Bitmap = suspendCancellableCoroutine { continuation ->
        fun deliver(bitmap: Bitmap) {
            continuation.resume(bitmap) { bitmap.recycle() }
        }
        val view = activity.window.decorView
        val width = view.width.coerceAtLeast(1)
        val height = view.height.coerceAtLeast(1)

        fun drawFallback(): Bitmap {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            return bitmap
        }

        try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                PixelCopy.request(activity.window, bitmap, { result ->
                    if (!continuation.isActive) {
                        bitmap.recycle()
                    } else if (result == PixelCopy.SUCCESS) {
                        deliver(bitmap)
                    } else {
                        bitmap.recycle()
                        try { deliver(drawFallback()) }
                        catch (e: Exception) { continuation.resumeWithException(e) }
                    }
                }, Handler(Looper.getMainLooper()))
            } catch (e: Exception) {
                bitmap.recycle()
                if (continuation.isActive) deliver(drawFallback())
            }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    }

    fun notifyMediaScanner(context: Context, file: File) {
        try {
            MediaScannerConnection.scanFile(context.applicationContext, arrayOf(file.absolutePath), arrayOf("image/png"), null)
        } catch (_: Exception) { }
    }
}
