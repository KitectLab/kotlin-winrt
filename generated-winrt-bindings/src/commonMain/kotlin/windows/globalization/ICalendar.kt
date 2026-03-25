package windows.globalization

import dev.winrt.core.Inspectable
import dev.winrt.core.Int32
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

  public val dayOfWeek: DayOfWeek
    get() = get_DayOfWeek()

  public val isDaylightSavingTime: WinRtBoolean
    get() = get_IsDaylightSavingTime()

  public fun clone(): Calendar =
      Calendar(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

  public fun get_Year(): Int32 =
      Int32(PlatformComInterop.invokeInt32Method(pointer, 30).getOrThrow())

  public fun setYear(value: Int32) {
    PlatformComInterop.invokeInt32Setter(pointer, 31, value.value).getOrThrow()
  }

  public fun yearAsString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 33).getOrThrow()
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

  public fun get_Day(): Int32 =
      Int32(PlatformComInterop.invokeInt32Method(pointer, 52).getOrThrow())

  public fun get_DayOfWeek(): DayOfWeek =
      DayOfWeek.fromValue(PlatformComInterop.invokeUInt32Method(pointer, 57).getOrThrow().toInt())

  public fun get_IsDaylightSavingTime(): WinRtBoolean =
      WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 103).getOrThrow())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Globalization.ICalendar"

    override val iid: Guid = guidOf("ca30221d-86d9-40fb-a26b-d44eb7cf08ea")

    public fun from(inspectable: Inspectable): ICalendar = inspectable.projectInterface(this,
        ::ICalendar)
  }
}
