package windows.system.userprofile

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import kotlin.String

public open class IGlobalizationPreferencesStatics3(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String =
        "Windows.System.UserProfile.IGlobalizationPreferencesStatics3"

    override val iid: Guid = guidOf("1e059733-35f5-40d8-b9e8-aef3ef856fce")

    public fun from(inspectable: Inspectable): IGlobalizationPreferencesStatics3 =
        inspectable.projectInterface(this, ::IGlobalizationPreferencesStatics3)
  }
}
