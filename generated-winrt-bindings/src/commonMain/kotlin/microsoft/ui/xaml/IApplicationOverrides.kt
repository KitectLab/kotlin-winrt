package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

public interface IApplicationOverrides {
  public fun onLaunched(args: LaunchActivatedEventArgs)

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.IApplicationOverrides"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.IApplicationOverrides"

    override val iid: Guid = guidOf("a33e81ef-c665-503b-8827-d27ef1720a06")

    public fun from(inspectable: Inspectable): IApplicationOverrides = inspectable.projectInterface(
        this, ::IApplicationOverridesProjection)
  }
}

private class IApplicationOverridesProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer), IApplicationOverrides {
  override fun onLaunched(args: LaunchActivatedEventArgs) {
    PlatformComInterop.invokeObjectSetter(pointer, 6, args.pointer).getOrThrow()
  }
}
