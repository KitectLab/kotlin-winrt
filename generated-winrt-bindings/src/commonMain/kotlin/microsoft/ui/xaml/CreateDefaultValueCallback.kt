package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import kotlin.String

public typealias CreateDefaultValueCallbackHandler = () -> Inspectable

public open class CreateDefaultValueCallback(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.CreateDefaultValueCallback"

    override val iid: Guid = guidOf("7f808c05-2ac4-5ad9-ac8a-26890333d81e")

    public fun from(inspectable: Inspectable): CreateDefaultValueCallback =
        inspectable.projectInterface(this, ::CreateDefaultValueCallback)
  }
}
