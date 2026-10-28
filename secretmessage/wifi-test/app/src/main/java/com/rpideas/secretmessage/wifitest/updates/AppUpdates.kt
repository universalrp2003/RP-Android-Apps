package com.rpideas.secretmessage.wifitest.updates

import androidx.compose.runtime.Composable
import com.rpideas.secretmessage.wifitest.Tr

/** No local APK download or installation. Secret Message's own signing identity is unchanged. */
object AppUpdates {
    fun newer(candidate: String, installed: String): Boolean = com.universalrp.updates.AppUpdates.newer(candidate, installed)

    @Composable
    fun Control(prefix: String, automatic: Boolean = false) {
        if (prefix != "Secret-Message") return
        com.universalrp.updates.StoreFirstUpdateNotice("secretmessage", Tr.tamil, showControls = !automatic)
    }
}
