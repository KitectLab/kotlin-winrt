package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import kotlin.String
import kotlin.Unit

public typealias DependencyPropertyChangedEventHandlerHandler = (Inspectable,
    DependencyPropertyChangedEventArgs) -> Unit

public open class DependencyPropertyChangedEventHandler(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.DependencyPropertyChangedEventHandler"

    override val iid: Guid = guidOf("4be8dc75-373d-5f4e-a0b4-54b9eeafb4a9")

    public fun from(inspectable: Inspectable): DependencyPropertyChangedEventHandler =
        inspectable.projectInterface(this, ::DependencyPropertyChangedEventHandler)
  }
}
