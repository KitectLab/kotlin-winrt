package microsoft.ui.xaml.interop

import dev.winrt.core.Inspectable
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.projection.WinRtListProjection
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

open class IBindableVectorView(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    List<Inspectable> by createListDelegate(pointer) {
    fun getAt(index: UInt32): Inspectable =
        Inspectable(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 7, index.value).getOrThrow())

    val winRtSize: UInt32
        get() = UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())

    fun first(): IBindableIterator =
        IBindableIterator(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Interop.IBindableVectorView"
        override val projectionTypeKey: String = "Microsoft.UI.Xaml.Interop.IBindableVectorView"
        override val iid: Guid = guidOf("346dd6e7-976e-4bc3-815d-ece243bc0f33")

        private fun createListDelegate(pointer: ComPtr): List<Inspectable> =
            WinRtListProjection(
                sizeProvider = {
                    UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow()).value.toInt()
                },
                getter = { index ->
                    Inspectable(
                        PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 7, index.toUInt()).getOrThrow(),
                    )
                },
            )

        fun from(inspectable: Inspectable): IBindableVectorView =
            inspectable.projectInterface(this, ::IBindableVectorView)
    }
}
