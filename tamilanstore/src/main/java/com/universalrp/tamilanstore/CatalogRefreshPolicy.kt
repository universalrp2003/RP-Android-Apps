package com.universalrp.tamilanstore

/** Failed transport is terminal, not an active operation. Never discard a verified installer. */
internal object CatalogRefreshPolicy {
    fun allowed(refreshing: Boolean, batchActive: Boolean, busy: Boolean,
        verified: Boolean, hasTransfer: Boolean, failed: Boolean): Boolean =
        !refreshing && !batchActive && !busy && !verified && (!hasTransfer || failed)
}
