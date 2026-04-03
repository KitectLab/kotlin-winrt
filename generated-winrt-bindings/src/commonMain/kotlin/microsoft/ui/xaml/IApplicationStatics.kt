package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

internal open class IApplicationStatics(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public fun get_Current(): Application = Application(PlatformComInterop.invokeObjectMethod(pointer,
      6).getOrThrow())

  public fun start(callback: ApplicationInitializationCallback) {
    PlatformComInterop.invokeObjectSetter(pointer, 7, callback.pointer).getOrThrow()
  }

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.IApplicationStatics"

    override val iid: Guid = guidOf("4e0d09f5-4358-512c-a987-503b52848e95")

    public fun from(inspectable: Inspectable): IApplicationStatics =
        inspectable.projectInterface(this, ::IApplicationStatics)
  }
}
