package com.universalrp.updates

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Read-only, once-per-day update check when this app is opened or resumed. It only compares
 * validated public release metadata and opens the official website; it never downloads or installs
 * an APK. A successful check or an unavailable network is not a security verdict.
 */
@Composable
fun DailyWebsiteUpdateNotice(appId: String, tamil: Boolean) {
    val context = LocalContext.current
    val app = remember(appId, context.packageName) {
        SisterApps.byId(appId)?.takeIf { it.packageName == context.packageName && it.id != "tamilanstore" }
    } ?: return
    if (context.checkSelfPermission(android.Manifest.permission.INTERNET) != android.content.pm.PackageManager.PERMISSION_GRANTED) return

    val prefs = remember(context, appId) {
        context.getSharedPreferences("website-update-$appId", android.content.Context.MODE_PRIVATE)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeToken by remember { mutableIntStateOf(0) }
    var checking by remember { mutableStateOf(false) }
    var updateVersion by remember { mutableStateOf<String?>(null) }
    var openError by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeToken++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(lifecycleOwner) {
        // Wait for an actual foreground/resume event before spending the daily attempt. Keeping
        // this collector stable means a second resume cannot cancel an in-flight network check.
        snapshotFlow { resumeToken }.collect { token ->
            if (token == 0) return@collect
            val now = System.currentTimeMillis()
            val lastAttempt = prefs.getLong("last_attempt", 0L)
            if (checking || !UpdateReminderPolicy.shouldCheck(enabled = true, now = now, lastAttempt = lastAttempt)) return@collect

            // Persist before networking so repeated resumes cannot create a request loop.
            prefs.edit().putLong("last_attempt", now).apply()
            checking = true
            try {
                val latest = try {
                    withContext(Dispatchers.IO) { websiteReleaseVersion(context.packageName, app.id, app.tagPrefix) }
                } catch (unavailable: java.io.IOException) {
                    // The public GitHub Releases API is a read-only fallback if Pages is unavailable.
                    OfficialReleaseVersion.latest(app)
                }
                @Suppress("DEPRECATION")
                val installed = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: return@collect
                if (AppUpdates.newer(latest, installed)) updateVersion = latest
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A failed check is silent; the next daily open may retry. Never claim there is no update.
            } finally {
                checking = false
            }
        }
    }

    updateVersion?.let { version ->
        AlertDialog(
            onDismissRequest = { updateVersion = null; openError = false },
            title = { Text(if (tamil) "புதுப்பிப்பு உள்ளது" else "Update available") },
            text = {
                Column {
                    Text(
                        if (tamil) "புதிய பதிப்பு $version அதிகாரப்பூர்வ இணையதளத்தில் உள்ளது. உலாவியில் பதிவிறக்குங்கள்; இந்தச் செயலி APK-ஐப் பதிவிறக்கவோ நிறுவவோ செய்யாது."
                        else "Version $version is available on the official website. Download it in your browser; this app does not download or install APKs."
                    )
                    if (openError) Text(if (tamil) "உலாவியைத் திறக்க முடியவில்லை." else "Could not open the browser.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (StoreUpdateRouter.openWebsite(context, appId)) {
                        updateVersion = null
                        openError = false
                    } else {
                        openError = true
                    }
                }) { Text(if (tamil) "அதிகாரப்பூர்வ இணையதளத்தைத் திற" else "Open official website") }
            },
            dismissButton = {
                TextButton(onClick = { updateVersion = null; openError = false }) {
                    Text(if (tamil) "பிறகு" else "Later")
                }
            },
        )
    }
}

private fun websiteReleaseVersion(packageName: String, appId: String, tagPrefix: String): String {
    val connection = URL(SisterApps.OFFICIAL_WEBSITE_URL + "catalog-v2.json").openConnection() as HttpURLConnection
    connection.instanceFollowRedirects = false
    connection.requestMethod = "GET"
    connection.doOutput = false
    connection.connectTimeout = 12_000
    connection.readTimeout = 12_000
    connection.setRequestProperty("Accept", "application/json")
    connection.setRequestProperty("User-Agent", "RP-Android-Apps-read-only-update-check")
    try {
        if (connection.responseCode != HttpURLConnection.HTTP_OK) throw java.io.IOException("Catalog unavailable")
        val text = connection.inputStream.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                check(output.size() + count <= 256 * 1024) { "Catalog too large" }
                output.write(buffer, 0, count)
            }
            output.toString(Charsets.UTF_8.name())
        }
        val root = JSONObject(text)
        check(root.getInt("schema") == 1)
        val apps = root.getJSONArray("apps")
        val matches = (0 until apps.length()).map { apps.getJSONObject(it) }.filter { it.optString("id") == appId }
        check(matches.size == 1) { "App missing or duplicated in catalog" }
        val entry = matches.single()
        check(entry.getString("packageName") == packageName)
        val version = entry.getString("version")
        check(version.matches(Regex("[0-9]+(?:\\.[0-9]+){1,3}")))
        val tag = "$tagPrefix-v$version"
        check(entry.getString("releaseUrl") == "${SisterApps.GITHUB_REPO_URL}/releases/tag/$tag")
        val assetName = OfficialReleaseVersion.expectedAssetName(appId, version)
        check(entry.getString("url") == "${SisterApps.GITHUB_REPO_URL}/releases/download/$tag/$assetName")
        check(entry.getLong("size") in 1..200_000_000L)
        check(entry.getString("sha256").matches(Regex("[a-f0-9]{64}")))
        return version
    } finally {
        connection.disconnect()
    }
}
