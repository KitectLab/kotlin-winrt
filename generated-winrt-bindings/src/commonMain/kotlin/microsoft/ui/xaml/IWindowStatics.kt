package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

internal open class IWindowStatics(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public val current: Window
    get() = Window(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

  public fun get_Current(): Window = Window(PlatformComInterop.invokeObjectMethod(pointer,
      6).getOrThrow())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.IWindowStatics"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.IWindowStatics"

    override val iid: Guid = guidOf("8cc985e3-a41a-5df4-b531-d3a1788d86c5")

    public fun from(inspectable: Inspectable): IWindowStatics = inspectable.projectInterface(this,
        ::IWindowStatics)

    public operator fun invoke(inspectable: Inspectable): IWindowStatics = from(inspectable)
  }
}
