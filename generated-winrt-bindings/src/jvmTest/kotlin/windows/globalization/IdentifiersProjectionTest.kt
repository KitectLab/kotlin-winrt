package windows.globalization

import dev.winrt.kom.JvmComRuntime
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.KnownHResults
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.PlatformRuntime
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
import org.junit.Test

class IdentifiersProjectionTest {
    @Test
    fun can_read_calendar_and_clock_identifiers_statics() {
        assumeTrue(PlatformRuntime.isWindows)

        val comResult = JvmComRuntime.initializeMultithreaded()
        val roResult = JvmWinRtRuntime.initializeMultithreaded()
        val shouldUninitializeCom = comResult.isSuccess
        val shouldUninitializeRo = roResult.isSuccess

        try {
            val calendarStaticsPointer = JvmWinRtRuntime
                .getActivationFactory(CalendarIdentifiers.qualifiedName, ICalendarIdentifiersStatics.iid)
                .getOrThrow()
            try {
                val calendarStatics = ICalendarIdentifiersStatics(calendarStaticsPointer)
                assertFalse(calendarStatics.gregorian.isBlank())
                assertFalse(calendarStatics.hebrew.isBlank())
                assertFalse(calendarStatics.hijri.isBlank())
                assertFalse(calendarStatics.japanese.isBlank())
                assertFalse(calendarStatics.julian.isBlank())
                assertFalse(calendarStatics.korean.isBlank())
                assertFalse(calendarStatics.taiwan.isBlank())
                assertFalse(calendarStatics.thai.isBlank())
                assertFalse(calendarStatics.umAlQura.isBlank())
            } finally {
                PlatformComInterop.release(calendarStaticsPointer)
            }

            val clockStaticsPointer = JvmWinRtRuntime
                .getActivationFactory(ClockIdentifiers.qualifiedName, IClockIdentifiersStatics.iid)
                .getOrThrow()
            try {
                val clockStatics = IClockIdentifiersStatics(clockStaticsPointer)
                assertFalse(clockStatics.twelveHour.isBlank())
                assertFalse(clockStatics.twentyFourHour.isBlank())
            } finally {
                PlatformComInterop.release(clockStaticsPointer)
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
