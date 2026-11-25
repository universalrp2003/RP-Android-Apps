package com.universalrp.cleansweep.data

/** Android dataBytes already contains cacheBytes; neither cache nor the APK is added twice. */
internal object AppStorageSizePolicy {
    fun valid(codeBytes: Long, dataBytes: Long, cacheBytes: Long): Boolean =
        codeBytes >= 0 && dataBytes >= 0 && cacheBytes >= 0 && cacheBytes <= dataBytes &&
            codeBytes <= Long.MAX_VALUE - dataBytes

    fun size(codeBytes: Long?, dataBytes: Long, cacheBytes: Long, apkBytes: Long): Long? =
        if (codeBytes != null && valid(codeBytes, dataBytes, cacheBytes)) codeBytes + dataBytes
        else apkBytes.takeIf { it > 0 }
}
