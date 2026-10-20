package com.universalrp.cleansweep.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.universalrp.cleansweep.MainViewModel
import com.universalrp.cleansweep.data.Lang
import com.universalrp.cleansweep.ui.theme.AccentCyan
import com.universalrp.cleansweep.ui.theme.SurfaceHigh
import com.universalrp.cleansweep.ui.theme.TextPrimary
import com.universalrp.cleansweep.ui.theme.TextSecondary

/**
 * 2.53 — the guide, as its own screen.
 *
 * The owner tapped **Guide** in the side menu and landed on *Reviewed feature setup* — a permission
 * checklist. That row was wired to `Screen.FEATURE_SETUP`, which is a different thing entirely:
 * one page says what Live Guard may do, the other explains how to use it. They are now two rows,
 * and this is the real guide — the same expandable Tamil + English text the About screen already
 * shows, with a back arrow that returns to wherever the user came from.
 */
@Composable
fun LiveGuardGuideScreen(vm: MainViewModel) {
    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        com.universalrp.updates.GuideScreen(
            content = com.universalrp.updates.AppGuides.liveGuard,
            tamil = Lang.isTamil,
            onBack = { vm.goBack() },
            accent = AccentCyan,
            surface = SurfaceHigh,
            contentColor = TextPrimary,
            mutedColor = TextSecondary,
        )
    }
}
