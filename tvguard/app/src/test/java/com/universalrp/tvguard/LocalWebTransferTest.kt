package com.universalrp.tvguard

import com.universalrp.tvguard.web.LocalWebTransferServer
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalWebTransferTest {

    @Test
    fun testIpDetection() {
        val ip = LocalWebTransferServer.getLocalIp()
        assertNotNull(ip)
        assertTrue(ip.isNotEmpty())
    }

    @Test
    fun testServerUrlFormat() {
        val url = LocalWebTransferServer.getServerUrl()
        assertTrue(url.startsWith("http://"))
        assertTrue(url.contains(":"))
    }
}
