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
                        projected.addYears(Int32(0))
                        assertTrue(projected.dateTime.universalTime > 0)
                        projected.setDateTime(projected.dateTime)
                        projected.setToNow()
                        assertTrue(projected.month.value > 0)
                        projected.addMonths(Int32(0))
                        projected.addWeeks(Int32(0))
                        assertTrue(projected.day.value > 0)
                        projected.addDays(Int32(0))
                        assertTrue(projected.hour.value >= 0)
                        projected.hour = projected.hour
                        projected.addHours(Int32(0))
                        assertTrue(projected.minute.value >= 0)
                        projected.minute = projected.minute
                        projected.addMinutes(Int32(0))
                        assertTrue(projected.second.value >= 0)
                        projected.second = projected.second
                        projected.addSeconds(Int32(0))
                        assertTrue(projected.nanosecond.value >= 0)
                        projected.nanosecond = projected.nanosecond
                        projected.addNanoseconds(Int32(0))
                        assertFalse(projected.numeralSystem.isBlank())
                        projected.numeralSystem = projected.numeralSystem
                        assertFalse(projected.calendarSystem.isBlank())
                        projected.changeCalendarSystem(projected.calendarSystem)
                        assertFalse(projected.clock.isBlank())
                        projected.changeClock(projected.clock)
                        assertTrue(projected.dayOfWeek.value in 0..6)
                        assertFalse(projected.resolvedLanguage.isBlank())
                        assertFalse(projected.yearAsString().isBlank())
                        assertFalse(projected.yearAsTruncatedString(Int32(2)).isBlank())
                        assertFalse(projected.monthAsString().isBlank())
                        assertFalse(projected.monthAsString(Int32(3)).isBlank())
                        assertFalse(projected.monthAsSoloString().isBlank())
                        assertFalse(projected.monthAsSoloString(Int32(3)).isBlank())
                        assertFalse(projected.monthAsNumericString().isBlank())
                        assertFalse(projected.monthAsPaddedNumericString(Int32(2)).isBlank())
                        assertFalse(projected.dayAsString().isBlank())
                        assertFalse(projected.dayAsPaddedString(Int32(2)).isBlank())
                        assertFalse(projected.dayOfWeekAsString().isBlank())
                        assertFalse(projected.yearAsPaddedString(Int32(4)).isBlank())
                        assertFalse(projected.hourAsString().isBlank())
                        assertFalse(projected.hourAsPaddedString(Int32(2)).isBlank())
                        assertFalse(projected.minuteAsString().isBlank())
                        assertFalse(projected.minuteAsPaddedString(Int32(2)).isBlank())
                        assertFalse(projected.secondAsString().isBlank())
                        assertFalse(projected.secondAsPaddedString(Int32(2)).isBlank())
                        assertFalse(projected.nanosecondAsString().isBlank())
                        assertFalse(projected.nanosecondAsPaddedString(Int32(3)).isBlank())
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
