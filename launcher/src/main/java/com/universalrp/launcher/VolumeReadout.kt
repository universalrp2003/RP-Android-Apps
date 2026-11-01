package com.universalrp.launcher

import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Show both actual levels because adjustVolume's suggested stream is selected by Android/OEM. */
@Composable
fun volumeReadout(): String {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var levels by remember { mutableStateOf("—") }
    LaunchedEffect(context, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val audio = context.getSystemService(AudioManager::class.java)
            while (isActive) {
                fun level(stream: Int): String = runCatching {
                    if (audio == null) "—" else VolumeLevel(audio.getStreamVolume(stream), audio.getStreamMaxVolume(stream)).label()
                }.getOrDefault("—")
                levels = context.getString(R.string.volume_levels, level(AudioManager.STREAM_RING), level(AudioManager.STREAM_MUSIC))
                delay(500)
            }
        }
    }
    return levels
}
