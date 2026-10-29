package com.universalrp.cleansweep.ui

import androidx.compose.runtime.Composable
import com.universalrp.cleansweep.data.Lang
import com.universalrp.updates.BrowserReleaseUpdates

/** Explicit About-page handoff to the official website; no in-app APK handling. */
@Composable
fun LiveGuardUpdateCard(compact: Boolean = false) {
    BrowserReleaseUpdates("CleanSweep", tamil = Lang.isTamil)
}
