package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComMethodResultKind
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.requireObject
import kotlin.String

internal open class IPropertyMetadataStatics(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public fun create(defaultValue: Inspectable): PropertyMetadata =
      PropertyMetadata(PlatformComInterop.invokeObjectMethodWithObjectArg(pointer, 6,
      projectedObjectArgumentPointer(defaultValue, "Object",
      "cinterface(IInspectable)")).getOrThrow())

  public fun create(defaultValue: Inspectable, propertyChangedCallback: PropertyChangedCallback):
      PropertyMetadata = PropertyMetadata(PlatformComInterop.invokeMethodWithTwoObjectArgs(pointer,
      7, ComMethodResultKind.OBJECT, projectedObjectArgumentPointer(defaultValue, "Object",
      "cinterface(IInspectable)"), projectedObjectArgumentPointer(propertyChangedCallback,
      "Microsoft.UI.Xaml.PropertyChangedCallback",
      "delegate({5fd9243a-2422-53c9-8d6f-f1ba1a0bba9a})")).getOrThrow().requireObject())

  public fun create(createDefaultValueCallback: CreateDefaultValueCallback): PropertyMetadata =
      PropertyMetadata(PlatformComInterop.invokeObjectMethodWithObjectArg(pointer, 8,
      projectedObjectArgumentPointer(createDefaultValueCallback,
      "Microsoft.UI.Xaml.CreateDefaultValueCallback",
      "delegate({7f808c05-2ac4-5ad9-ac8a-26890333d81e})")).getOrThrow())

  public fun create(createDefaultValueCallback: CreateDefaultValueCallback,
      propertyChangedCallback: PropertyChangedCallback): PropertyMetadata =
      PropertyMetadata(PlatformComInterop.invokeMethodWithTwoObjectArgs(pointer, 9,
      ComMethodResultKind.OBJECT, projectedObjectArgumentPointer(createDefaultValueCallback,
      "Microsoft.UI.Xaml.CreateDefaultValueCallback",
      "delegate({7f808c05-2ac4-5ad9-ac8a-26890333d81e})"),
      projectedObjectArgumentPointer(propertyChangedCallback,
      "Microsoft.UI.Xaml.PropertyChangedCallback",
      "delegate({5fd9243a-2422-53c9-8d6f-f1ba1a0bba9a})")).getOrThrow().requireObject())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.IPropertyMetadataStatics"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.IPropertyMetadataStatics"

    override val iid: Guid = guidOf("37b8add4-7a4a-5cf7-a174-235182cd082e")

    public fun from(inspectable: Inspectable): IPropertyMetadataStatics =
        inspectable.projectInterface(this, ::IPropertyMetadataStatics)

    public operator fun invoke(inspectable: Inspectable): IPropertyMetadataStatics =
        from(inspectable)
  }
}
