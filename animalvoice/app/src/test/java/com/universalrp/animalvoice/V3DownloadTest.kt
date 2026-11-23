package com.universalrp.animalvoice

import org.junit.Assert.*
import org.junit.Test
import java.net.URL

class V3DownloadTest {
    @Test fun onlyTrustedHttpsPublishersCanReceiveRequests() {
        assertTrue(V3Download.allowed(URL(V3Download.modelUrl)))
        assertTrue(V3Download.allowed(URL("https://bucket.s3.cern.ch/object")))
        assertFalse(V3Download.allowed(URL("http://zenodo.org/file")))
        assertFalse(V3Download.allowed(URL("https://zenodo.org.evil.example/file")))
        assertFalse(V3Download.allowed(URL("https://127.0.0.1/file")))
        assertFalse(V3Download.allowed(URL("https://zenodo.org:444/file")))
        assertFalse(V3Download.allowed(URL("https://user@zenodo.org/file")))
    }
}
