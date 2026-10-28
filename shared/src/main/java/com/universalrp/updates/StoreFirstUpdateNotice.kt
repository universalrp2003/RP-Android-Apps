package com.universalrp.updates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Pilot: user-triggered in-app notice only. No background/system notifications yet. */
@Composable
fun StoreFirstUpdateNotice(appId: String, tamil: Boolean) {
    val context = LocalContext.current
    val app = SisterApps.all.firstOrNull { it.id == appId && it.packageName == context.packageName } ?: return
    var requested by remember { mutableIntStateOf(0) }
    var checking by remember { mutableStateOf(false) }
    var offered by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    fun tr(en: String, ta: String) = if (tamil) ta else en
    LaunchedEffect(requested) {
        if (requested == 0) return@LaunchedEffect
        checking = true
        try {
            val version = withContext(Dispatchers.IO) {
                val connection = URL(SisterApps.OFFICIAL_WEBSITE_URL + "catalog.json").openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15000; connection.readTimeout = 15000
                try {
                    check(connection.responseCode == 200)
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
            @Suppress("DEPRECATION")
            val installed = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
            offered = version.takeIf { AppUpdates.newer(it, installed) }
            message = if (offered == null) tr("No newer stable version found", "புதிய நிலையான பதிப்பு இல்லை") else ""
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { offered = null; message = tr("Update check unavailable; try later", "புதுப்பிப்பைச் சரிபார்க்க முடியவில்லை; பிறகு முயலுங்கள்") }
        finally { checking = false }
    }
    Column {
        Row {
            TextButton(enabled = !checking, onClick = { requested++ }) {
                Text(tr(if (checking) "Checking…" else "Check updates", if (checking) "சரிபார்க்கிறது…" else "புதுப்பிப்பைச் சரிபார்"))
            }
            offered?.let { version ->
                TextButton(onClick = {
                    if (!StoreUpdateRouter.open(context, appId)) message = tr("Cannot open Store or browser", "அங்காடி அல்லது உலாவியைத் திறக்க முடியவில்லை")
                }) { Text(tr("Update available · $version", "புதுப்பிப்பு உள்ளது · $version")) }
            }
        }
        if (message.isNotBlank()) Text(message)
    }
}
