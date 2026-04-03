package microsoft.ui.xaml.controls

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

interface IPanel {
    val children: UIElementCollection

    fun get_Children(): UIElementCollection

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.IPanel"
        override val projectionTypeKey: String = "Microsoft.UI.Xaml.Controls.IPanel"
        override val iid: Guid = guidOf("27a1b418-56f3-525e-b883-cefed905eed3")

        fun from(inspectable: Inspectable): IPanel =
            inspectable.projectInterface(this, ::IPanelProjection)

        operator fun invoke(inspectable: Inspectable): IPanel = from(inspectable)
    }
}

private class IPanelProjection(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IPanel {
    override val children: UIElementCollection
        get() = UIElementCollection(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

    override fun get_Children(): UIElementCollection =
        UIElementCollection(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
}
