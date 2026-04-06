package microsoft.ui.xaml.media

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.guidOf
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComMethodResultKind
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.requireObject
import kotlin.String
import microsoft.ui.composition.ICompositionSupportsSystemBackdrop
import microsoft.ui.composition.systembackdrops.SystemBackdropConfiguration
import microsoft.ui.xaml.DependencyObject
import microsoft.ui.xaml.XamlRoot

public open class SystemBackdrop(
  pointer: ComPtr,
) : DependencyObject(pointer) {
  public constructor() : this(Companion.factoryCreateInstance().pointer)

  public fun onTargetConnected(connectedTarget: ICompositionSupportsSystemBackdrop,
      xamlRoot: XamlRoot) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithTwoObjectArgs(pointer, 6,
        projectedObjectArgumentPointer(connectedTarget,
        "Microsoft.UI.Composition.ICompositionSupportsSystemBackdrop",
        "{397dafe4-b6c2-5bb9-951d-f5707de8b7bc}"), projectedObjectArgumentPointer(xamlRoot,
        "Microsoft.UI.Xaml.XamlRoot",
        "rc(Microsoft.UI.Xaml.XamlRoot;{60cb215a-ad15-520a-8b01-4416824f0441})")).getOrThrow()
  }

  public fun onTargetDisconnected(disconnectedTarget: ICompositionSupportsSystemBackdrop) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 7,
        projectedObjectArgumentPointer(disconnectedTarget,
        "Microsoft.UI.Composition.ICompositionSupportsSystemBackdrop",
        "{397dafe4-b6c2-5bb9-951d-f5707de8b7bc}")).getOrThrow()
  }

  public fun onDefaultSystemBackdropConfigurationChanged(target: ICompositionSupportsSystemBackdrop,
      xamlRoot: XamlRoot) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithTwoObjectArgs(pointer, 8,
        projectedObjectArgumentPointer(target,
        "Microsoft.UI.Composition.ICompositionSupportsSystemBackdrop",
        "{397dafe4-b6c2-5bb9-951d-f5707de8b7bc}"), projectedObjectArgumentPointer(xamlRoot,
        "Microsoft.UI.Xaml.XamlRoot",
        "rc(Microsoft.UI.Xaml.XamlRoot;{60cb215a-ad15-520a-8b01-4416824f0441})")).getOrThrow()
  }

  public fun getDefaultSystemBackdropConfiguration(target: ICompositionSupportsSystemBackdrop,
      xamlRoot: XamlRoot): SystemBackdropConfiguration {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetDefaultSystemBackdropConfiguration")
    }
    return SystemBackdropConfiguration(PlatformComInterop.invokeMethodWithTwoObjectArgs(pointer, 6,
        ComMethodResultKind.OBJECT, projectedObjectArgumentPointer(target,
        "Microsoft.UI.Composition.ICompositionSupportsSystemBackdrop",
        "{397dafe4-b6c2-5bb9-951d-f5707de8b7bc}"), projectedObjectArgumentPointer(xamlRoot,
        "Microsoft.UI.Xaml.XamlRoot",
        "rc(Microsoft.UI.Xaml.XamlRoot;{60cb215a-ad15-520a-8b01-4416824f0441})")).getOrThrow().requireObject())
  }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Media.SystemBackdrop"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Media",
        "SystemBackdrop")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Media.ISystemBackdrop"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

    private fun factoryCreateInstance(): SystemBackdrop {
      return WinRtRuntime.compose(this, guidOf("1e07656b-fad2-5b29-913f-b6748bc45942"),
          guidOf("5aeed5c4-37ac-5852-b73f-1b76ebc3205f"), ::SystemBackdrop, 6, ComPtr.NULL)
    }
  }
}
