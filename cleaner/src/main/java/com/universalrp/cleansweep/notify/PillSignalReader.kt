package com.universalrp.cleansweep.notify

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.*
import androidx.core.content.ContextCompat
import com.universalrp.cleansweep.data.SimSignalPolicy

/** Lightweight subscription-local reads. No location, cell IDs, probing, upload or new service. */
object PillSignalReader {
    fun read(context: Context): List<PillSignalPolicy.Sample> {
        if (Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return emptyList()
        return try {
            val subscriptions = context.getSystemService(SubscriptionManager::class.java)
                ?.activeSubscriptionInfoList.orEmpty()
            val base = context.getSystemService(TelephonyManager::class.java) ?: return emptyList()
            subscriptions.filter { it.simSlotIndex in 0..1 }.sortedBy { it.simSlotIndex }.map { info ->
                // Failure/revocation for one subscription must never reuse another SIM's reading.
                val measured = runCatching {
                    val tm = base.createForSubscriptionId(info.subscriptionId)
                    val candidates = tm.signalStrength?.cellSignalStrengths.orEmpty()
                    fun kind(cell: CellSignalStrength): String? = when (cell) {
                        is CellSignalStrengthNr -> "NR_SS_RSRP"
                        is CellSignalStrengthLte -> "LTE_RSRP"
                        is CellSignalStrengthWcdma -> "WCDMA_RSCP"
                        is CellSignalStrengthGsm -> "GSM_RSSI"
                        else -> null
                    }
                    val picked = candidates.sortedBy { when (kind(it)) {
                        "NR_SS_RSRP" -> 0; "LTE_RSRP" -> 1; "WCDMA_RSCP" -> 2; else -> 3
                    } }.firstOrNull { SimSignalPolicy.percent(it.dbm, kind(it)) != null }
                    PillSignalPolicy.Sample(info.simSlotIndex + 1, picked?.dbm, picked?.let { kind(it) })
                }.getOrElse { PillSignalPolicy.Sample(info.simSlotIndex + 1, null, null) }
                measured
            }
        } catch (_: Exception) { emptyList() }
    }
}
