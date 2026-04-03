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
                    assertTrue(calendar.year.value > 0)
                    val languages = calendar.languages
                        try {
                            assertTrue(languages.size > 0)
                            assertFalse(languages[0].isBlank())
                        } finally {
                            PlatformComInterop.release(languages.pointer)
                        }
                        calendar.setYear(calendar.year)
                        calendar.addYears(Int32(0))
                        assertTrue(calendar.firstEra.value >= 0)
                        assertTrue(calendar.lastEra.value >= calendar.firstEra.value)
                        assertTrue(calendar.numberOfEras.value > 0)
                        calendar.era = calendar.era
                        calendar.addEras(Int32(0))
                        assertFalse(calendar.eraAsString().isBlank())
                        assertFalse(calendar.eraAsString(Int32(3)).isBlank())
                        assertTrue(calendar.firstYearInThisEra.value > 0)
                        assertTrue(calendar.lastYearInThisEra.value >= calendar.firstYearInThisEra.value)
                        assertTrue(calendar.numberOfYearsInThisEra.value > 0)
                        assertTrue(calendar.dateTime.toEpochMilliseconds() > 0)
                        calendar.setDateTime(calendar.dateTime)
                        calendar.setToNow()
                        assertTrue(calendar.firstMonthInThisYear.value > 0)
                        assertTrue(calendar.lastMonthInThisYear.value >= calendar.firstMonthInThisYear.value)
                        assertTrue(calendar.numberOfMonthsInThisYear.value > 0)
                        assertTrue(calendar.month.value > 0)
                        calendar.addMonths(Int32(0))
                        calendar.addWeeks(Int32(0))
                        assertTrue(calendar.firstDayInThisMonth.value > 0)
                        assertTrue(calendar.lastDayInThisMonth.value >= calendar.firstDayInThisMonth.value)
                        assertTrue(calendar.numberOfDaysInThisMonth.value > 0)
                        assertTrue(calendar.day.value > 0)
                        calendar.addDays(Int32(0))
                        assertTrue(calendar.firstPeriodInThisDay.value >= 0)
                        assertTrue(calendar.lastPeriodInThisDay.value >= calendar.firstPeriodInThisDay.value)
                        assertTrue(calendar.numberOfPeriodsInThisDay.value > 0)
                        calendar.period = calendar.period
                        calendar.addPeriods(Int32(0))
                        assertTrue(calendar.hour.value >= 0)
                        assertTrue(calendar.firstHourInThisPeriod.value >= 0)
                        assertTrue(calendar.lastHourInThisPeriod.value >= calendar.firstHourInThisPeriod.value)
                        assertTrue(calendar.numberOfHoursInThisPeriod.value > 0)
                        calendar.hour = calendar.hour
                        calendar.addHours(Int32(0))
                        assertTrue(calendar.minute.value >= 0)
                        assertTrue(calendar.firstMinuteInThisHour.value >= 0)
                        assertTrue(calendar.lastMinuteInThisHour.value >= calendar.firstMinuteInThisHour.value)
                        assertTrue(calendar.numberOfMinutesInThisHour.value > 0)
                        calendar.minute = calendar.minute
                        calendar.addMinutes(Int32(0))
                        assertTrue(calendar.second.value >= 0)
                        assertTrue(calendar.firstSecondInThisMinute.value >= 0)
                        assertTrue(calendar.lastSecondInThisMinute.value >= calendar.firstSecondInThisMinute.value)
                        assertTrue(calendar.numberOfSecondsInThisMinute.value > 0)
                        calendar.second = calendar.second
                        calendar.addSeconds(Int32(0))
                        assertTrue(calendar.nanosecond.value >= 0)
                        calendar.nanosecond = calendar.nanosecond
                        calendar.addNanoseconds(Int32(0))
                        assertFalse(calendar.numeralSystem.isBlank())
                        calendar.numeralSystem = calendar.numeralSystem
                        assertFalse(calendar.calendarSystem.isBlank())
                        calendar.changeCalendarSystem(calendar.calendarSystem)
                        assertFalse(calendar.clock.isBlank())
                        calendar.changeClock(calendar.clock)
                        assertTrue(calendar.dayOfWeek.value in 0..6)
                        assertFalse(calendar.resolvedLanguage.isBlank())
                        assertFalse(calendar.yearAsString().isBlank())
                        assertFalse(calendar.yearAsTruncatedString(Int32(2)).isBlank())
                        assertFalse(calendar.monthAsString().isBlank())
                        assertFalse(calendar.monthAsString(Int32(3)).isBlank())
                        assertFalse(calendar.monthAsSoloString().isBlank())
                        assertFalse(calendar.monthAsSoloString(Int32(3)).isBlank())
                        assertFalse(calendar.monthAsNumericString().isBlank())
                        assertFalse(calendar.monthAsPaddedNumericString(Int32(2)).isBlank())
                        assertFalse(calendar.dayAsString().isBlank())
                        assertFalse(calendar.dayAsPaddedString(Int32(2)).isBlank())
                        assertFalse(calendar.dayOfWeekAsString().isBlank())
                        assertFalse(calendar.dayOfWeekAsString(Int32(3)).isBlank())
                        assertFalse(calendar.dayOfWeekAsSoloString().isBlank())
                        assertFalse(calendar.dayOfWeekAsSoloString(Int32(3)).isBlank())
                        calendar.periodAsString()
                        calendar.periodAsString(Int32(2))
                        assertFalse(calendar.yearAsPaddedString(Int32(4)).isBlank())
                        assertFalse(calendar.hourAsString().isBlank())
                        assertFalse(calendar.hourAsPaddedString(Int32(2)).isBlank())
                        assertFalse(calendar.minuteAsString().isBlank())
                        assertFalse(calendar.minuteAsPaddedString(Int32(2)).isBlank())
                        assertFalse(calendar.secondAsString().isBlank())
                        assertFalse(calendar.secondAsPaddedString(Int32(2)).isBlank())
                        assertFalse(calendar.nanosecondAsString().isBlank())
                        assertFalse(calendar.nanosecondAsPaddedString(Int32(3)).isBlank())
                        val cloned = calendar.clone()
                        try {
                            assertFalse(cloned.pointer.isNull)
                        } finally {
                            PlatformComInterop.release(cloned.pointer)
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
