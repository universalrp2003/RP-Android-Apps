package com.universalrp.tvguard.web

import java.io.File
import java.io.FileInputStream
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.StandardOpenOption

/**
 * Publish a completed upload without replacing an existing file. Files.move without
 * REPLACE_EXISTING is not necessarily an atomic no-replace operation: two callers
 * can both observe an absent destination and then overwrite each other on rename.
 * CREATE_NEW reserves the destination atomically, including against other processes.
 * The caller owns the temporary file and must delete it on success or failure.
 */
internal object SafeUploadStorage {
    fun publish(temp: File, directory: File, filename: String): File {
        val dot = filename.lastIndexOf('.').takeIf { it > 0 } ?: filename.length
        val stem = filename.substring(0, dot)
        val extension = filename.substring(dot)
        var suffix = 0
        while (true) {
            checkInterrupted()
            val name = if (suffix == 0) filename else "${stem}_$suffix$extension"
            val destination = File(directory, name)
            val output = try {
                Files.newOutputStream(destination.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
            } catch (_: FileAlreadyExistsException) {
                suffix++
                continue
            }
            var complete = false
            try {
                output.use { out ->
                    FileInputStream(temp).use { input ->
                        val buffer = ByteArray(32_768)
                        while (true) {
                            checkInterrupted()
                            val count = input.read(buffer)
                            if (count < 0) break
                            out.write(buffer, 0, count)
                        }
                        checkInterrupted()
                    }
                }
                complete = true
                return destination
            } finally {
                if (!complete) destination.delete()
            }
        }
    }

    private fun checkInterrupted() {
        if (Thread.currentThread().isInterrupted) throw InterruptedException("Upload cancelled")
    }
}
