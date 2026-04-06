package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import kotlin.String

internal open class IPropertyMetadataFactory(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.IPropertyMetadataFactory"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.IPropertyMetadataFactory"

    override val iid: Guid = guidOf("9f420906-111a-5465-91ee-bed14b3e7fec")

    public fun from(inspectable: Inspectable): IPropertyMetadataFactory =
        inspectable.projectInterface(this, ::IPropertyMetadataFactory)

    public operator fun invoke(inspectable: Inspectable): IPropertyMetadataFactory =
        from(inspectable)
  }
}
