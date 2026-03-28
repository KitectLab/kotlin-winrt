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

open class IBindableVector(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
    fun getAt(index: UInt32): Inspectable =
        Inspectable(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 6, index.value).getOrThrow())

    fun getView(): IBindableVectorView =
        IBindableVectorView(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())

    fun append(value: Inspectable) {
        PlatformComInterop.invokeObjectSetter(pointer, 13, value.pointer).getOrThrow()
    }

    fun removeAtEnd() {
        PlatformComInterop.invokeUnitMethod(pointer, 14).getOrThrow()
    }

    fun clear() {
        PlatformComInterop.invokeUnitMethod(pointer, 15).getOrThrow()
    }

    fun first(): IBindableIterator =
        IBindableIterator(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Interop.IBindableVector"
        override val projectionTypeKey: String = "System.Collections.IList"
        override val iid: Guid = guidOf("393de7de-6fd0-4c0d-bb71-47244a113e93")

        fun from(inspectable: Inspectable): IBindableVector =
            inspectable.projectInterface(this, ::IBindableVector)
    }
}
