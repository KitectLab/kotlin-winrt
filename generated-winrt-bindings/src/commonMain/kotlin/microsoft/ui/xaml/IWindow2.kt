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
import microsoft.ui.windowing.AppWindow
import microsoft.ui.xaml.media.SystemBackdrop

internal interface IWindow2 {
  public val appWindow: AppWindow

  public var systemBackdrop: SystemBackdrop

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.IWindow2"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.IWindow2"

    override val iid: Guid = guidOf("42febaa5-1c32-522a-a591-57618c6f665d")

    public fun from(inspectable: Inspectable): IWindow2 = inspectable.projectInterface(this,
        ::IWindow2Projection)

    public operator fun invoke(inspectable: Inspectable): IWindow2 = from(inspectable)
  }
}

private class IWindow2Projection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IWindow2 {
  override val appWindow: AppWindow
    get() = AppWindow(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())

  override var systemBackdrop: SystemBackdrop
    get() = SystemBackdrop(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 7, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.Media.SystemBackdrop",
          "rc(Microsoft.UI.Xaml.Media.SystemBackdrop;{5aeed5c4-37ac-5852-b73f-1b76ebc3205f})")).getOrThrow()
    }
}
