package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid

typealias PropertyChangedCallbackHandler = (DependencyObject, DependencyPropertyChangedEventArgs) -> Unit

open class PropertyChangedCallback(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.PropertyChangedCallback"
        override val iid: Guid = guidOf("5fd9243a-2422-53c9-8d6f-f1ba1a0bba9a")

        fun from(inspectable: Inspectable): PropertyChangedCallback =
            inspectable.projectInterface(this, ::PropertyChangedCallback)
    }
}
