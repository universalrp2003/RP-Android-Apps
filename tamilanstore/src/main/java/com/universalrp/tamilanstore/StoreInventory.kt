package com.universalrp.tamilanstore
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
object StoreInventory {
    @Suppress("DEPRECATION")
    fun inspect(context: android.content.Context, app: StoreApp): InstalledApp? = try {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val info = context.packageManager.getPackageInfo(app.packageName, flags)
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        val trusted = signatures?.size == 1 && signatureHex(MessageDigest.getInstance("SHA-256").digest(signatures[0].toByteArray())) == Catalog.signerFor(app.packageName)
        InstalledApp(info.versionName, trusted)
    } catch (_: PackageManager.NameNotFoundException) { null }
      catch (_: SecurityException) { InstalledApp(null, false) }
    private fun signatureHex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
}
