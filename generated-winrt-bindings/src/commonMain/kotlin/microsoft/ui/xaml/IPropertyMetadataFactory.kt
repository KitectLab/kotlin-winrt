package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid

internal open class IPropertyMetadataFactory(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
    fun createInstanceWithDefaultValue(
        defaultValue: Inspectable,
        baseInterface: Inspectable,
        innerInterface: Inspectable,
    ): PropertyMetadata = error("Unbound runtime factory: createInstanceWithDefaultValue")

    fun createInstanceWithDefaultValueAndCallback(
        defaultValue: Inspectable,
        propertyChangedCallback: PropertyChangedCallback,
        baseInterface: Inspectable,
        innerInterface: Inspectable,
    ): PropertyMetadata = error("Unbound runtime factory: createInstanceWithDefaultValueAndCallback")

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.IPropertyMetadataFactory"
        override val projectionTypeKey: String = "Microsoft.UI.Xaml.IPropertyMetadataFactory"
        override val iid: Guid = guidOf("9f420906-111a-5465-91ee-bed14b3e7fec")

        fun from(inspectable: Inspectable): IPropertyMetadataFactory =
            inspectable.projectInterface(this, ::IPropertyMetadataFactory)

        operator fun invoke(inspectable: Inspectable): IPropertyMetadataFactory = from(inspectable)
    }
}
