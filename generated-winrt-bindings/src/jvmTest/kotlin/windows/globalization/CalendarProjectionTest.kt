package windows.globalization

import dev.winrt.core.Int32
import dev.winrt.kom.JvmComRuntime
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.KnownHResults
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.PlatformRuntime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class CalendarProjectionTest {
    @Test
    fun can_activate_calendar_and_read_basic_properties() {
        assumeTrue(PlatformRuntime.isWindows)

        val comResult = JvmComRuntime.initializeMultithreaded()
        val roResult = JvmWinRtRuntime.initializeMultithreaded()
        val shouldUninitializeCom = comResult.isSuccess
        val shouldUninitializeRo = roResult.isSuccess

        try {
            val factory = JvmWinRtRuntime.getActivationFactory("Windows.Globalization.Calendar").getOrThrow()
            try {
                val calendar = Calendar(JvmWinRtRuntime.activateInstance(factory).getOrThrow())
                try {
                    val projected = calendar.asICalendar()
                    try {
                        assertTrue(projected.year.value > 0)
                        projected.setYear(projected.year)
                        assertTrue(projected.month.value > 0)
                        assertTrue(projected.day.value > 0)
                        assertFalse(projected.numeralSystem.isBlank())
                        projected.numeralSystem = projected.numeralSystem
                        assertTrue(projected.dayOfWeek.value in 0..6)
                        assertFalse(projected.resolvedLanguage.isBlank())
                        assertFalse(projected.yearAsString().isBlank())
                        assertFalse(projected.yearAsPaddedString(Int32(4)).isBlank())
                        val cloned = projected.clone()
                        try {
                            assertFalse(cloned.pointer.isNull)
                        } finally {
                            PlatformComInterop.release(cloned.pointer)
                        }
                    } finally {
                        PlatformComInterop.release(projected.pointer)
                    }
                } finally {
                    PlatformComInterop.release(calendar.pointer)
                }
            } finally {
                PlatformComInterop.release(factory)
            }
        } finally {
            if (shouldUninitializeRo) {
                JvmWinRtRuntime.uninitialize()
            }
            if (shouldUninitializeCom && roResult != KnownHResults.RPC_E_CHANGED_MODE) {
                JvmComRuntime.uninitialize()
            }
        }
    }
}
