package com.universalrp.updates

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Explicit user handoff to the official website. No app download, installer, or Store launch. */
object StoreUpdateRouter {
    private val anchors = mapOf(
        "dailyvault" to "dailyvault",
        "equalizer" to "pulseeq",
        "oorarivippu" to "oor",
        "vocalremover" to "vocal",
        "secretmessage" to "secret",
        "expensetracker" to "expense",
        "voicenotes" to "voice",
    )

    fun open(context: Context, appId: String): Boolean = openWebsite(context, appId)

    fun openWebsite(context: Context, appId: String): Boolean {
        if (SisterApps.all.none { it.id == appId && it.id != "tamilanstore" }) return false
        val browser = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(SisterApps.OFFICIAL_WEBSITE_URL + "#st-" + (anchors[appId] ?: appId)),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(browser) }.isSuccess
    }
}
