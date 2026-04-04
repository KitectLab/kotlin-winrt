package windows.system.userprofile

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String

internal open class IGlobalizationPreferencesStatics2(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public fun trySetHomeGeographicRegion(region: String): WinRtBoolean =
      WinRtBoolean(PlatformComInterop.invokeBooleanMethodWithStringArg(pointer, 6,
      region).getOrThrow())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String =
        "Windows.System.UserProfile.IGlobalizationPreferencesStatics2"

    override val projectionTypeKey: String =
        "Windows.System.UserProfile.IGlobalizationPreferencesStatics2"

    override val iid: Guid = guidOf("fcce85f1-4300-4cd0-9cac-1a8e7b7e18f4")

    public fun from(inspectable: Inspectable): IGlobalizationPreferencesStatics2 =
        inspectable.projectInterface(this, ::IGlobalizationPreferencesStatics2)

    public operator fun invoke(inspectable: Inspectable): IGlobalizationPreferencesStatics2 =
        from(inspectable)
  }
}
