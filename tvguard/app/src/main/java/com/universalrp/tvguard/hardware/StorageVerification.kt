package com.universalrp.tvguard.hardware

object StorageVerification {
    /** A warning, not proof: the candidate disk may be removable or not back /data. */
    fun isSizeMismatch(candidateBytes: Long?, dataBytes: Long): Boolean {
        if (candidateBytes == null || candidateBytes <= 0 || dataBytes <= candidateBytes) return false
        return dataBytes - candidateBytes > candidateBytes / 10
    }
}
