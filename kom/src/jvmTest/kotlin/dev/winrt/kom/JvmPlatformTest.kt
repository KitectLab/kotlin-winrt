package dev.winrt.kom

import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class JvmPlatformTest {
    @Test
    fun string_arrays_are_supported_as_abi_arguments() {
        assumeTrue(PlatformRuntime.isWindows)

        assertEquals(ValueLayout.ADDRESS, methodArgumentLayout(arrayOf("alpha", "beta")))

        val preparedArguments = prepareAbiArguments(arrayOf(arrayOf("alpha", "beta")))
        try {
            assertEquals(1, preparedArguments.values.size)
            assertTrue(preparedArguments.values.single() is MemorySegment)
        } finally {
            preparedArguments.close()
        }
    }
}
