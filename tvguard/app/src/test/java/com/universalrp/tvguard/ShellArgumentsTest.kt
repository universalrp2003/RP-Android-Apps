package com.universalrp.tvguard

import com.universalrp.tvguard.web.shellQuote
import org.junit.Assert.assertEquals
import org.junit.Test

class ShellArgumentsTest {
    @Test fun browserTextIsQuotedAsOneShellArgument() {
        assertEquals("'hello'", shellQuote("hello"))
        assertEquals("'can'\\''t'", shellQuote("can't"))
        assertEquals("'\$(echo danger); `id`'", shellQuote("\$(echo danger); `id`"))
        assertEquals("''", shellQuote(""))
    }
}
