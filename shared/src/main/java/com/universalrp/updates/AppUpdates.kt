package com.universalrp.updates

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Read-only release comparison with a browser handoff; never downloads or installs an APK. */
object AppUpdates {
    private val ids = mapOf(
        "CleanSweep" to "liveguard", "Ramesh-Radio" to "radio", "PulseEQ" to "pulseeq",
        "RP-Vocal-Remover-Studio" to "vocalremover", "Oor-Arivippu" to "oorarivippu",
        "SMS-Guard" to "smsguard", "Voice-Notes" to "voicenotes", "Expense-Tracker" to "expensetracker",
        "Tamil-Podcast-Player" to "podcast", "Elderly-Launcher" to "launcher", "Vasanthi-AI" to "vasanthi",
        "DailyVault" to "dailyvault", "Secret-Message" to "secretmessage",
    )

    fun appId(prefix: String): String? = ids[prefix]

    fun newer(candidate: String, installed: String): Boolean {
        fun parts(value: String): List<Int>? {
            if (!value.matches(Regex("[0-9]+(?:\\.[0-9]+){1,3}"))) return null
            return value.split('.').map { it.toIntOrNull() ?: return null }
        }
        val a = parts(candidate) ?: return false
        val b = parts(installed) ?: return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val delta = a.getOrElse(i) { 0 }.compareTo(b.getOrElse(i) { 0 })
            if (delta != 0) return delta > 0
        }
        return false
    }

    /** Only the single app-root call enables the automatic, once-per-day foreground check. */
    @Composable
    fun Control(prefix: String, automatic: Boolean = false, tamil: Boolean? = null) {
        if (!automatic) return
        val context = LocalContext.current
        val appId = appId(prefix) ?: return
        if (context.checkSelfPermission(android.Manifest.permission.INTERNET) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        if (appId == "tamilanstore" || SisterApps.all.none { it.id == appId && it.packageName == context.packageName }) return
        DailyWebsiteUpdateNotice(appId, tamil ?: (AppLanguageState.current == AppLanguage.TAMIL))
    }
}
