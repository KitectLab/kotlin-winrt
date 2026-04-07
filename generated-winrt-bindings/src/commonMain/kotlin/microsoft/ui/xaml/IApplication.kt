package microsoft.ui.xaml

import dev.winrt.core.EventRegistrationToken
import dev.winrt.core.Inspectable
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

public interface IApplication {
  public val debugSettings: DebugSettings

  public var focusVisualKind: FocusVisualKind

  public var highContrastAdjustment: ApplicationHighContrastAdjustment

  public var requestedTheme: ApplicationTheme

  public var resources: ResourceDictionary

  public fun add_UnhandledException(handler: UnhandledExceptionEventHandler): EventRegistrationToken

  public fun remove_UnhandledException(token: EventRegistrationToken)

  public fun exit()

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.IApplication"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.IApplication"

    override val iid: Guid = guidOf("06a8f4e7-1146-55af-820d-ebd55643b021")

    public fun from(inspectable: Inspectable): IApplication = inspectable.projectInterface(this,
        ::IApplicationProjection)

    public operator fun invoke(inspectable: Inspectable): IApplication = from(inspectable)
  }
}

private class IApplicationProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IApplication {
  override val debugSettings: DebugSettings
    get() = DebugSettings(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())

  override var focusVisualKind: FocusVisualKind
    get() = FocusVisualKind.fromValue(PlatformComInterop.invokeInt32Method(pointer,
        11).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 12, value.value).getOrThrow()
    }

  override var highContrastAdjustment: ApplicationHighContrastAdjustment
    get() =
        ApplicationHighContrastAdjustment.fromValue(PlatformComInterop.invokeUInt32Method(pointer,
        13).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithUInt32Arg(pointer, 14, value.value).getOrThrow()
    }

  override var requestedTheme: ApplicationTheme
    get() = ApplicationTheme.fromValue(PlatformComInterop.invokeInt32Method(pointer,
        9).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 10, value.value).getOrThrow()
    }

  override var resources: ResourceDictionary
    get() = ResourceDictionary(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 7, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.ResourceDictionary",
          "rc(Microsoft.UI.Xaml.ResourceDictionary;{1b690975-a710-5783-a6e1-15836f6186c2})")).getOrThrow()
    }

  override fun add_UnhandledException(handler: UnhandledExceptionEventHandler):
      EventRegistrationToken =
      EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 15,
      EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
      "Microsoft.UI.Xaml.UnhandledExceptionEventHandler",
      "delegate({3427c1b6-5eca-5631-84b8-5bae732fb67f})")).getOrThrow())

  override fun remove_UnhandledException(token: EventRegistrationToken) {
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 16, token.toAbi()).getOrThrow()
  }

  override fun exit() {
    PlatformComInterop.invokeUnitMethod(pointer, 17).getOrThrow()
  }

  public fun add_UnhandledException(callback: (Inspectable, UnhandledExceptionEventArgs) -> Unit):
      WinRtDelegateHandle {
    val delegateHandle = WinRtDelegateBridge.createUnitDelegate(UnhandledExceptionEventHandler.iid,
        listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
        dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args ->
        callback(dev.winrt.core.Inspectable(args[0] as ComPtr),
        microsoft.ui.xaml.UnhandledExceptionEventArgs(args[1] as ComPtr)) }
    try {
      add_UnhandledException(UnhandledExceptionEventHandler(delegateHandle.pointer))
    } catch (t: Throwable) {
      delegateHandle.close()
      throw t
    }
    return delegateHandle
  }
}
