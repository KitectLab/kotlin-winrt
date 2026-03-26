package windows.globalization

import dev.winrt.core.Inspectable
import dev.winrt.core.Int32
import dev.winrt.core.DateTime
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.WinRtStrings
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String

public open class ICalendar(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public val year: Int32
    get() = get_Year()

  public val dateTime: DateTime
    get() = get_DateTime()

  public var month: Int32
    get() = get_Month()
    set(value) {
      PlatformComInterop.invokeInt32Setter(pointer, 40, value.value).getOrThrow()
    }

  public var day: Int32
    get() = get_Day()
    set(value) {
      PlatformComInterop.invokeInt32Setter(pointer, 53, value.value).getOrThrow()
    }

  public var hour: Int32
    get() = get_Hour()
    set(value) {
      PlatformComInterop.invokeInt32Setter(pointer, 74, value.value).getOrThrow()
    }

  public var minute: Int32
    get() = get_Minute()
    set(value) {
      PlatformComInterop.invokeInt32Setter(pointer, 79, value.value).getOrThrow()
    }

  public var second: Int32
    get() = get_Second()
    set(value) {
      PlatformComInterop.invokeInt32Setter(pointer, 84, value.value).getOrThrow()
    }

  public var nanosecond: Int32
    get() = get_Nanosecond()
    set(value) {
      PlatformComInterop.invokeInt32Setter(pointer, 89, value.value).getOrThrow()
    }

  public var numeralSystem: String
    get() = get_NumeralSystem()
    set(value) {
      PlatformComInterop.invokeStringSetter(pointer, 11, value).getOrThrow()
    }

  public var calendarSystem: String
    get() = get_CalendarSystem()
    set(value) {
      changeCalendarSystem(value)
    }

  public var clock: String
    get() = get_Clock()
    set(value) {
      changeClock(value)
    }

  public val dayOfWeek: DayOfWeek
    get() = get_DayOfWeek()

  public val resolvedLanguage: String
    get() = get_ResolvedLanguage()

  public val isDaylightSavingTime: WinRtBoolean
    get() = get_IsDaylightSavingTime()

  public fun clone(): Calendar =
      Calendar(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

  public fun get_Year(): Int32 =
      Int32(PlatformComInterop.invokeInt32Method(pointer, 30).getOrThrow())

  public fun setYear(value: Int32) {
    PlatformComInterop.invokeInt32Setter(pointer, 31, value.value).getOrThrow()
  }

  public fun addYears(years: Int32) {
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 32, years.value).getOrThrow()
  }

  public fun get_DateTime(): DateTime =
      DateTime(PlatformComInterop.invokeInt64Getter(pointer, 16).getOrThrow())

  public fun setDateTime(value: DateTime) {
    PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 17, value.universalTime).getOrThrow()
  }

  public fun setToNow() {
    PlatformComInterop.invokeUnitMethod(pointer, 18).getOrThrow()
  }

  public fun yearAsString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 33).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun yearAsTruncatedString(remainingDigits: Int32): String {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 34,
        remainingDigits.value).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun yearAsPaddedString(minDigits: Int32): String {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 35,
        minDigits.value).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun get_Month(): Int32 =
      Int32(PlatformComInterop.invokeInt32Method(pointer, 39).getOrThrow())

  public fun addMonths(months: Int32) {
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 41, months.value).getOrThrow()
  }

  public fun monthAsString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 42).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun monthAsString(idealLength: Int32): String {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 43,
        idealLength.value).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun monthAsSoloString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 44).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun monthAsSoloString(idealLength: Int32): String {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 45,
        idealLength.value).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun monthAsNumericString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 46).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun monthAsPaddedNumericString(minDigits: Int32): String {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 47,
        minDigits.value).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun addWeeks(weeks: Int32) {
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 48, weeks.value).getOrThrow()
  }

  public fun get_Day(): Int32 =
      Int32(PlatformComInterop.invokeInt32Method(pointer, 52).getOrThrow())

  public fun addDays(days: Int32) {
    PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 54, days.value).getOrThrow()
  }

  public fun dayAsString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 55).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun dayAsPaddedString(minDigits: Int32): String {
    val value = PlatformComInterop.invokeHStringMethodWithInt32Arg(pointer, 56,
        minDigits.value).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun get_Hour(): Int32 =
      Int32(PlatformComInterop.invokeInt32Method(pointer, 73).getOrThrow())

  public fun get_Minute(): Int32 =
      Int32(PlatformComInterop.invokeInt32Method(pointer, 78).getOrThrow())

  public fun get_Second(): Int32 =
      Int32(PlatformComInterop.invokeInt32Method(pointer, 83).getOrThrow())

  public fun get_Nanosecond(): Int32 =
      Int32(PlatformComInterop.invokeInt32Method(pointer, 88).getOrThrow())

  public fun get_NumeralSystem(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 10).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun get_CalendarSystem(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 12).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun changeCalendarSystem(value: String) {
    PlatformComInterop.invokeStringSetter(pointer, 13, value).getOrThrow()
  }

  public fun get_Clock(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 14).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun changeClock(value: String) {
    PlatformComInterop.invokeStringSetter(pointer, 15, value).getOrThrow()
  }

  public fun get_DayOfWeek(): DayOfWeek =
      DayOfWeek.fromValue(PlatformComInterop.invokeUInt32Method(pointer, 57).getOrThrow().toInt())

  public fun dayOfWeekAsString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 58).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun get_ResolvedLanguage(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 102).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun get_IsDaylightSavingTime(): WinRtBoolean =
      WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 103).getOrThrow())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Globalization.ICalendar"

    override val iid: Guid = guidOf("ca30221d-86d9-40fb-a26b-d44eb7cf08ea")

    public fun from(inspectable: Inspectable): ICalendar = inspectable.projectInterface(this,
        ::ICalendar)
  }
}
