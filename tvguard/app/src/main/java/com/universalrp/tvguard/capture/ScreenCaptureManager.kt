package com.universalrp.tvguard.capture

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import com.universalrp.tvguard.debloat.DebloatEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScreenCaptureManager {

    fun getScreenshotDir(): File {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(downloads, "Screenshots")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
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

    private fun generateScreenshotFile(): File {
        val dir = getScreenshotDir()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "TvGuard_Screenshot_$timestamp.png")
    }

    /**
     * Attempts root capture first (screencap -p captures the whole TV screen).
     * If root is unavailable or fails, falls back to in-app window PixelCopy capture.
     */
    fun captureScreen(
        activity: Activity,
        onComplete: (Result<File>) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val targetFile = generateScreenshotFile()
            val targetPath = targetFile.absolutePath

            // 1. Root attempt (works on rooted TV boxes, Synovo Q1 PLUS, Allwinner, Rockchip)
            val rootRes = DebloatEngine.executeRootCommand("screencap -p '$targetPath' && chmod 664 '$targetPath'")
            if (rootRes.isSuccess && targetFile.exists() && targetFile.length() > 1024) {
                notifyMediaScanner(activity, targetFile)
                withContext(Dispatchers.Main) {
                    onComplete(Result.success(targetFile))
                }
                return@launch
            }

            // 2. PixelCopy window capture fallback (API 26+)
            withContext(Dispatchers.Main) {
                captureWindow(activity, activity.window, targetFile, onComplete)
            }
        }
    }

    /**
     * Delayed capture allows user to dismiss menus or view specific content before capture.
     */
    fun captureScreenDelayed(
        activity: Activity,
        delaySeconds: Int = 3,
        onTick: (Int) -> Unit = {},
        onComplete: (Result<File>) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            for (i in delaySeconds downTo 1) {
                onTick(i)
                delay(1000)
            }
            onTick(0)
            captureScreen(activity, onComplete)
        }
    }

    private fun captureWindow(
        context: Context,
        window: Window,
        targetFile: File,
        onComplete: (Result<File>) -> Unit
    ) {
        try {
            val decorView = window.decorView
            val width = decorView.width.coerceAtLeast(1280)
            val height = decorView.height.coerceAtLeast(720)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PixelCopy.request(
                    window,
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            saveBitmapToFile(context, bitmap, targetFile, onComplete)
                        } else {
                            // Fallback to Canvas draw
                            captureViaCanvas(context, window, targetFile, onComplete)
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } else {
                captureViaCanvas(context, window, targetFile, onComplete)
            }
        } catch (e: Throwable) {
            captureViaCanvas(context, window, targetFile, onComplete)
        }
    }

    private fun captureViaCanvas(
        context: Context,
        window: Window,
        targetFile: File,
        onComplete: (Result<File>) -> Unit
    ) {
        try {
            val view = window.decorView
            val width = view.width.coerceAtLeast(1280)
            val height = view.height.coerceAtLeast(720)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            saveBitmapToFile(context, bitmap, targetFile, onComplete)
        } catch (e: Throwable) {
            onComplete(Result.failure(e))
        }
    }

    private fun saveBitmapToFile(
        context: Context,
        bitmap: Bitmap,
        targetFile: File,
        onComplete: (Result<File>) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FileOutputStream(targetFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }
                notifyMediaScanner(context, targetFile)
                withContext(Dispatchers.Main) {
                    onComplete(Result.success(targetFile))
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    onComplete(Result.failure(e))
                }
            }
        }
    }

    fun notifyMediaScanner(context: Context, file: File) {
        try {
            MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(file.absolutePath),
                arrayOf("image/png"),
                null
            )
        } catch (_: Throwable) {}
    }
}
