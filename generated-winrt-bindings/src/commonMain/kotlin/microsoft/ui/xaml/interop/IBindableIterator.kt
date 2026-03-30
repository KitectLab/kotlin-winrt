package microsoft.ui.xaml.interop

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

open class IBindableIterator(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
    fun get_Current(): Inspectable =
        Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

    fun get_HasCurrent(): WinRtBoolean =
        WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 7).getOrThrow())

    fun moveNext(): WinRtBoolean =
        WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 8).getOrThrow())

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Interop.IBindableIterator"
        override val projectionTypeKey: String = "kotlin.collections.Iterator"
        override val iid: Guid = guidOf("6a1d6c07-076d-49f2-8314-f52c9c9a8331")

        fun from(inspectable: Inspectable): IBindableIterator =
            inspectable.projectInterface(this, ::IBindableIterator)
    }
}
