package microsoft.ui.xaml.controls

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String

public interface IXamlControlsResources {
  public var useCompactResources: WinRtBoolean

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.IXamlControlsResources"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.Controls.IXamlControlsResources"

    override val iid: Guid = guidOf("918ca043-f42c-5805-861b-62d6d1d0c162")

    public fun from(inspectable: Inspectable): IXamlControlsResources =
        inspectable.projectInterface(this, ::IXamlControlsResourcesProjection)

    public operator fun invoke(inspectable: Inspectable): IXamlControlsResources = from(inspectable)
  }
}

private class IXamlControlsResourcesProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IXamlControlsResources {
  override var useCompactResources: WinRtBoolean
    get() = WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 6).getOrThrow())
    set(value) {
      PlatformComInterop.invokeBooleanSetter(pointer, 7, value.value).getOrThrow()
    }
}
