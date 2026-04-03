package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import microsoft.ui.xaml.media.SystemBackdrop

interface IWindow2 {
    var systemBackdrop: SystemBackdrop

    fun get_SystemBackdrop(): SystemBackdrop

    fun put_SystemBackdrop(value: SystemBackdrop)

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.IWindow2"
        override val projectionTypeKey: String = "Microsoft.UI.Xaml.IWindow2"
        override val iid: Guid = guidOf("42febaa5-1c32-522a-a591-57618c6f665d")

        fun from(inspectable: Inspectable): IWindow2 =
            inspectable.projectInterface(this, ::IWindow2Projection)

        operator fun invoke(inspectable: Inspectable): IWindow2 = from(inspectable)
    }
}

private class IWindow2Projection(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IWindow2 {
    override var systemBackdrop: SystemBackdrop
        get() = SystemBackdrop(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
        set(value) {
            PlatformComInterop.invokeObjectSetter(pointer, 7, (value as Inspectable).pointer).getOrThrow()
        }

    override fun get_SystemBackdrop(): SystemBackdrop =
        SystemBackdrop(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

    override fun put_SystemBackdrop(value: SystemBackdrop) {
        PlatformComInterop.invokeObjectSetter(pointer, 7, (value as Inspectable).pointer).getOrThrow()
    }
}
