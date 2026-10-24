package com.universalrp.tamilnadufm.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.universalrp.updates.AppLanguage
import com.universalrp.updates.AppLanguageState

/**
 * Keeps Ramesh Radio playing with the screen off.
 *
 * [com.universalrp.tamilnadufm.player.PlaybackService] already runs as a
 * `mediaPlayback` foreground service and holds a network wake lock, so on stock
 * Android the stream never stops when the screen turns off. Many phones, however —
 * Xiaomi/MIUI/HyperOS, realme/Oppo/Vivo and Samsung One UI in particular — kill
 * background apps with their own battery optimisation, which cuts the radio out
 * when the screen goes off. This shows a short bilingual reminder with a one-tap
 * shortcut into the app's own settings so the user can exempt it.
 *
 * It never blocks playback, it stops appearing the moment the app is no longer
 * under battery optimisation, and after "Not now" it stays quiet for seven days.
 */
object BackgroundPlayGuide {

    private const val PREFS = "background_guide"
    private const val KEY_DISMISSED_AT = "dismissed_at"
    private const val COOLDOWN_MS = 7L * 24 * 60 * 60 * 1000

    @Composable
    fun Control() {
        val context = LocalContext.current
        val prefs = remember(context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
        var exempt by remember { mutableStateOf(isExempt(context)) }
        var dismissedAt by remember { mutableStateOf(prefs.getLong(KEY_DISMISSED_AT, 0L)) }

        // Re-check whenever we come back to the app, so the prompt disappears as
        // soon as the user exempts the app from battery optimisation.
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) exempt = isExempt(context)
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        val coolingDown = dismissedAt > 0 && System.currentTimeMillis() - dismissedAt < COOLDOWN_MS
        if (exempt || coolingDown) return

        val tamil = AppLanguageState.current == AppLanguage.TAMIL
        AlertDialog(
            onDismissRequest = {
                val now = System.currentTimeMillis()
                dismissedAt = now
                prefs.edit().putLong(KEY_DISMISSED_AT, now).apply()
            },
            title = {
                Text(
                    if (tamil) "பின்னணியில் ரேடியோ தொடர்ந்து இருங்கள்"
                    else "Keep the radio playing in the background"
                )
            },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        if (tamil) "உங்கள் ஃபோனின் பேட்டரி அமைப்புகள் ஆப்-ஐ பின்னணியில் இருந்தால் நிறுத்திவிடுகின்றன; அதனால் திரை அணைக்கும்போது ரேடியோ நிற்கிறது. சரி செய்ய:"
                        else "Your phone's battery settings stop this app in the background, so the radio cuts out when the screen turns off. To fix it:"
                    )
                    Text(
                        if (tamil) "1. Autostart-ஐ ON செய்யுங்கள் (Xiaomi / realme / Oppo / Vivo)."
                        else "1. Turn Autostart ON (Xiaomi / realme / Oppo / Vivo)."
                    )
                    Text(
                        if (tamil) "2. பேட்டரியை \"No restrictions\" / \"Unrestricted\" ஆக அமைக்கவும்."
                        else "2. Set Battery to \"No restrictions\" / \"Unrestricted\"."
                    )
                    Text(
                        if (tamil) "3. Recents மெனுவில் ஆப்-ஐ 🔒 lock செய்யுங்கள்."
                        else "3. Lock the app in the Recents menu (tap the 🔒)."
                    )
                    Text(
                        if (tamil) "அமைப்புகளுக்குச் செல்ல \"அமைப்புகளைத் திற\" பட்டனைத் தட்டுங்கள்."
                        else "Tap \"Open app settings\" to jump straight there."
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { openAppSettings(context) }) {
                    Text(if (tamil) "அமைப்புகளைத் திற" else "Open app settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val now = System.currentTimeMillis()
                        dismissedAt = now
                        prefs.edit().putLong(KEY_DISMISSED_AT, now).apply()
                    },
                ) {
                    Text(if (tamil) "பிறகு" else "Not now")
                }
            },
        )
    }

    /** True when the app is already exempt from battery optimisation (or if it cannot be determined). */
    private fun isExempt(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return runCatching { pm.isIgnoringBatteryOptimizations(context.packageName) }.getOrDefault(true)
    }

    /** Opens this app's system settings page (Autostart / Battery live here on every OEM). */
    private fun openAppSettings(context: Context) {
        val opened = runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        if (opened.isFailure) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
}
