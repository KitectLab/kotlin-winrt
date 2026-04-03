package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import windows.ui.xaml.interop.TypeName

interface IDependencyProperty {
    fun getMetadata(forType: TypeName): PropertyMetadata

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.IDependencyProperty"
        override val projectionTypeKey: String = "Microsoft.UI.Xaml.IDependencyProperty"
        override val iid: Guid = guidOf("960eab49-9672-58a0-995b-3a42e5ea6278")

        fun from(inspectable: Inspectable): IDependencyProperty =
            inspectable.projectInterface(this, ::IDependencyPropertyProjection)

        operator fun invoke(inspectable: Inspectable): IDependencyProperty = from(inspectable)
    }
}

private class IDependencyPropertyProjection(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer), IDependencyProperty {
    override fun getMetadata(forType: TypeName): PropertyMetadata =
        PropertyMetadata(PlatformComInterop.invokeObjectMethodWithObjectArg(pointer, 6, forType.pointer).getOrThrow())
}
