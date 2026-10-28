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

    // The legacy CI private key is tracked publicly. Its certificate alone is not proof of origin.
    // Only exact, previously published Store bytes may receive explicit in-app handoff.
    // Future releases need a reviewed hash addition; unknown/debug/split/unreadable APKs use browser.
    private const val STORE_VERSION = "0.5"
    private const val STORE_CODE = 5L
    private const val STORE_BYTES = 16912504L
    private const val STORE_APK_SHA256 = "d5f41e7b5142507e8b42fb9d2c4e0e2d492eaf03581a53b3dabacf692daf4bc7"

    @Suppress("DEPRECATION")
    private fun matchesPublishedStore(context: Context): Boolean = runCatching {
        val flags = if (android.os.Build.VERSION.SDK_INT >= 28) android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
            else android.content.pm.PackageManager.GET_SIGNATURES
        val info = context.packageManager.getPackageInfo(STORE, flags)
        val signatures = if (android.os.Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        val signerMatches = signatures?.map { signature ->
            java.security.MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
        }?.toSet() == setOf(SIGNER)
        val code = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        if (!signerMatches || info.versionName != STORE_VERSION || code != STORE_CODE) return@runCatching false
        val path = info.applicationInfo?.publicSourceDir ?: return@runCatching false
        val apk = java.io.File(path)
        if (apk.length() != STORE_BYTES) return@runCatching false
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        apk.inputStream().buffered().use { input ->
            val buffer = ByteArray(16384)
            var total = 0L
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > STORE_BYTES) return@runCatching false
                digest.update(buffer, 0, count)
            }
            if (total != STORE_BYTES) return@runCatching false
        }
        digest.digest().joinToString("") { "%02x".format(it) } == STORE_APK_SHA256
    }.getOrDefault(false)

    fun open(context: Context, appId: String): Boolean {
        if (SisterApps.all.none { it.id == appId }) return false
        val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("tamilan://app/$appId"))
            .setComponent(ComponentName(STORE, "$STORE.MainActivity"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (matchesPublishedStore(context) && runCatching { context.startActivity(storeIntent) }.isSuccess) return true
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
