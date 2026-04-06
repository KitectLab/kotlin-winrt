package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String

public interface IPropertyMetadata {
  public val createDefaultValueCallback: CreateDefaultValueCallback

  public val defaultValue: Inspectable

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.IPropertyMetadata"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.IPropertyMetadata"

    override val iid: Guid = guidOf("b3644425-9464-5434-b0ae-aff8d3159fe1")

    public fun from(inspectable: Inspectable): IPropertyMetadata =
        inspectable.projectInterface(this, ::IPropertyMetadataProjection)

    public operator fun invoke(inspectable: Inspectable): IPropertyMetadata = from(inspectable)
  }
}

private class IPropertyMetadataProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IPropertyMetadata {
  override val createDefaultValueCallback: CreateDefaultValueCallback
    get() = CreateDefaultValueCallback(PlatformComInterop.invokeObjectMethod(pointer,
        7).getOrThrow())

  override val defaultValue: Inspectable
    get() = Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
}
