package windows.globalization

import dev.winrt.core.Inspectable
import dev.winrt.core.Int32
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import kotlin.time.Instant
import windows.foundation.collections.IVectorView

public interface ICalendar {
  public var day: Int32

  public val dayOfWeek: DayOfWeek

  public var era: Int32

  public val firstDayInThisMonth: Int32

  public val firstEra: Int32

  public val firstHourInThisPeriod: Int32

  public val firstMinuteInThisHour: Int32

  public val firstMonthInThisYear: Int32

  public val firstPeriodInThisDay: Int32

  public val firstSecondInThisMinute: Int32

  public val firstYearInThisEra: Int32

  public var hour: Int32

  public val isDaylightSavingTime: WinRtBoolean

  public val languages: IVectorView<String>

  public val lastDayInThisMonth: Int32

  public val lastEra: Int32

  public val lastHourInThisPeriod: Int32

  public val lastMinuteInThisHour: Int32

  public val lastMonthInThisYear: Int32

  public val lastPeriodInThisDay: Int32

  public val lastSecondInThisMinute: Int32

  public val lastYearInThisEra: Int32

  public var minute: Int32

  public var month: Int32

  public var nanosecond: Int32

  public val numberOfDaysInThisMonth: Int32

  public val numberOfEras: Int32

  public val numberOfHoursInThisPeriod: Int32

  public val numberOfMinutesInThisHour: Int32

  public val numberOfMonthsInThisYear: Int32

  public val numberOfPeriodsInThisDay: Int32

  public val numberOfSecondsInThisMinute: Int32

  public val numberOfYearsInThisEra: Int32

  public var numeralSystem: String

  public var period: Int32

  public val resolvedLanguage: String

  public var second: Int32

  public var year: Int32

  public fun clone(): Calendar

  public fun setToMin()

  public fun setToMax()

  public fun getCalendarSystem(): String

  public fun changeCalendarSystem(value: String)

  public fun getClock(): String

  public fun changeClock(value: String)

  public fun getDateTime(): Instant

  public fun setDateTime(value: Instant)

  public fun setToNow()

  public fun addEras(eras: Int32)

  public fun eraAsString(): String

  public fun eraAsString(idealLength: Int32): String

  public fun addYears(years: Int32)

  public fun yearAsString(): String

  public fun yearAsTruncatedString(remainingDigits: Int32): String

  public fun yearAsPaddedString(minDigits: Int32): String

  public fun addMonths(months: Int32)

  public fun monthAsString(): String

  public fun monthAsString(idealLength: Int32): String

  public fun monthAsSoloString(): String

  public fun monthAsSoloString(idealLength: Int32): String

  public fun monthAsNumericString(): String

  public fun monthAsPaddedNumericString(minDigits: Int32): String

  public fun addWeeks(weeks: Int32)

  public fun addDays(days: Int32)

  public fun dayAsString(): String

  public fun dayAsPaddedString(minDigits: Int32): String

  public fun dayOfWeekAsString(): String

  public fun dayOfWeekAsString(idealLength: Int32): String

  public fun dayOfWeekAsSoloString(): String

  public fun dayOfWeekAsSoloString(idealLength: Int32): String

  public fun addPeriods(periods: Int32)

  public fun periodAsString(): String

  public fun periodAsString(idealLength: Int32): String

  public fun addHours(hours: Int32)

  public fun hourAsString(): String

  public fun hourAsPaddedString(minDigits: Int32): String

  public fun addMinutes(minutes: Int32)

  public fun minuteAsString(): String

  public fun minuteAsPaddedString(minDigits: Int32): String

  public fun addSeconds(seconds: Int32)

  public fun secondAsString(): String

  public fun secondAsPaddedString(minDigits: Int32): String

  public fun addNanoseconds(nanoseconds: Int32)

  public fun nanosecondAsString(): String

  public fun nanosecondAsPaddedString(minDigits: Int32): String

  public fun compare(other: Calendar): Int32

  public fun copyTo(other: Calendar)

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Globalization.ICalendar"

    override val projectionTypeKey: String = "Windows.Globalization.ICalendar"

