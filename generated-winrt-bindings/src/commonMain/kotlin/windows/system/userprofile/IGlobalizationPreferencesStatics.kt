package windows.system.userprofile

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import windows.foundation.collections.StringVectorView
import windows.globalization.DayOfWeek

internal open class IGlobalizationPreferencesStatics(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public val calendars: StringVectorView
    get() = get_Calendars()

  public val clocks: StringVectorView
    get() = get_Clocks()

  public val currencies: StringVectorView
    get() = get_Currencies()

  public val languages: StringVectorView
    get() = get_Languages()

  public val homeGeographicRegion: String
    get() = get_HomeGeographicRegion()

  public val weekStartsOn: DayOfWeek
    get() = get_WeekStartsOn()

  public fun get_Calendars(): StringVectorView =
      StringVectorView(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

  public fun get_Clocks(): StringVectorView =
      StringVectorView(PlatformComInterop.invokeObjectMethod(pointer, 7).getOrThrow())

  public fun get_Currencies(): StringVectorView =
      StringVectorView(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())

  public fun get_Languages(): StringVectorView =
      StringVectorView(PlatformComInterop.invokeObjectMethod(pointer, 9).getOrThrow())

  public fun get_HomeGeographicRegion(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 10).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  public fun get_WeekStartsOn(): DayOfWeek =
      DayOfWeek.fromValue(PlatformComInterop.invokeUInt32Method(pointer, 11).getOrThrow().toInt())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String =
        "Windows.System.UserProfile.IGlobalizationPreferencesStatics"

    override val iid: Guid = guidOf("01bf4326-ed37-4e96-b0e9-c1340d1ea158")

    public fun from(inspectable: Inspectable): IGlobalizationPreferencesStatics =
        inspectable.projectInterface(this, ::IGlobalizationPreferencesStatics)
  }
}
