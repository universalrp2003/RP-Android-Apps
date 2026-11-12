package com.universalrp.cleansweep.data

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.telephony.*

/** No requests/prompts, callbacks, identifiers, disk cache or new poller. */
object SubscriptionSignalReader {
    fun read(context: Context, tm: TelephonyManager, activeSimCount: Int,
             reportedCells: List<CellInfo>? = null): ServingSignalPolicy.Reading? {
        val direct = if (Build.VERSION.SDK_INT >= 29) runCatching {
            tm.signalStrength?.cellSignalStrengths.orEmpty().map { it.dbm to kind(it) }
                .sortedBy { when (it.second) { "NR_SS_RSRP" -> 0; "LTE_RSRP" -> 1; "WCDMA_RSCP" -> 2; else -> 3 } }
                .firstOrNull { SimSignalPolicy.percent(it.first, it.second) != null }
        }.getOrNull() else null
        ServingSignalPolicy.resolve(direct?.first, direct?.second, emptyList(), activeSimCount)?.let { return it }
        // Existing precise-location grant + visible UI only. A floating overlay alone
        // is not permission to start background tower/location reads. Denial just hides.
        if (activeSimCount != 1 || !ForegroundSignalAccess.visible || !MobileNet.hasPreciseLocation(context)) return null
        val cells = reportedCells ?: try {
            tm.allCellInfo.orEmpty()
        } catch (_: SecurityException) {
            // The existing grant can be revoked between checking it and the modem call.
            return null
        } catch (_: Exception) {
            return null
        }
        if (!ForegroundSignalAccess.visible) return null
        val nowNanos = SystemClock.elapsedRealtimeNanos()
        val nowMs = SystemClock.elapsedRealtime()
        val candidates = cells.mapNotNull { cell ->
            if (!cell.isRegistered) return@mapNotNull null
            val measurement = when {
                Build.VERSION.SDK_INT >= 29 && cell is CellInfoNr -> cell.cellSignalStrength.dbm to "NR_SS_RSRP"
                cell is CellInfoLte -> cell.cellSignalStrength.dbm to "LTE_RSRP"
                cell is CellInfoWcdma -> cell.cellSignalStrength.dbm to "WCDMA_RSCP"
                cell is CellInfoGsm -> cell.cellSignalStrength.dbm to "GSM_RSSI"
                else -> return@mapNotNull null
            }
            @Suppress("DEPRECATION")
            val sampledAt = cell.timeStamp
            // 2.95: normalise the modem's millisecond/nanosecond convention instead of treating a
            // fresh millisecond stamp (or a missing one) as stale; the 60 s rule still applies.
            val age = ServingSignalPolicy.ageMs(sampledAt, nowNanos, nowMs)
            ServingSignalPolicy.Cell(true, measurement.first, measurement.second, age)
        }
        return ServingSignalPolicy.resolve(null, null, candidates, activeSimCount)
    }
    @androidx.annotation.RequiresApi(29)
    private fun kind(cell: CellSignalStrength): String? = when (cell) {
        is CellSignalStrengthNr -> "NR_SS_RSRP"
        is CellSignalStrengthLte -> "LTE_RSRP"
        is CellSignalStrengthWcdma -> "WCDMA_RSCP"
        is CellSignalStrengthGsm -> "GSM_RSSI"
        else -> null
    }
}
