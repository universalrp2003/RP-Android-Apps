package com.universalrp.tamilanstore

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Caller must obtain this file through StoreModel.verifiedFile immediately before handoff. */
internal object InstallerHandoff {
    fun intent(context: Context, verifiedFile: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", verifiedFile)
        return Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply {
                // Propagate the read grant consistently through Android/OEM intent forwarding.
                clipData = ClipData.newRawUri("Verified APK", uri)
            }
    }
}
