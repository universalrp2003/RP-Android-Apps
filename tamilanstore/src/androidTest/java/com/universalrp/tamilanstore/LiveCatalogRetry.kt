package com.universalrp.tamilanstore

import android.os.SystemClock

/**
 * A freshly booted emulator runner does not always have its route to the public site (DNS, TLS,
 * the Pages deployment) ready for the very first refresh, so a single attempt can end as "offline"
 * although nothing is wrong with the app. Tests repeat the same user-visible refresh a bounded
 * number of times before the strict "live" assertion. The app itself still makes exactly one
 * attempt per refresh and never retries on its own; this only keeps runner warm-up out of what the
 * assertion measures.
 */
internal object LiveCatalogRetry {
    const val ATTEMPTS = 5
    private const val SPACING_MS = 3_000L
    private const val REFRESH_TIMEOUT_MS = 60_000L

    /** Runs [refresh] until [model] reports a validated live catalog or the attempts run out. */
    fun awaitLive(model: StoreModel, refresh: () -> Unit) {
        for (attempt in 1..ATTEMPTS) {
            val revision = model.checkRevision
            refresh()
            // A click reaches the model asynchronously; a refresh that already finished also counts.
            val startDeadline = SystemClock.elapsedRealtime() + START_TIMEOUT_MS
            while (!model.refreshing && model.checkRevision <= revision && SystemClock.elapsedRealtime() < startDeadline) {
                Thread.sleep(100)
            }
            check(model.refreshing || model.checkRevision > revision) { "catalog refresh was not started" }
            val deadline = SystemClock.elapsedRealtime() + REFRESH_TIMEOUT_MS
            while ((model.checkRevision <= revision || model.refreshing) && SystemClock.elapsedRealtime() < deadline) {
                Thread.sleep(100)
            }
            check(!model.refreshing) { "refresh must finish within ${REFRESH_TIMEOUT_MS / 1000}s" }
            if (model.catalogMessage == "live") return
            if (attempt < ATTEMPTS) Thread.sleep(SPACING_MS)
        }
    }
    private const val START_TIMEOUT_MS = 5_000L
}
