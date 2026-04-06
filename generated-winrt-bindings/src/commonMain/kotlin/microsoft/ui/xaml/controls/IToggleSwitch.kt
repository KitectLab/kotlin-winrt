package microsoft.ui.xaml.controls

import dev.winrt.core.EventRegistrationToken
import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtDelegateBridge
import dev.winrt.core.WinRtDelegateHandle
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import kotlin.Unit
import microsoft.ui.xaml.DataTemplate
import microsoft.ui.xaml.RoutedEventArgs
import microsoft.ui.xaml.RoutedEventHandler
import microsoft.ui.xaml.controls.primitives.ToggleSwitchTemplateSettings

public interface IToggleSwitch {
  public var header: Inspectable

  public var headerTemplate: DataTemplate

  public var isOn: WinRtBoolean

  public var offContent: Inspectable

  public var offContentTemplate: DataTemplate

  public var onContent: Inspectable

  public var onContentTemplate: DataTemplate

  public val templateSettings: ToggleSwitchTemplateSettings

  public fun add_Toggled(handler: RoutedEventHandler): EventRegistrationToken

  public fun remove_Toggled(token: EventRegistrationToken)

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.IToggleSwitch"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.Controls.IToggleSwitch"

    override val iid: Guid = guidOf("1b17eeb1-74bf-5a83-8161-a86f0fdcdf24")

    public fun from(inspectable: Inspectable): IToggleSwitch = inspectable.projectInterface(this,
        ::IToggleSwitchProjection)

    public operator fun invoke(inspectable: Inspectable): IToggleSwitch = from(inspectable)
  }
}

private class IToggleSwitchProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IToggleSwitch {
  override var header: Inspectable
    get() = Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())
    set(value) {
      PlatformComInterop.invokeObjectSetter(pointer, 9, projectedObjectArgumentPointer(value,
          "Object", "cinterface(IInspectable)")).getOrThrow()
    }

  override var headerTemplate: DataTemplate
    get() = DataTemplate(PlatformComInterop.invokeObjectMethod(pointer, 10).getOrThrow())
    set(value) {
      PlatformComInterop.invokeObjectSetter(pointer, 11, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.DataTemplate",
          "rc(Microsoft.UI.Xaml.DataTemplate;{08fa70fa-ee75-5e92-a101-f52d0e1e9fab})")).getOrThrow()
    }

  override var isOn: WinRtBoolean
    get() = WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 6).getOrThrow())
    set(value) {
      PlatformComInterop.invokeBooleanSetter(pointer, 7, value.value).getOrThrow()
    }

  override var offContent: Inspectable
    get() = Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 16).getOrThrow())
    set(value) {
      PlatformComInterop.invokeObjectSetter(pointer, 17, projectedObjectArgumentPointer(value,
          "Object", "cinterface(IInspectable)")).getOrThrow()
    }

  override var offContentTemplate: DataTemplate
    get() = DataTemplate(PlatformComInterop.invokeObjectMethod(pointer, 18).getOrThrow())
    set(value) {
      PlatformComInterop.invokeObjectSetter(pointer, 19, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.DataTemplate",
          "rc(Microsoft.UI.Xaml.DataTemplate;{08fa70fa-ee75-5e92-a101-f52d0e1e9fab})")).getOrThrow()
    }

  override var onContent: Inspectable
    get() = Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 12).getOrThrow())
    set(value) {
      PlatformComInterop.invokeObjectSetter(pointer, 13, projectedObjectArgumentPointer(value,
          "Object", "cinterface(IInspectable)")).getOrThrow()
    }

  override var onContentTemplate: DataTemplate
    get() = DataTemplate(PlatformComInterop.invokeObjectMethod(pointer, 14).getOrThrow())
    set(value) {
      PlatformComInterop.invokeObjectSetter(pointer, 15, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.DataTemplate",
          "rc(Microsoft.UI.Xaml.DataTemplate;{08fa70fa-ee75-5e92-a101-f52d0e1e9fab})")).getOrThrow()
    }

  override val templateSettings: ToggleSwitchTemplateSettings
    get() = ToggleSwitchTemplateSettings(PlatformComInterop.invokeObjectMethod(pointer,
        20).getOrThrow())

  override fun add_Toggled(handler: RoutedEventHandler): EventRegistrationToken =
      EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 21,
      EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
      "Microsoft.UI.Xaml.RoutedEventHandler",
      "delegate({dae23d85-69ca-5bdf-805b-6161a3a215cc})")).getOrThrow())

  override fun remove_Toggled(token: EventRegistrationToken) {
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 22, token.toAbi()).getOrThrow()
  }

  public fun add_Toggled(callback: (Inspectable, RoutedEventArgs) -> Unit): WinRtDelegateHandle {
    val delegateHandle = WinRtDelegateBridge.createUnitDelegate(RoutedEventHandler.iid,
        listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
        dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args ->
        callback(dev.winrt.core.Inspectable(args[0] as ComPtr),
        microsoft.ui.xaml.RoutedEventArgs(args[1] as ComPtr)) }
    try {
      add_Toggled(RoutedEventHandler(delegateHandle.pointer))
    } catch (t: Throwable) {
      delegateHandle.close()
      throw t
    }
    return delegateHandle
  }
}
