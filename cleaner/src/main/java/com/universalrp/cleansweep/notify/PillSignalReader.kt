package com.universalrp.cleansweep.notify

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.*
import androidx.core.content.ContextCompat
import com.universalrp.cleansweep.data.SimAttributionPolicy
import com.universalrp.cleansweep.data.SubscriptionSignalReader

/** Subscription-local signal; registered-cell fallback is visible-UI/previous-grant only. */
object PillSignalReader {
    fun read(context: Context): List<PillSignalPolicy.Sample> {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return emptyList()
        return try {
            val subscriptions = context.getSystemService(SubscriptionManager::class.java)
                ?.activeSubscriptionInfoList.orEmpty()
            val base = context.getSystemService(TelephonyManager::class.java) ?: return emptyList()
            // 2.95: attribute by physical slot (an OEM list that repeats one slot is still one SIM),
            // never by list length, so a single-SIM phone keeps its own measured reading.
            val activeSimCount = SimAttributionPolicy.activeSimCount(
                subscriptions.map { info -> info.simSlotIndex.takeIf { it >= 0 } },
            )
            subscriptions.filter { it.simSlotIndex in 0..1 }.sortedBy { it.simSlotIndex }.map { info ->
                // Failure/revocation for one subscription must never reuse another SIM's reading.
                val measured = runCatching {
                    val tm = base.createForSubscriptionId(info.subscriptionId)
                    val reading = SubscriptionSignalReader.read(context, tm, activeSimCount)
                    PillSignalPolicy.Sample(info.simSlotIndex + 1, reading?.dbm, reading?.kind)
                }.getOrElse { PillSignalPolicy.Sample(info.simSlotIndex + 1, null, null) }
                measured
            }
        } catch (_: Exception) { emptyList() }
    }
}
