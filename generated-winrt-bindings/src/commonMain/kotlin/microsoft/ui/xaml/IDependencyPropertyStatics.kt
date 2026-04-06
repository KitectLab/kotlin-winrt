package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import windows.ui.xaml.interop.TypeName

internal open class IDependencyPropertyStatics(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public val unsetValue: Inspectable
    get() = Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

  public fun register(
    name: String,
    propertyType: TypeName,
    ownerType: TypeName,
    typeMetadata: PropertyMetadata,
  ): DependencyProperty = DependencyProperty(PlatformComInterop.invokeObjectMethodWithArgs(pointer,
      7, name, propertyType.toAbi(), ownerType.toAbi(), projectedObjectArgumentPointer(typeMetadata,
      "Microsoft.UI.Xaml.PropertyMetadata",
      "rc(Microsoft.UI.Xaml.PropertyMetadata;{b3644425-9464-5434-b0ae-aff8d3159fe1})")).getOrThrow())

  public fun registerAttached(
    name: String,
    propertyType: TypeName,
    ownerType: TypeName,
    defaultMetadata: PropertyMetadata,
  ): DependencyProperty = DependencyProperty(PlatformComInterop.invokeObjectMethodWithArgs(pointer,
      8, name, propertyType.toAbi(), ownerType.toAbi(),
      projectedObjectArgumentPointer(defaultMetadata, "Microsoft.UI.Xaml.PropertyMetadata",
      "rc(Microsoft.UI.Xaml.PropertyMetadata;{b3644425-9464-5434-b0ae-aff8d3159fe1})")).getOrThrow())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.IDependencyPropertyStatics"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.IDependencyPropertyStatics"

    override val iid: Guid = guidOf("61ddc651-0383-5d6f-98ce-5c046aaaaa8f")

    public fun from(inspectable: Inspectable): IDependencyPropertyStatics =
        inspectable.projectInterface(this, ::IDependencyPropertyStatics)

    public operator fun invoke(inspectable: Inspectable): IDependencyPropertyStatics =
        from(inspectable)
  }
}
