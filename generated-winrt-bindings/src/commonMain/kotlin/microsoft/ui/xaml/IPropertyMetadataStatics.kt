package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComMethodResultKind
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.requireObject

internal open class IPropertyMetadataStatics(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
    fun create(defaultValue: Inspectable): PropertyMetadata =
        PropertyMetadata(PlatformComInterop.invokeObjectMethodWithObjectArg(pointer, 6, defaultValue.pointer).getOrThrow())

    fun create(defaultValue: Inspectable, propertyChangedCallback: PropertyChangedCallback): PropertyMetadata =
        PropertyMetadata(
            PlatformComInterop.invokeMethodWithTwoObjectArgs(
                pointer,
                7,
                ComMethodResultKind.OBJECT,
                defaultValue.pointer,
                propertyChangedCallback.pointer,
            ).getOrThrow().requireObject(),
        )

    fun create(createDefaultValueCallback: CreateDefaultValueCallback): PropertyMetadata =
        PropertyMetadata(PlatformComInterop.invokeObjectMethodWithObjectArg(pointer, 8, createDefaultValueCallback.pointer).getOrThrow())

    fun create(createDefaultValueCallback: CreateDefaultValueCallback, propertyChangedCallback: PropertyChangedCallback): PropertyMetadata =
        PropertyMetadata(
            PlatformComInterop.invokeMethodWithTwoObjectArgs(
                pointer,
                9,
                ComMethodResultKind.OBJECT,
                createDefaultValueCallback.pointer,
                propertyChangedCallback.pointer,
            ).getOrThrow().requireObject(),
        )

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.IPropertyMetadataStatics"
        override val projectionTypeKey: String = "Microsoft.UI.Xaml.IPropertyMetadataStatics"
        override val iid: Guid = guidOf("37b8add4-7a4a-5cf7-a174-235182cd082e")

        fun from(inspectable: Inspectable): IPropertyMetadataStatics =
            inspectable.projectInterface(this, ::IPropertyMetadataStatics)

        operator fun invoke(inspectable: Inspectable): IPropertyMetadataStatics = from(inspectable)

        fun create(defaultValue: Inspectable): PropertyMetadata = error("Unbound static projection")
        fun create(defaultValue: Inspectable, propertyChangedCallback: PropertyChangedCallback): PropertyMetadata = error("Unbound static projection")
        fun create(createDefaultValueCallback: CreateDefaultValueCallback): PropertyMetadata = error("Unbound static projection")
        fun create(createDefaultValueCallback: CreateDefaultValueCallback, propertyChangedCallback: PropertyChangedCallback): PropertyMetadata = error("Unbound static projection")
    }
}
