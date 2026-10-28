package com.universalrp.updates

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Only explicit user actions call this. No APK download, installer or forced Store install. */
object StoreUpdateRouter {
    private const val STORE = "com.universalrp.tamilanstore"
    private val anchors = mapOf("equalizer" to "pulseeq", "oorarivippu" to "oor", "vocalremover" to "vocal",
        "secretmessage" to "secret", "expensetracker" to "expense", "voicenotes" to "voice")
    private const val SIGNER = "9e30f4eab8afce5e4ab444ff9578fec354d6d1414d245253590a9703d5985d55"

    @Suppress("DEPRECATION")
    private fun trustedStore(context: Context): Boolean = runCatching {
        val flags = if (android.os.Build.VERSION.SDK_INT >= 28) android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
            else android.content.pm.PackageManager.GET_SIGNATURES
        val info = context.packageManager.getPackageInfo(STORE, flags)
        val signatures = if (android.os.Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        signatures?.map { signature ->
            java.security.MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
        }?.toSet() == setOf(SIGNER)
    }.getOrDefault(false)

    fun open(context: Context, appId: String): Boolean {
        if (SisterApps.all.none { it.id == appId }) return false
        val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("tamilan://app/$appId"))
            .setComponent(ComponentName(STORE, "$STORE.MainActivity"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (trustedStore(context) && runCatching { context.startActivity(storeIntent) }.isSuccess) return true
        return openWebsite(context, appId)
    }

    fun openWebsite(context: Context, appId: String): Boolean {
        if (SisterApps.all.none { it.id == appId }) return false
        val browser = Intent(Intent.ACTION_VIEW,
            Uri.parse(SisterApps.OFFICIAL_WEBSITE_URL + "#st-" + (anchors[appId] ?: appId)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(browser) }.isSuccess
    }
}
