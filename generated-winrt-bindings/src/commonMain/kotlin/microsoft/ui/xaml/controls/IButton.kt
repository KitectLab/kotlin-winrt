package microsoft.ui.xaml.controls

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
import microsoft.ui.xaml.controls.primitives.FlyoutBase

public interface IButton {
  public var flyout: FlyoutBase

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.IButton"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.Controls.IButton"

    override val iid: Guid = guidOf("216c183d-d07a-5aa5-b8a4-0300a2683e87")

    public fun from(inspectable: Inspectable): IButton = inspectable.projectInterface(this,
        ::IButtonProjection)

    public operator fun invoke(inspectable: Inspectable): IButton = from(inspectable)
  }
}

private class IButtonProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IButton {
  override var flyout: FlyoutBase
    get() = FlyoutBase(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    set(value) {
      PlatformComInterop.invokeObjectSetter(pointer, 7, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.Controls.Primitives.FlyoutBase",
          "rc(Microsoft.UI.Xaml.Controls.Primitives.FlyoutBase;{bb6603bf-744d-5c31-a87d-744394634d77})")).getOrThrow()
    }
}
