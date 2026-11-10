package com.universalrp.tvguard

import com.universalrp.tvguard.web.existingHubFileName
import com.universalrp.tvguard.web.htmlEscape
import com.universalrp.tvguard.web.hubUrlComponent
import java.net.URLDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HubFileNamesTest {
    @Test
    fun namesAreDataInHtmlAndRoundTripThroughUrls() {
        val name = "தமிழ் <img src=x> & 'test' \"one\"?.apk"
        assertEquals(name, existingHubFileName(name))
        assertEquals(
            "தமிழ் &lt;img src=x&gt; &amp; &#39;test&#39; &quot;one&quot;?.apk",
            htmlEscape(name)
        )
        assertEquals(name, URLDecoder.decode(hubUrlComponent(name), "UTF-8"))
    }

    @Test
    fun existingFilesMustBeSingleFileNames() {
        listOf("", " ", ".", "..", "../secret", "folder/file", "name\u0000tail").forEach {
            assertNull(it, existingHubFileName(it))
        }
    }
}
