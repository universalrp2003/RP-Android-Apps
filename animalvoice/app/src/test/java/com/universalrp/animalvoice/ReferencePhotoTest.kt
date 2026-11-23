package com.universalrp.animalvoice

import java.net.URL
import org.junit.Assert.*
import org.junit.Test

class ReferencePhotoTest {
    @Test fun redirectsOnlyAllowWikimediaHttps() {
        assertTrue(ReferencePhoto.allowed(URL("https://upload.wikimedia.org/test.jpg")))
        assertTrue(ReferencePhoto.allowed(URL("https://thumb.wikimedia.org/test.jpg")))
        assertFalse(ReferencePhoto.allowed(URL("http://upload.wikimedia.org/test.jpg")))
        assertFalse(ReferencePhoto.allowed(URL("https://upload.wikimedia.org.evil.example/a")))
        assertFalse(ReferencePhoto.allowed(URL("https://evil@upload.wikimedia.org/a")))
        assertFalse(ReferencePhoto.allowed(URL("https://upload.wikimedia.org:444/a")))
    }
}
