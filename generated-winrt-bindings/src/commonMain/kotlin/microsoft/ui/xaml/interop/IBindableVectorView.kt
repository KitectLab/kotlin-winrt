package microsoft.ui.xaml.interop

import dev.winrt.core.Inspectable
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

open class IBindableVectorView(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
    fun getAt(index: UInt32): Inspectable =
        Inspectable(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 6, index.value).getOrThrow())

    fun first(): IBindableIterator =
        IBindableIterator(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Interop.IBindableVectorView"
        override val projectionTypeKey: String = "Microsoft.UI.Xaml.Interop.IBindableVectorView"
        override val iid: Guid = guidOf("346dd6e7-976e-4bc3-815d-ece243bc0f33")

        fun from(inspectable: Inspectable): IBindableVectorView =
            inspectable.projectInterface(this, ::IBindableVectorView)
    }
}
