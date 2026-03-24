package dev.winrt.kom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class Jdk22ForeignTest {
    @Test
    fun jvm_runtime_reports_ffm_backend() {
        assertEquals("jdk22-ffm", PlatformRuntime.ffiBackend)
        assertNotNull(Jdk22Foreign.linker)
    }
}
