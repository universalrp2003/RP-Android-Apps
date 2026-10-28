package com.universalrp.updates

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Only explicit user actions call this. No APK download, installer or forced Store install. */
object StoreUpdateRouter {
    private const val STORE = "com.universalrp.tamilanstore"
    private val anchors = mapOf("oorarivippu" to "oor", "vocalremover" to "vocal",
        "secretmessage" to "secret", "expensetracker" to "expense", "voicenotes" to "voice")
    fun open(context: Context, appId: String): Boolean {
        if (SisterApps.all.none { it.id == appId }) return false
        val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("tamilan://app/$appId"))
            .setComponent(ComponentName(STORE, "$STORE.MainActivity"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(storeIntent) }.isSuccess) return true
        val browser = Intent(Intent.ACTION_VIEW,
            Uri.parse(SisterApps.OFFICIAL_WEBSITE_URL + "#st-" + (anchors[appId] ?: appId)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(browser) }.isSuccess
    }
}
