package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid

public open class IApplicationInitializationCallbackParams(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String =
        "Microsoft.UI.Xaml.IApplicationInitializationCallbackParams"

    override val iid: Guid = guidOf("1b1906ea-5b7b-5876-81ab-7c2281ac3d20")

    public fun from(inspectable: Inspectable): IApplicationInitializationCallbackParams =
        inspectable.projectInterface(this, ::IApplicationInitializationCallbackParams)
  }
}
