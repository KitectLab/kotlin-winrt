package microsoft.ui.xaml.interop

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

open class IBindableIterable(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
    fun first(): IBindableIterator =
        IBindableIterator(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Interop.IBindableIterable"
        override val projectionTypeKey: String = "System.Collections.IEnumerable"
        override val iid: Guid = guidOf("036d2c08-df29-41af-8aa2-d774be62ba6f")

        fun from(inspectable: Inspectable): IBindableIterable =
            inspectable.projectInterface(this, ::IBindableIterable)
    }
}
