package microsoft.ui.xaml

import dev.winrt.core.EventRegistrationToken
import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtDelegateBridge
import dev.winrt.core.WinRtDelegateHandle
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.guidOf
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComPtr
import dev.winrt.kom.ComStructValue
import dev.winrt.kom.PlatformComInterop
import java.lang.AutoCloseable
import kotlin.String
import kotlin.Unit
import kotlin.collections.MutableMap
import windows.foundation.TypedEventHandler
import windows.foundation.Uri

public open class Application(
  pointer: ComPtr,
) : Inspectable(pointer),
    IApplication {
  private val backing_DispatcherShutdownMode: RuntimeProperty<DispatcherShutdownMode> =
      RuntimeProperty<DispatcherShutdownMode>(DispatcherShutdownMode.fromValue(0))

  public var dispatcherShutdownMode: DispatcherShutdownMode
    get() {
      if (pointer.isNull) {
        return backing_DispatcherShutdownMode.get()
      }
      return DispatcherShutdownMode.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          6).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_DispatcherShutdownMode.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 7, value.value).getOrThrow()
    }

  private val backing_DebugSettings: RuntimeProperty<DebugSettings> =
      RuntimeProperty<DebugSettings>(DebugSettings(ComPtr.NULL))

  override val debugSettings: DebugSettings
    get() {
      if (pointer.isNull) {
        return backing_DebugSettings.get()
      }
      return DebugSettings(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())
    }

  private val backing_FocusVisualKind: RuntimeProperty<FocusVisualKind> =
      RuntimeProperty<FocusVisualKind>(FocusVisualKind.fromValue(0))

  override var focusVisualKind: FocusVisualKind
    get() {
      if (pointer.isNull) {
        return backing_FocusVisualKind.get()
      }
      return FocusVisualKind.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          11).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_FocusVisualKind.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 12, value.value).getOrThrow()
    }

  private val backing_HighContrastAdjustment: RuntimeProperty<ApplicationHighContrastAdjustment> =
      RuntimeProperty<ApplicationHighContrastAdjustment>(ApplicationHighContrastAdjustment.fromValue(0u))

  override var highContrastAdjustment: ApplicationHighContrastAdjustment
    get() {
      if (pointer.isNull) {
        return backing_HighContrastAdjustment.get()
      }
      return ApplicationHighContrastAdjustment.fromValue(PlatformComInterop.invokeUInt32Method(pointer,
          13).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_HighContrastAdjustment.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithUInt32Arg(pointer, 14, value.value).getOrThrow()
    }

  private val backing_RequestedTheme: RuntimeProperty<ApplicationTheme> =
      RuntimeProperty<ApplicationTheme>(ApplicationTheme.fromValue(0))

  override var requestedTheme: ApplicationTheme
    get() {
      if (pointer.isNull) {
        return backing_RequestedTheme.get()
      }
      return ApplicationTheme.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          9).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_RequestedTheme.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 10, value.value).getOrThrow()
    }

  private val backing_Resources: RuntimeProperty<ResourceDictionary> =
      RuntimeProperty<ResourceDictionary>(ResourceDictionary(ComPtr.NULL))

  override var resources: ResourceDictionary
    get() {
      if (pointer.isNull) {
        return backing_Resources.get()
      }
      return ResourceDictionary(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Resources.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 7, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_Current: RuntimeProperty<Application> =
      RuntimeProperty<Application>(Application(ComPtr.NULL))

  public val current: Application
    get() = backing_Current.get()

  private val resourceManagerRequestedEventSlot: ResourceManagerRequestedEvent =
      ResourceManagerRequestedEvent()

  public val resourceManagerRequestedEvent: ResourceManagerRequestedEvent
    get() = resourceManagerRequestedEventSlot

  public constructor() : this(Companion.factoryCreateInstance().pointer)

  public
      fun add_ResourceManagerRequested(handler: TypedEventHandler<Inspectable, ResourceManagerRequestedEventArgs>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 6,
        EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Object, Microsoft.UI.Xaml.ResourceManagerRequestedEventArgs>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};cinterface(IInspectable);rc(Microsoft.UI.Xaml.ResourceManagerRequestedEventArgs;{c35f4cf1-fcd6-5c6b-9be2-4cfaefb68b2a}))")).getOrThrow())
  }

  public fun remove_ResourceManagerRequested(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 7, token.toAbi()).getOrThrow()
  }

  public fun onLaunched(args: LaunchActivatedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 6, projectedObjectArgumentPointer(args,
        "Microsoft.UI.Xaml.LaunchActivatedEventArgs",
        "rc(Microsoft.UI.Xaml.LaunchActivatedEventArgs;{d505cea9-1bcb-5b29-a8be-944e00f06f78})")).getOrThrow()
  }

  override fun add_UnhandledException(handler: UnhandledExceptionEventHandler):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 15,
        EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.UnhandledExceptionEventHandler",
        "delegate({3427c1b6-5eca-5631-84b8-5bae732fb67f})")).getOrThrow())
  }

  override fun remove_UnhandledException(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 16, token.toAbi()).getOrThrow()
  }

  override fun exit() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 17).getOrThrow()
  }

  public fun start() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 6).getOrThrow()
  }

  public fun getLaunchCount(): UInt32 {
    if (pointer.isNull) {
      return UInt32(0u)
    }
    return UInt32(PlatformComInterop.invokeUInt32Method(pointer, 7).getOrThrow())
  }

  public inner class ResourceManagerRequestedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public
        fun subscribe(handler: TypedEventHandler<Inspectable, ResourceManagerRequestedEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 6,
        handler.pointer).getOrThrow())

    public
        fun subscribeScoped(handler: TypedEventHandler<Inspectable, ResourceManagerRequestedEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (ComPtr, ResourceManagerRequestedEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(args[0] as ComPtr,
          ResourceManagerRequestedEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<Inspectable, ResourceManagerRequestedEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (ComPtr, ResourceManagerRequestedEventArgs) -> Unit):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator
        fun plusAssign(handler: TypedEventHandler<Inspectable, ResourceManagerRequestedEventArgs>) {
      subscribe(handler)
    }

    public operator
        fun invoke(handler: TypedEventHandler<Inspectable, ResourceManagerRequestedEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (ComPtr, ResourceManagerRequestedEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 7, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Application"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml", "Application")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.IApplication"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

    private val statics: IApplicationStatics by lazy { WinRtRuntime.projectActivationFactory(this,
        IApplicationStatics, ::IApplicationStatics) }

    public val current: Application
      get() = statics.current

    private fun factoryCreateInstance(): Application {
      return WinRtRuntime.compose(this, guidOf("9fd96657-5294-5a65-a1db-4fea143597da"),
          guidOf("06a8f4e7-1146-55af-820d-ebd55643b021"), ::Application, 6, ComPtr.NULL)
    }

    public fun start(callback: ApplicationInitializationCallback) {
      statics.start(callback)
    }

    public fun start(callback: (ApplicationInitializationCallbackParams) -> Unit):
        WinRtDelegateHandle {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(ApplicationInitializationCallback.iid,
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args ->
          callback(microsoft.ui.xaml.ApplicationInitializationCallbackParams(args[0] as ComPtr)) }
      try {
        statics.start(ApplicationInitializationCallback(delegateHandle.pointer))
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
      return delegateHandle
    }

    public fun loadComponent(component: Inspectable, resourceLocator: Uri) {
      statics.loadComponent(component, resourceLocator)
    }
  }
}