    override val iid: Guid = guidOf("ca30221d-86d9-40fb-a26b-d44eb7cf08ea")

    public fun from(inspectable: Inspectable): ICalendar = inspectable.projectInterface(this,
        ::ICalendarProjection)

    public operator fun invoke(inspectable: Inspectable): ICalendar = from(inspectable)
  }
}

private class ICalendarProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    ICalendar {
  override var day: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 52).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 53, value.value).getOrThrow()
    }

  override val dayOfWeek: DayOfWeek
    get() = DayOfWeek.fromValue(PlatformComInterop.invokeInt32Method(pointer, 57).getOrThrow())

  override var era: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 22).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 23, value.value).getOrThrow()
    }

  override val firstDayInThisMonth: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 49).getOrThrow())

  override val firstEra: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 19).getOrThrow())

  override val firstHourInThisPeriod: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 70).getOrThrow())

  override val firstMinuteInThisHour: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 96).getOrThrow())

  override val firstMonthInThisYear: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 36).getOrThrow())

  override val firstPeriodInThisDay: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 62).getOrThrow())

  override val firstSecondInThisMinute: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 99).getOrThrow())

  override val firstYearInThisEra: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 27).getOrThrow())

  override var hour: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 73).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 74, value.value).getOrThrow()
    }

  override val isDaylightSavingTime: WinRtBoolean
    get() = WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 103).getOrThrow())

  override val languages: IVectorView<String>
    get() = IVectorView.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        9).getOrThrow()), "string", "String")

  override val lastDayInThisMonth: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 50).getOrThrow())

  override val lastEra: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 20).getOrThrow())

  override val lastHourInThisPeriod: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 71).getOrThrow())

  override val lastMinuteInThisHour: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 97).getOrThrow())

  override val lastMonthInThisYear: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 37).getOrThrow())

  override val lastPeriodInThisDay: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 63).getOrThrow())

  override val lastSecondInThisMinute: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 100).getOrThrow())

  override val lastYearInThisEra: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 28).getOrThrow())

  override var minute: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 78).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 79, value.value).getOrThrow()
    }

  override var month: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 39).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 40, value.value).getOrThrow()
    }

  override var nanosecond: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 88).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 89, value.value).getOrThrow()
    }

  override val numberOfDaysInThisMonth: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 51).getOrThrow())

  override val numberOfEras: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 21).getOrThrow())

  override val numberOfHoursInThisPeriod: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 72).getOrThrow())

  override val numberOfMinutesInThisHour: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 98).getOrThrow())

  override val numberOfMonthsInThisYear: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 38).getOrThrow())

  override val numberOfPeriodsInThisDay: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 64).getOrThrow())

  override val numberOfSecondsInThisMinute: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 101).getOrThrow())

  override val numberOfYearsInThisEra: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 29).getOrThrow())

  override var numeralSystem: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 10).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }
    set(value) {
      PlatformComInterop.invokeUnitMethodWithStringArg(pointer, 11, value).getOrThrow()
    }

  override var period: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 65).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 66, value.value).getOrThrow()
    }

  override val resolvedLanguage: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 102).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  override var second: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 83).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 84, value.value).getOrThrow()
    }

  override var year: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 30).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 31, value.value).getOrThrow()
    }

  override fun clone(): Calendar = Calendar(PlatformComInterop.invokeObjectMethod(pointer,
      6).getOrThrow())

  override fun setToMin() {
    PlatformComInterop.invokeUnitMethod(pointer, 7).getOrThrow()
  }

  override fun setToMax() {
    PlatformComInterop.invokeUnitMethod(pointer, 8).getOrThrow()
  }

  override fun getCalendarSystem(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 12).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun changeCalendarSystem(value: String) {
    PlatformComInterop.invokeUnitMethodWithStringArg(pointer, 13, value).getOrThrow()
  }

  override fun getClock(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 14).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun changeClock(value: String) {
    PlatformComInterop.invokeUnitMethodWithStringArg(pointer, 15, value).getOrThrow()
  }

  override fun getDateTime(): Instant =
      Instant.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 16,
      Instant.ABI_LAYOUT).getOrThrow())

  override fun setDateTime(value: Instant) {
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 17, value.toAbi()).getOrThrow()
  }

  override fun setToNow() {
    PlatformComInterop.invokeUnitMethod(pointer, 18).getOrThrow()
  }

  override fun addEras(eras: Int32) {
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 24, eras.value).getOrThrow()
  }

  override fun eraAsString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 25).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun eraAsString(idealLength: Int32): String = run {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 26,
        idealLength.value).getOrThrow()
    try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun addYears(years: Int32) {
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 32, years.value).getOrThrow()
  }

  override fun yearAsString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 33).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun yearAsTruncatedString(remainingDigits: Int32): String = run {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 34,
        remainingDigits.value).getOrThrow()
    try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun yearAsPaddedString(minDigits: Int32): String = run {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 35,
        minDigits.value).getOrThrow()
    try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun addMonths(months: Int32) {
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 41, months.value).getOrThrow()
  }

  override fun monthAsString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 42).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun monthAsString(idealLength: Int32): String = run {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 43,
        idealLength.value).getOrThrow()
    try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun monthAsSoloString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 44).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun monthAsSoloString(idealLength: Int32): String = run {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 45,
        idealLength.value).getOrThrow()
    try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun monthAsNumericString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 46).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun monthAsPaddedNumericString(minDigits: Int32): String = run {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 47,
        minDigits.value).getOrThrow()
    try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun addWeeks(weeks: Int32) {
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 48, weeks.value).getOrThrow()
  }

  override fun addDays(days: Int32) {
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 54, days.value).getOrThrow()
  }

  override fun dayAsString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 55).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun dayAsPaddedString(minDigits: Int32): String = run {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 56,
        minDigits.value).getOrThrow()
    try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun dayOfWeekAsString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 58).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun dayOfWeekAsString(idealLength: Int32): String = run {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 59,
        idealLength.value).getOrThrow()
    try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun dayOfWeekAsSoloString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 60).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun dayOfWeekAsSoloString(idealLength: Int32): String = run {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 61,
        idealLength.value).getOrThrow()
    try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun addPeriods(periods: Int32) {
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 67, periods.value).getOrThrow()
  }

  override fun periodAsString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 68).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun periodAsString(idealLength: Int32): String = run {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 69,
        idealLength.value).getOrThrow()
    try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun addHours(hours: Int32) {
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 75, hours.value).getOrThrow()
  }

  override fun hourAsString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 76).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun hourAsPaddedString(minDigits: Int32): String = run {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 77,
        minDigits.value).getOrThrow()
    try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun addMinutes(minutes: Int32) {
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 80, minutes.value).getOrThrow()
  }

  override fun minuteAsString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 81).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun minuteAsPaddedString(minDigits: Int32): String = run {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 82,
        minDigits.value).getOrThrow()
    try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun addSeconds(seconds: Int32) {
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 85, seconds.value).getOrThrow()
  }

  override fun secondAsString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 86).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun secondAsPaddedString(minDigits: Int32): String = run {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 87,
        minDigits.value).getOrThrow()
    try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun addNanoseconds(nanoseconds: Int32) {
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 90, nanoseconds.value).getOrThrow()
  }

  override fun nanosecondAsString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 91).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun nanosecondAsPaddedString(minDigits: Int32): String = run {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 92,
        minDigits.value).getOrThrow()
    try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun compare(other: Calendar): Int32 =
      Int32(PlatformComInterop.invokeInt32MethodWithObjectArg(pointer, 93,
      projectedObjectArgumentPointer(other, "Windows.Globalization.Calendar",
      "rc(Windows.Globalization.Calendar;{ca30221d-86d9-40fb-a26b-d44eb7cf08ea})")).getOrThrow())

  override fun copyTo(other: Calendar) {
    PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 95, projectedObjectArgumentPointer(other,
        "Windows.Globalization.Calendar",
        "rc(Windows.Globalization.Calendar;{ca30221d-86d9-40fb-a26b-d44eb7cf08ea})")).getOrThrow()
  }
}
