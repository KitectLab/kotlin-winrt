package microsoft.ui.xaml.controls

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

interface IContentControl {
    var content: Inspectable

    fun get_Content(): Inspectable

    fun put_Content(value: Inspectable)

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.IContentControl"
        override val projectionTypeKey: String = "Microsoft.UI.Xaml.Controls.IContentControl"
        override val iid: Guid = guidOf("07e81761-11b2-52ae-8f8b-4d53d2b5900a")

        fun from(inspectable: Inspectable): IContentControl =
            inspectable.projectInterface(this, ::IContentControlProjection)

        operator fun invoke(inspectable: Inspectable): IContentControl = from(inspectable)
    }
}

private class IContentControlProjection(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IContentControl {
    override var content: Inspectable
        get() = Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
        set(value) {
            PlatformComInterop.invokeObjectSetter(pointer, 7, value.pointer).getOrThrow()
        }

    override fun get_Content(): Inspectable =
        Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

    override fun put_Content(value: Inspectable) {
        PlatformComInterop.invokeObjectSetter(pointer, 7, value.pointer).getOrThrow()
    }
}
