package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

interface IPropertyMetadata {
    val createDefaultValueCallback: CreateDefaultValueCallback
    val defaultValue: Inspectable

    fun get_DefaultValue(): Inspectable
    fun get_CreateDefaultValueCallback(): CreateDefaultValueCallback

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.IPropertyMetadata"
        override val projectionTypeKey: String = "Microsoft.UI.Xaml.IPropertyMetadata"
        override val iid: Guid = guidOf("b3644425-9464-5434-b0ae-aff8d3159fe1")

        fun from(inspectable: Inspectable): IPropertyMetadata =
            inspectable.projectInterface(this, ::IPropertyMetadataProjection)

        operator fun invoke(inspectable: Inspectable): IPropertyMetadata = from(inspectable)
    }
}

private class IPropertyMetadataProjection(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer), IPropertyMetadata {
    override val createDefaultValueCallback: CreateDefaultValueCallback
        get() = CreateDefaultValueCallback(PlatformComInterop.invokeObjectMethod(pointer, 7).getOrThrow())

    override val defaultValue: Inspectable
        get() = Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

    override fun get_DefaultValue(): Inspectable =
        Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

    override fun get_CreateDefaultValueCallback(): CreateDefaultValueCallback =
        CreateDefaultValueCallback(PlatformComInterop.invokeObjectMethod(pointer, 7).getOrThrow())
}
