package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid

typealias DependencyPropertyChangedCallbackHandler = (DependencyObject, DependencyPropertyChangedEventArgs) -> Unit

open class DependencyPropertyChangedCallback(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.DependencyPropertyChangedCallback"
        override val iid: Guid = guidOf("f055bb21-219b-5b0c-805d-bcaedae15458")

        fun from(inspectable: Inspectable): DependencyPropertyChangedCallback =
            inspectable.projectInterface(this, ::DependencyPropertyChangedCallback)
    }
}
