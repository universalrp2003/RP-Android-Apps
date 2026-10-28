package com.universalrp.cleansweep.ui

import androidx.compose.runtime.Composable
import com.universalrp.cleansweep.data.Lang
import com.universalrp.updates.StoreFirstUpdateNotice

/** Preserve existing Home/About call sites without any local APK installer. */
@Composable
fun LiveGuardUpdateCard(compact: Boolean = false) {
    StoreFirstUpdateNotice("liveguard", Lang.isTamil)
}

@Composable
fun LiveGuardUpdatePrompt() {
    StoreFirstUpdateNotice("liveguard", Lang.isTamil, showControls = false)
}
