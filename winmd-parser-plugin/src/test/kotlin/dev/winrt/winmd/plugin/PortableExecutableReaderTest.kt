package dev.winrt.winmd.plugin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class PortableExecutableReaderTest {
    @Test
    fun mz_header_is_detected() {
        val file = Files.createTempFile("sample", ".winmd")
        Files.write(file, byteArrayOf('M'.code.toByte(), 'Z'.code.toByte(), 0x01))

        val info = PortableExecutableReader.inspect(file)

        assertTrue(info.isPortableExecutable)
    }

    @Test
    fun random_header_is_rejected() {
        val file = Files.createTempFile("sample", ".winmd")
        Files.write(file, byteArrayOf(0x01, 0x02, 0x03))

        val info = PortableExecutableReader.inspect(file)

        assertFalse(info.isPortableExecutable)
    }
}
