package microsoft.ui.xaml.controls

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import microsoft.ui.xaml.DependencyProperty
import microsoft.ui.xaml.controls.primitives.ButtonBase
import microsoft.ui.xaml.controls.primitives.FlyoutBase

public open class Button(
  pointer: ComPtr,
) : ButtonBase(pointer),
    IButton {
  private val backing_Flyout: RuntimeProperty<FlyoutBase> =
      RuntimeProperty<FlyoutBase>(FlyoutBase(ComPtr.NULL))

  override var flyout: FlyoutBase
    get() {
      if (pointer.isNull) {
        return backing_Flyout.get()
      }
      return FlyoutBase(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Flyout.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 7, (value as
          Inspectable).pointer).getOrThrow()
    }

  public constructor() : this(Companion.factoryCreateInstance().pointer)

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.Button"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "Button")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IButton"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

    private val statics: IButtonStatics by lazy { WinRtRuntime.projectActivationFactory(this,
        IButtonStatics, ::IButtonStatics) }

    public val flyoutProperty: DependencyProperty
      get() = statics.flyoutProperty

    private fun factoryCreateInstance(): Button {
      return WinRtRuntime.compose(this, guidOf("fe393422-d91c-57b1-9a9c-2c7e3f41f77c"),
          guidOf("216c183d-d07a-5aa5-b8a4-0300a2683e87"), ::Button, 6, ComPtr.NULL)
    }
  }
}
