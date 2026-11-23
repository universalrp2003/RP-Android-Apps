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
    @Test fun acceptsReuseSafeLicencesIncludingCcBy20() {
        assertEquals("CC BY 2.0", ReferencePhoto.licence("https://creativecommons.org/licenses/by/2.0")?.name)
        assertEquals("CC BY 2.0", ReferencePhoto.licence("https://creativecommons.org/licenses/by/2.0/")?.name)
        assertEquals("https://creativecommons.org/licenses/by/2.0/", ReferencePhoto.licence("https://creativecommons.org/licenses/by/2.0")?.url)
        assertEquals("CC BY 3.0", ReferencePhoto.licence("https://creativecommons.org/licenses/by/3.0/")?.name)
        assertEquals("CC BY 4.0", ReferencePhoto.licence("https://creativecommons.org/licenses/by/4.0/")?.name)
        assertEquals("CC BY-SA 3.0", ReferencePhoto.licence("https://creativecommons.org/licenses/by-sa/3.0/")?.name)
        assertEquals("CC BY-SA 4.0", ReferencePhoto.licence("https://creativecommons.org/licenses/by-sa/4.0/")?.name)
        assertNull(ReferencePhoto.licence("https://creativecommons.org/licenses/by-nc/2.0/"))
        assertNull(ReferencePhoto.licence("https://creativecommons.org/licenses/by-nd/2.0/"))
        assertNull(ReferencePhoto.licence("http://creativecommons.org/licenses/by/2.0/"))
        assertNull(ReferencePhoto.licence("https://creativecommons.org.evil.example/licenses/by/2.0/"))
        assertNull(ReferencePhoto.licence("https://creativecommons.org/licenses/by/2.0?x=1"))
        assertNull(ReferencePhoto.licence("not a URL"))
    }
    @Test fun usesTheFreePageImageEvenWhenItIsNotTheLeadImage() {
        assertEquals("Lithobates_sylvaticus_(Woodfrog).jpg", ReferencePhoto.selectFile(" Lithobates_sylvaticus_(Woodfrog).jpg "))
        assertNull(ReferencePhoto.selectFile("  "))
    }
}
