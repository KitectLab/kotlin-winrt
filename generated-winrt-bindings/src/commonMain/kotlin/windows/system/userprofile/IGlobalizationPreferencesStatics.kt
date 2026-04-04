package windows.system.userprofile

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import windows.globalization.DayOfWeek

internal open class IGlobalizationPreferencesStatics(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public val homeGeographicRegion: String
    get() = PlatformComInterop.invokeHStringMethod(pointer, 10).getOrThrow().use {
        it.toKotlinString() }

  public val weekStartsOn: DayOfWeek
    get() = DayOfWeek(PlatformComInterop.invokeObjectMethod(pointer, 11).getOrThrow())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String =
        "Windows.System.UserProfile.IGlobalizationPreferencesStatics"

    override val projectionTypeKey: String =
        "Windows.System.UserProfile.IGlobalizationPreferencesStatics"

    override val iid: Guid = guidOf("01bf4326-ed37-4e96-b0e9-c1340d1ea158")

    public fun from(inspectable: Inspectable): IGlobalizationPreferencesStatics =
        inspectable.projectInterface(this, ::IGlobalizationPreferencesStatics)

    public operator fun invoke(inspectable: Inspectable): IGlobalizationPreferencesStatics =
        from(inspectable)
  }
}
