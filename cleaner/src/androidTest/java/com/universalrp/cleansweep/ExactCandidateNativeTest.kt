package com.universalrp.cleansweep

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Before

/** Verify the target's own APK during each test, before AGP cleans up this emulator. */
abstract class ExactCandidateNativeTest {
    @Before fun installedTargetIsTheExactArchivedCandidate() {
        val expected = InstrumentationRegistry.getArguments().getString("expectedCandidateSha256").orEmpty()
        assertTrue("CI must supply the archived candidate SHA256", expected.matches(Regex("[a-f0-9]{64}")))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.universalrp.cleansweep", context.packageName)
        val digest = MessageDigest.getInstance("SHA-256")
        File(context.applicationInfo.sourceDir).inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val size = input.read(buffer)
                if (size < 0) break
                digest.update(buffer, 0, size)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        assertEquals("Instrumented target must be the exact archived APK bytes", expected, actual)
    }
}
