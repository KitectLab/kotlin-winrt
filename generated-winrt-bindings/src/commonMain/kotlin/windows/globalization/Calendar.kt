package windows.globalization

import dev.winrt.core.Inspectable
import dev.winrt.core.Int32
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import kotlin.collections.Iterable
import kotlin.time.Instant
import windows.foundation.collections.IVectorView

public open class Calendar(
  pointer: ComPtr,
) : Inspectable(pointer),
    ITimeZoneOnCalendar {
  private val backing_Day: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public var day: Int32
    get() {
      if (pointer.isNull) {
        return backing_Day.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 52).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Day.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 53, value.value).getOrThrow()
    }

  private val backing_DayOfWeek: RuntimeProperty<DayOfWeek> =
      RuntimeProperty<DayOfWeek>(DayOfWeek.fromValue(0))

  public val dayOfWeek: DayOfWeek
    get() {
      if (pointer.isNull) {
        return backing_DayOfWeek.get()
      }
      return DayOfWeek(PlatformComInterop.invokeObjectMethod(pointer, 57).getOrThrow())
    }

  private val backing_Era: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public var era: Int32
    get() {
      if (pointer.isNull) {
        return backing_Era.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 22).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Era.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 23, value.value).getOrThrow()
    }

  private val backing_FirstDayInThisMonth: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public val firstDayInThisMonth: Int32
    get() {
      if (pointer.isNull) {
        return backing_FirstDayInThisMonth.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 49).getOrThrow())
    }

  private val backing_FirstEra: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public val firstEra: Int32
    get() {
      if (pointer.isNull) {
        return backing_FirstEra.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 19).getOrThrow())
    }

  private val backing_FirstHourInThisPeriod: RuntimeProperty<Int32> =
      RuntimeProperty<Int32>(Int32(0))

  public val firstHourInThisPeriod: Int32
    get() {
      if (pointer.isNull) {
        return backing_FirstHourInThisPeriod.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 70).getOrThrow())
    }

  private val backing_FirstMinuteInThisHour: RuntimeProperty<Int32> =
      RuntimeProperty<Int32>(Int32(0))

  public val firstMinuteInThisHour: Int32
    get() {
      if (pointer.isNull) {
        return backing_FirstMinuteInThisHour.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 96).getOrThrow())
    }

  private val backing_FirstMonthInThisYear: RuntimeProperty<Int32> =
      RuntimeProperty<Int32>(Int32(0))

  public val firstMonthInThisYear: Int32
    get() {
      if (pointer.isNull) {
        return backing_FirstMonthInThisYear.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 36).getOrThrow())
    }

  private val backing_FirstPeriodInThisDay: RuntimeProperty<Int32> =
      RuntimeProperty<Int32>(Int32(0))

  public val firstPeriodInThisDay: Int32
    get() {
      if (pointer.isNull) {
        return backing_FirstPeriodInThisDay.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 62).getOrThrow())
    }

  private val backing_FirstSecondInThisMinute: RuntimeProperty<Int32> =
      RuntimeProperty<Int32>(Int32(0))

  public val firstSecondInThisMinute: Int32
    get() {
      if (pointer.isNull) {
        return backing_FirstSecondInThisMinute.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 99).getOrThrow())
    }

  private val backing_FirstYearInThisEra: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public val firstYearInThisEra: Int32
    get() {
      if (pointer.isNull) {
        return backing_FirstYearInThisEra.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 27).getOrThrow())
    }

  private val backing_Hour: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public var hour: Int32
    get() {
      if (pointer.isNull) {
        return backing_Hour.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 73).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Hour.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 74, value.value).getOrThrow()
    }

  private val backing_IsDaylightSavingTime: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  public val isDaylightSavingTime: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsDaylightSavingTime.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 103).getOrThrow())
    }

  private val backing_LastDayInThisMonth: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public val lastDayInThisMonth: Int32
    get() {
      if (pointer.isNull) {
        return backing_LastDayInThisMonth.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 50).getOrThrow())
    }

  private val backing_LastEra: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public val lastEra: Int32
    get() {
      if (pointer.isNull) {
        return backing_LastEra.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 20).getOrThrow())
    }

  private val backing_LastHourInThisPeriod: RuntimeProperty<Int32> =
      RuntimeProperty<Int32>(Int32(0))

  public val lastHourInThisPeriod: Int32
    get() {
      if (pointer.isNull) {
        return backing_LastHourInThisPeriod.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 71).getOrThrow())
    }

  private val backing_LastMinuteInThisHour: RuntimeProperty<Int32> =
      RuntimeProperty<Int32>(Int32(0))

  public val lastMinuteInThisHour: Int32
    get() {
      if (pointer.isNull) {
        return backing_LastMinuteInThisHour.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 97).getOrThrow())
    }

  private val backing_LastMonthInThisYear: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public val lastMonthInThisYear: Int32
    get() {
      if (pointer.isNull) {
        return backing_LastMonthInThisYear.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 37).getOrThrow())
    }

  private val backing_LastPeriodInThisDay: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public val lastPeriodInThisDay: Int32
    get() {
      if (pointer.isNull) {
        return backing_LastPeriodInThisDay.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 63).getOrThrow())
    }

  private val backing_LastSecondInThisMinute: RuntimeProperty<Int32> =
      RuntimeProperty<Int32>(Int32(0))

  public val lastSecondInThisMinute: Int32
    get() {
      if (pointer.isNull) {
        return backing_LastSecondInThisMinute.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 100).getOrThrow())
    }

  private val backing_LastYearInThisEra: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public val lastYearInThisEra: Int32
    get() {
      if (pointer.isNull) {
        return backing_LastYearInThisEra.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 28).getOrThrow())
    }

  private val backing_Minute: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public var minute: Int32
    get() {
      if (pointer.isNull) {
        return backing_Minute.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 78).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Minute.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 79, value.value).getOrThrow()
    }

  private val backing_Month: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public var month: Int32
    get() {
      if (pointer.isNull) {
        return backing_Month.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 39).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Month.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 40, value.value).getOrThrow()
    }

  private val backing_Nanosecond: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public var nanosecond: Int32
    get() {
      if (pointer.isNull) {
        return backing_Nanosecond.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 88).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Nanosecond.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 89, value.value).getOrThrow()
    }

  private val backing_NumberOfDaysInThisMonth: RuntimeProperty<Int32> =
      RuntimeProperty<Int32>(Int32(0))

  public val numberOfDaysInThisMonth: Int32
    get() {
      if (pointer.isNull) {
        return backing_NumberOfDaysInThisMonth.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 51).getOrThrow())
    }

  private val backing_NumberOfEras: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public val numberOfEras: Int32
    get() {
      if (pointer.isNull) {
        return backing_NumberOfEras.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 21).getOrThrow())
    }

  private val backing_NumberOfHoursInThisPeriod: RuntimeProperty<Int32> =
      RuntimeProperty<Int32>(Int32(0))

  public val numberOfHoursInThisPeriod: Int32
    get() {
      if (pointer.isNull) {
        return backing_NumberOfHoursInThisPeriod.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 72).getOrThrow())
    }

  private val backing_NumberOfMinutesInThisHour: RuntimeProperty<Int32> =
      RuntimeProperty<Int32>(Int32(0))

  public val numberOfMinutesInThisHour: Int32
    get() {
      if (pointer.isNull) {
        return backing_NumberOfMinutesInThisHour.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 98).getOrThrow())
    }

  private val backing_NumberOfMonthsInThisYear: RuntimeProperty<Int32> =
      RuntimeProperty<Int32>(Int32(0))

  public val numberOfMonthsInThisYear: Int32
    get() {
      if (pointer.isNull) {
        return backing_NumberOfMonthsInThisYear.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 38).getOrThrow())
    }

  private val backing_NumberOfPeriodsInThisDay: RuntimeProperty<Int32> =
      RuntimeProperty<Int32>(Int32(0))

  public val numberOfPeriodsInThisDay: Int32
    get() {
      if (pointer.isNull) {
        return backing_NumberOfPeriodsInThisDay.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 64).getOrThrow())
    }

  private val backing_NumberOfSecondsInThisMinute: RuntimeProperty<Int32> =
      RuntimeProperty<Int32>(Int32(0))

  public val numberOfSecondsInThisMinute: Int32
    get() {
      if (pointer.isNull) {
        return backing_NumberOfSecondsInThisMinute.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 101).getOrThrow())
    }

  private val backing_NumberOfYearsInThisEra: RuntimeProperty<Int32> =
      RuntimeProperty<Int32>(Int32(0))

  public val numberOfYearsInThisEra: Int32
    get() {
      if (pointer.isNull) {
        return backing_NumberOfYearsInThisEra.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 29).getOrThrow())
    }

  private val backing_NumeralSystem: RuntimeProperty<String> = RuntimeProperty<String>("")

  public var numeralSystem: String
    get() {
      if (pointer.isNull) {
        return backing_NumeralSystem.get()
      }
      return PlatformComInterop.invokeHStringMethod(pointer, 10).getOrThrow().use {
          it.toKotlinString() }
    }
    set(value) {
      if (pointer.isNull) {
        backing_NumeralSystem.set(value)
        return
      }
      PlatformComInterop.invokeStringSetter(pointer, 11, value).getOrThrow()
    }

  private val backing_Period: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public var period: Int32
    get() {
      if (pointer.isNull) {
        return backing_Period.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 65).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Period.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 66, value.value).getOrThrow()
    }

  private val backing_ResolvedLanguage: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val resolvedLanguage: String
    get() {
      if (pointer.isNull) {
        return backing_ResolvedLanguage.get()
      }
      return PlatformComInterop.invokeHStringMethod(pointer, 102).getOrThrow().use {
          it.toKotlinString() }
    }

  private val backing_Second: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public var second: Int32
    get() {
      if (pointer.isNull) {
        return backing_Second.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 83).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Second.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 84, value.value).getOrThrow()
    }

  private val backing_Year: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public var year: Int32
    get() {
      if (pointer.isNull) {
        return backing_Year.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 30).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Year.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 31, value.value).getOrThrow()
    }

  public constructor(languages: Iterable<String>) :
      this(Companion.factoryCreateCalendarDefaultCalendarAndClock(languages).pointer)

  public constructor(
    languages: Iterable<String>,
    calendar: String,
    clock: String,
  ) : this(Companion.factoryCreateCalendar(languages, calendar, clock).pointer)

  public constructor(
    languages: Iterable<String>,
    calendar: String,
    clock: String,
    timeZoneId: String,
  ) : this(Companion.factory2CreateCalendarWithTimeZone(languages, calendar, clock,
      timeZoneId).pointer)

  override fun getTimeZone(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow().use { it.toKotlinString()
        }
  }

  override fun changeTimeZone(timeZoneId: String) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithStringArg(pointer, 7, timeZoneId).getOrThrow()
  }

  override fun timeZoneAsString(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 8).getOrThrow().use { it.toKotlinString()
        }
  }

  override fun timeZoneAsString(idealLength: Int32): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 9,
        idealLength.value).getOrThrow().use { it.toKotlinString() }
  }

  public fun clone(): Calendar {
    if (pointer.isNull) {
      error("Null runtime object pointer: Clone")
    }
    return Calendar(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
  }

  public fun setToMin() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 7).getOrThrow()
  }

  public fun setToMax() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 8).getOrThrow()
  }

  public fun get_Languages(): IVectorView<String> {
    if (pointer.isNull) {
      error("Null runtime object pointer: get_Languages")
    }
    return IVectorView<String>(PlatformComInterop.invokeObjectMethod(pointer, 9).getOrThrow())
  }

  public fun getCalendarSystem(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 12).getOrThrow().use {
        it.toKotlinString() }
  }

  public fun changeCalendarSystem(value: String) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithStringArg(pointer, 13, value).getOrThrow()
  }

  public fun getClock(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 14).getOrThrow().use {
        it.toKotlinString() }
  }

  public fun changeClock(value: String) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithStringArg(pointer, 15, value).getOrThrow()
  }

  public fun getDateTime(): Instant {
    if (pointer.isNull) {
      return Instant.fromEpochSeconds(0)
    }
    return Instant.fromEpochSeconds((PlatformComInterop.invokeInt64Getter(pointer,
        16).getOrThrow() - 116_444_736_000_000_000) / 10000000L,
        ((PlatformComInterop.invokeInt64Getter(pointer, 16).getOrThrow() - 116_444_736_000_000_000)
        % 10000000L * 100).toInt())
  }

  public fun setDateTime(value: Instant) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 17, (((value.epochSeconds *
        10000000L) + (value.nanosecondsOfSecond / 100)) + 116444736000000000)).getOrThrow()
  }

  public fun setToNow() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 18).getOrThrow()
  }

  public fun addEras(eras: Int32) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 24, eras.value).getOrThrow()
  }

  public fun eraAsString(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 25).getOrThrow().use {
        it.toKotlinString() }
  }

  public fun eraAsString(idealLength: Int32): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 26,
        idealLength.value).getOrThrow().use { it.toKotlinString() }
  }

  public fun addYears(years: Int32) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 32, years.value).getOrThrow()
  }

  public fun yearAsString(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 33).getOrThrow().use {
        it.toKotlinString() }
  }

  public fun yearAsTruncatedString(remainingDigits: Int32): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 34,
        remainingDigits.value).getOrThrow().use { it.toKotlinString() }
  }

  public fun yearAsPaddedString(minDigits: Int32): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 35,
        minDigits.value).getOrThrow().use { it.toKotlinString() }
  }

  public fun addMonths(months: Int32) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 41, months.value).getOrThrow()
  }

  public fun monthAsString(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 42).getOrThrow().use {
        it.toKotlinString() }
  }

  public fun monthAsString(idealLength: Int32): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 43,
        idealLength.value).getOrThrow().use { it.toKotlinString() }
  }

  public fun monthAsSoloString(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 44).getOrThrow().use {
        it.toKotlinString() }
  }

  public fun monthAsSoloString(idealLength: Int32): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 45,
        idealLength.value).getOrThrow().use { it.toKotlinString() }
  }

  public fun monthAsNumericString(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 46).getOrThrow().use {
        it.toKotlinString() }
  }

  public fun monthAsPaddedNumericString(minDigits: Int32): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 47,
        minDigits.value).getOrThrow().use { it.toKotlinString() }
  }

  public fun addWeeks(weeks: Int32) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 48, weeks.value).getOrThrow()
  }

  public fun addDays(days: Int32) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 54, days.value).getOrThrow()
  }

  public fun dayAsString(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 55).getOrThrow().use {
        it.toKotlinString() }
  }

  public fun dayAsPaddedString(minDigits: Int32): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 56,
        minDigits.value).getOrThrow().use { it.toKotlinString() }
  }

  public fun dayOfWeekAsString(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 58).getOrThrow().use {
        it.toKotlinString() }
  }

  public fun dayOfWeekAsString(idealLength: Int32): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 59,
        idealLength.value).getOrThrow().use { it.toKotlinString() }
  }

  public fun dayOfWeekAsSoloString(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 60).getOrThrow().use {
        it.toKotlinString() }
  }

  public fun dayOfWeekAsSoloString(idealLength: Int32): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 61,
        idealLength.value).getOrThrow().use { it.toKotlinString() }
  }

  public fun addPeriods(periods: Int32) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 67, periods.value).getOrThrow()
  }

  public fun periodAsString(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 68).getOrThrow().use {
        it.toKotlinString() }
  }

  public fun periodAsString(idealLength: Int32): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 69,
        idealLength.value).getOrThrow().use { it.toKotlinString() }
  }

  public fun addHours(hours: Int32) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 75, hours.value).getOrThrow()
  }

  public fun hourAsString(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 76).getOrThrow().use {
        it.toKotlinString() }
  }

  public fun hourAsPaddedString(minDigits: Int32): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 77,
        minDigits.value).getOrThrow().use { it.toKotlinString() }
  }

  public fun addMinutes(minutes: Int32) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 80, minutes.value).getOrThrow()
  }

  public fun minuteAsString(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 81).getOrThrow().use {
        it.toKotlinString() }
  }

  public fun minuteAsPaddedString(minDigits: Int32): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 82,
        minDigits.value).getOrThrow().use { it.toKotlinString() }
  }

  public fun addSeconds(seconds: Int32) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 85, seconds.value).getOrThrow()
  }

  public fun secondAsString(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 86).getOrThrow().use {
        it.toKotlinString() }
  }

  public fun secondAsPaddedString(minDigits: Int32): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 87,
        minDigits.value).getOrThrow().use { it.toKotlinString() }
  }

  public fun addNanoseconds(nanoseconds: Int32) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 90, nanoseconds.value).getOrThrow()
  }

  public fun nanosecondAsString(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 91).getOrThrow().use {
        it.toKotlinString() }
  }

  public fun nanosecondAsPaddedString(minDigits: Int32): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 92,
        minDigits.value).getOrThrow().use { it.toKotlinString() }
  }

  public fun compare(other: Calendar): Int32 {
    if (pointer.isNull) {
      return Int32(0)
    }
    return Int32(PlatformComInterop.invokeInt32MethodWithObjectArg(pointer, 93,
        projectedObjectArgumentPointer(other, "Windows.Globalization.Calendar",
        "rc(Windows.Globalization.Calendar;{ca30221d-86d9-40fb-a26b-d44eb7cf08ea})")).getOrThrow())
  }

  public fun copyTo(other: Calendar) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 95, projectedObjectArgumentPointer(other,
        "Windows.Globalization.Calendar",
        "rc(Windows.Globalization.Calendar;{ca30221d-86d9-40fb-a26b-d44eb7cf08ea})")).getOrThrow()
  }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.Globalization.Calendar"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Globalization", "Calendar")

    override val defaultInterfaceName: String? = "Windows.Globalization.ICalendar"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    private val factory: ICalendarFactory by lazy { WinRtRuntime.projectActivationFactory(this,
        ICalendarFactory, ::ICalendarFactory) }

    private val factory2: ICalendarFactory2 by lazy { WinRtRuntime.projectActivationFactory(this,
        ICalendarFactory2, ::ICalendarFactory2) }

    private fun factoryCreateCalendarDefaultCalendarAndClock(languages: Iterable<String>): Calendar
        = factory.createCalendarDefaultCalendarAndClock(languages)

    private fun factoryCreateCalendar(
      languages: Iterable<String>,
      calendar: String,
      clock: String,
    ): Calendar = factory.createCalendar(languages, calendar, clock)

    private fun factory2CreateCalendarWithTimeZone(
      languages: Iterable<String>,
      calendar: String,
      clock: String,
      timeZoneId: String,
    ): Calendar = factory2.createCalendarWithTimeZone(languages, calendar, clock, timeZoneId)
  }
}
