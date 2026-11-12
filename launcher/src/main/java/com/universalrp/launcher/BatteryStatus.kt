package com.universalrp.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Pure policy: never display a fabricated zero for an unavailable battery reading. */
fun batteryLabel(level: Int, scale: Int, charging: Boolean, full: Boolean): String {
    val percent = if (level >= 0 && scale > 0) ((level.toLong() * 100) / scale).coerceIn(0, 100).toInt() else null
    val amount = percent?.let { "$it%" } ?: "தகவல் இல்லை"
    val state = when {
        full -> "முழுமையாக நிரம்பியது"
        charging -> "மின்னேற்றம் நடைபெறுகிறது"
        else -> "மின்னேற்றம் இல்லை"
    }
    return "மின்கலம் $amount · $state"
}

@Composable
fun batteryText(): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    var label by remember { mutableStateOf("மின்கலத் தகவல் கிடைக்கவில்லை") }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                label = batteryLabel(
                    intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
                    intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1),
                    status == BatteryManager.BATTERY_STATUS_CHARGING,
                    status == BatteryManager.BATTERY_STATUS_FULL,
                )
            }
        }
        // BATTERY_CHANGED is a protected system sticky broadcast, not an app-defined event.
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let {
            receiver.onReceive(context, it)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }
    return label
}
