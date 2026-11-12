package com.universalrp.tvguard

import com.universalrp.tvguard.web.SafeUploadStorage
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SafeUploadStorageTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun simultaneousUploadsNeverReplaceAnExistingFileOrEachOther() {
        val directory = temporaryFolder.newFolder("downloads")
        val original = File(directory, "same.apk").apply { writeText("original") }
        val count = 24
        val ready = CountDownLatch(count)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(count)
        try {
            val uploads = (0 until count).map { index ->
                executor.submit<File> {
                    val temp = File.createTempFile("upload-", ".part", directory)
                    try {
                        temp.writeText("payload-$index")
                        ready.countDown()
                        check(start.await(10, TimeUnit.SECONDS))
                        SafeUploadStorage.publish(temp, directory, "same.apk")
                    } finally {
                        temp.delete()
                    }
                }
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS))
            start.countDown()
            val saved = uploads.map { it.get(10, TimeUnit.SECONDS) }
            assertEquals("original", original.readText())
            assertEquals(count, saved.map { it.name }.toSet().size)
            assertEquals((0 until count).map { "payload-$it" }.toSet(), saved.map { it.readText() }.toSet())
        } finally {
            start.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun failedCopyDoesNotLeaveAFileAtTheReservedDestination() {
        val directory = temporaryFolder.newFolder("failed")
        val missing = File(directory, "missing.part")
        try {
            SafeUploadStorage.publish(missing, directory, "result.bin")
            throw AssertionError("A missing temporary file should fail")
        } catch (_: java.io.IOException) {
            assertFalse(File(directory, "result.bin").exists())
        }
    }

    @Test
    fun interruptedPublishDoesNotCreateADestination() {
        val directory = temporaryFolder.newFolder("cancelled")
        val temp = File(directory, "upload.part").apply { writeText("payload") }
        Thread.currentThread().interrupt()
        try {
            try {
                SafeUploadStorage.publish(temp, directory, "result.bin")
                throw AssertionError("An interrupted upload should fail")
            } catch (_: InterruptedException) {
                assertFalse(File(directory, "result.bin").exists())
                assertTrue(temp.exists()) // caller is responsible for its temporary file
            }
        } finally {
            Thread.interrupted() // do not leak the interrupt into the test runner
        }
    }
}
