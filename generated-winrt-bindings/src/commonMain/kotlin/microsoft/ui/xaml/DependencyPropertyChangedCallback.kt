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

public typealias DependencyPropertyChangedCallbackHandler = (DependencyObject,
    DependencyProperty) -> Unit

public open class DependencyPropertyChangedCallback(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.DependencyPropertyChangedCallback"

    override val iid: Guid = guidOf("f055bb21-219b-5b0c-805d-bcaedae15458")

    public fun from(inspectable: Inspectable): DependencyPropertyChangedCallback =
        inspectable.projectInterface(this, ::DependencyPropertyChangedCallback)
  }
}
