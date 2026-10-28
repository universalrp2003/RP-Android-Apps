package com.universalrp.updates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Pilot: explicit checks and optional daily app-opening checks. No background/system notifications. */
@Composable
fun StoreFirstUpdateNotice(appId: String, tamil: Boolean, showControls: Boolean = true) {
    val context = LocalContext.current
    val app = SisterApps.all.firstOrNull { it.id == appId && it.packageName == context.packageName } ?: return
    val prefs = remember(context, appId) { context.getSharedPreferences("store-update-$appId", android.content.Context.MODE_PRIVATE) }
    var reminders by remember { mutableStateOf(prefs.getBoolean("daily_in_app", false)) }
    var requested by remember { mutableIntStateOf(0) }
    var checking by remember { mutableStateOf(false) }
    var offered by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    var showResult by remember { mutableStateOf(false) }
    fun tr(en: String, ta: String) = if (tamil) ta else en
    LaunchedEffect(requested) {
        val manual = requested > 0
        val now = System.currentTimeMillis()
        if (!manual && !UpdateReminderPolicy.shouldCheck(reminders, now, prefs.getLong("last_attempt", 0L))) return@LaunchedEffect
        // Count attempts, not just successful checks, to avoid repeated offline noise/network use.
        prefs.edit().putLong("last_attempt", now).apply()
        checking = true
        try {
            val version = try { withContext(Dispatchers.IO) {
                val connection = URL(SisterApps.OFFICIAL_WEBSITE_URL + "catalog.json").openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15000; connection.readTimeout = 15000
                try {
                    if (connection.responseCode != 200) throw java.io.IOException("Catalog HTTP ${connection.responseCode}")
                    val text = connection.inputStream.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val bytes = ByteArray(8192)
                        while (true) {
                            val count = input.read(bytes)
                            if (count < 0) break
                            check(output.size() + count <= 256 * 1024)
                            output.write(bytes, 0, count)
                        }
                        output.toString("UTF-8")
                    }
                    val root = JSONObject(text); check(root.getInt("schema") == 1)
                    val apps = root.getJSONArray("apps")
                    val matches = (0 until apps.length()).map { apps.getJSONObject(it) }.filter { it.getString("id") == appId }
                    check(matches.size == 1)
                    val entry = matches.single()
                    check(entry.getString("packageName") == context.packageName)
                    val version = entry.getString("version")
                    check(version.matches(Regex("[0-9]+(?:\\.[0-9]+){1,3}")))
                    check(entry.getString("releaseUrl") == "${SisterApps.GITHUB_REPO_URL}/releases/tag/${app.tagPrefix}-v$version")
                    version
                } finally { connection.disconnect() }
            }
            } catch (unavailable: java.io.IOException) {
                // Missing deployment/offline feed: read only canonical public release metadata.
                // Schema/identity violations do NOT fall back to a less strict source.
                OfficialReleaseVersion.latest(app)
            }
            @Suppress("DEPRECATION")
            val installed = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
            val candidate = version.takeIf { AppUpdates.newer(it, installed) }
            offered = candidate.takeIf { UpdateReminderPolicy.shouldOffer(it, prefs.getString("skipped_version", null), manual) }
            message = if (offered == null) tr("No newer stable version found", "புதிய நிலையான பதிப்பு இல்லை") else ""
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { offered = null; message = tr("Update check unavailable; try later", "புதுப்பிப்பைச் சரிபார்க்க முடியவில்லை; பிறகு முயலுங்கள்") }
        finally { checking = false; showResult = manual }
    }
    Column {
        Row {
            if (showControls) TextButton(enabled = !checking, onClick = { checking = true; offered = null; message = ""; requested++ }) {
                Text(tr(if (checking) "Checking…" else "Check updates", if (checking) "சரிபார்க்கிறது…" else "புதுப்பிப்பைச் சரிபார்"))
            }
            offered?.let { version ->
                TextButton(onClick = {
                    if (!StoreUpdateRouter.open(context, appId)) { offered = null; showResult = true; message = tr("Cannot open Store or browser", "அங்காடி அல்லது உலாவியைத் திறக்க முடியவில்லை") }
                }) { Text(tr("Update available · $version", "புதுப்பிப்பு உள்ளது · $version")) }
            }
        }
        if (showResult) AlertDialog(
            onDismissRequest = { showResult = false },
            title = { Text(tr("Update check", "புதுப்பிப்பு சரிபார்ப்பு")) },
            text = { Column {
                Text(offered?.let { tr("Update available · $it", "புதுப்பிப்பு உள்ளது · $it") } ?: message)
                TextButton(onClick = {
                    if (!StoreUpdateRouter.openWebsite(context, appId)) message = tr("Cannot open browser", "உலாவியைத் திறக்க முடியவில்லை")
                }) { Text(tr("Downloads in browser", "உலாவியில் பதிவிறக்கப் பக்கம்")) }
                TextButton(onClick = {
                    reminders = !reminders
                    prefs.edit().putBoolean("daily_in_app", reminders).apply()
                }) { Text(tr(
                    "Daily check on app open: ${if (reminders) "On" else "Off"}",
                    "செயலியைத் திறக்கும்போது தினசரி சரிபார்ப்பு: ${if (reminders) "இயக்கம்" else "நிறுத்தம்"}"
                )) }
            } },
            confirmButton = {
                offered?.let {
                    TextButton(onClick = {
                        if (StoreUpdateRouter.open(context, appId)) showResult = false
                        else { offered = null; message = tr("Cannot open Store or browser", "அங்காடி அல்லது உலாவியைத் திறக்க முடியவில்லை") }
                    }) { Text(tr("Open Store / website", "அங்காடி / இணையதளம்")) }
                } ?: TextButton(onClick = { showResult = false }) { Text(tr("Close", "மூடு")) }
            },
            dismissButton = { if (offered != null) Row {
                TextButton(onClick = { showResult = false }) { Text(tr("Later", "பிறகு")) }
                TextButton(onClick = {
                    prefs.edit().putString("skipped_version", offered).apply()
                    offered = null; showResult = false
                }) { Text(tr("Skip version", "இந்தப் பதிப்பைத் தவிர்")) }
            } }
        )
    }
}
