package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

public open class IApplicationOverrides(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public fun onLaunched(args: LaunchActivatedEventArgs) {
    PlatformComInterop.invokeObjectSetter(pointer, 6, args.pointer).getOrThrow()
  }

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.IApplicationOverrides"

    override val iid: Guid = guidOf("a33e81ef-c665-503b-8827-d27ef1720a06")

    public fun from(inspectable: Inspectable): IApplicationOverrides =
        inspectable.projectInterface(this, ::IApplicationOverrides)
  }
}
