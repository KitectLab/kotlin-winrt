package microsoft.ui.xaml

import dev.winrt.core.EventRegistrationToken
import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtDelegateBridge
import dev.winrt.core.WinRtDelegateHandle
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import java.lang.AutoCloseable
import kotlin.String
import kotlin.Unit
import kotlin.collections.MutableMap
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.Uuid
import microsoft.ui.composition.Compositor
import microsoft.ui.dispatching.DispatcherQueue
import microsoft.ui.windowing.AppWindow
import microsoft.ui.xaml.media.SystemBackdrop
import windows.foundation.Rect
import windows.foundation.TypedEventHandler
import windows.ui.core.CoreDispatcher
import windows.ui.core.CoreWindow

public open class Window(
  pointer: ComPtr,
) : Inspectable(pointer) {
  private val backing_AppWindow: RuntimeProperty<AppWindow> =
      RuntimeProperty<AppWindow>(AppWindow(ComPtr.NULL))

  public val appWindow: AppWindow
    get() {
      if (pointer.isNull) {
        return backing_AppWindow.get()
      }
      return AppWindow(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())
    }

  private val backing_SystemBackdrop: RuntimeProperty<SystemBackdrop> =
      RuntimeProperty<SystemBackdrop>(SystemBackdrop(ComPtr.NULL))

  public var systemBackdrop: SystemBackdrop
    get() {
      if (pointer.isNull) {
        return backing_SystemBackdrop.get()
      }
      return SystemBackdrop(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_SystemBackdrop.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 7, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_Bounds: RuntimeProperty<Rect> = RuntimeProperty<Rect>(Rect(ComPtr.NULL))

  public val bounds: Rect
    get() {
      if (pointer.isNull) {
        return backing_Bounds.get()
      }
      return Rect(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    }

  private val backing_Compositor: RuntimeProperty<Compositor> =
      RuntimeProperty<Compositor>(Compositor(ComPtr.NULL))

  public val compositor: Compositor
    get() {
      if (pointer.isNull) {
        return backing_Compositor.get()
      }
      return Compositor(PlatformComInterop.invokeObjectMethod(pointer, 11).getOrThrow())
    }

  private val backing_Content: RuntimeProperty<UIElement> =
      RuntimeProperty<UIElement>(UIElement(ComPtr.NULL))

  public var content: UIElement
    get() {
      if (pointer.isNull) {
        return backing_Content.get()
      }
      return UIElement(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Content.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 9, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_CoreWindow: RuntimeProperty<CoreWindow> =
      RuntimeProperty<CoreWindow>(CoreWindow(ComPtr.NULL))

  public val coreWindow: CoreWindow
    get() {
      if (pointer.isNull) {
        return backing_CoreWindow.get()
      }
      return CoreWindow(PlatformComInterop.invokeObjectMethod(pointer, 10).getOrThrow())
    }

  private val backing_Dispatcher: RuntimeProperty<CoreDispatcher> =
      RuntimeProperty<CoreDispatcher>(CoreDispatcher(ComPtr.NULL))

  public val dispatcher: CoreDispatcher
    get() {
      if (pointer.isNull) {
        return backing_Dispatcher.get()
      }
      return CoreDispatcher(PlatformComInterop.invokeObjectMethod(pointer, 12).getOrThrow())
    }

  private val backing_DispatcherQueue: RuntimeProperty<DispatcherQueue> =
      RuntimeProperty<DispatcherQueue>(DispatcherQueue(ComPtr.NULL))

  public val dispatcherQueue: DispatcherQueue
    get() {
      if (pointer.isNull) {
        return backing_DispatcherQueue.get()
      }
      return DispatcherQueue(PlatformComInterop.invokeObjectMethod(pointer, 13).getOrThrow())
    }

  private val backing_ExtendsContentIntoTitleBar: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  public var extendsContentIntoTitleBar: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_ExtendsContentIntoTitleBar.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 16).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_ExtendsContentIntoTitleBar.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 17, value.value).getOrThrow()
    }

  private val backing_Title: RuntimeProperty<String> = RuntimeProperty<String>("")

  public var title: String
    get() {
      if (pointer.isNull) {
        return backing_Title.get()
      }
      return PlatformComInterop.invokeHStringMethod(pointer, 14).getOrThrow().use {
          it.toKotlinString() }
    }
    set(value) {
      if (pointer.isNull) {
        backing_Title.set(value)
        return
      }
      PlatformComInterop.invokeStringSetter(pointer, 15, value).getOrThrow()
    }

  private val backing_Visible: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  public val visible: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_Visible.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 7).getOrThrow())
    }

  private val backing_Current: RuntimeProperty<Window> =
      RuntimeProperty<Window>(Window(ComPtr.NULL))

  public val current: Window
    get() = backing_Current.get()

  private val backing_IsVisible: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  public val isVisible: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsVisible.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 8).getOrThrow())
    }

  private val backing_CreatedAt: RuntimeProperty<Instant> =
      RuntimeProperty<Instant>(Instant.fromEpochSeconds(0))

  public val createdAt: Instant
    get() {
      if (pointer.isNull) {
        return backing_CreatedAt.get()
      }
      return Instant.fromEpochSeconds((PlatformComInterop.invokeInt64Getter(pointer,
          10).getOrThrow() - 116444736000000000) / 10000000L,
          ((PlatformComInterop.invokeInt64Getter(pointer, 10).getOrThrow() - 116444736000000000) %
          10000000L * 100).toInt())
    }

  private val backing_Lifetime: RuntimeProperty<Duration> =
      RuntimeProperty<Duration>(Duration.parse("0s"))

  public val lifetime: Duration
    get() {
      if (pointer.isNull) {
        return backing_Lifetime.get()
      }
      return Duration(PlatformComInterop.invokeInt64Getter(pointer, 11).getOrThrow())
    }

  private val backing_LastToken: RuntimeProperty<EventRegistrationToken> =
      RuntimeProperty<EventRegistrationToken>(EventRegistrationToken(0))

  public val lastToken: EventRegistrationToken
    get() {
      if (pointer.isNull) {
        return backing_LastToken.get()
      }
      return EventRegistrationToken(PlatformComInterop.invokeInt64Getter(pointer, 12).getOrThrow())
    }

  private val backing_StableId: RuntimeProperty<Uuid> =
      RuntimeProperty<Uuid>(Uuid.parse("00000000000000000000000000000000"))

  public val stableId: Uuid
    get() {
      if (pointer.isNull) {
        return backing_StableId.get()
      }
      return Uuid.parse(PlatformComInterop.invokeGuidGetter(pointer, 9).getOrThrow().toString())
    }

  private val backing_OptionalTitle: RuntimeProperty<String?> = RuntimeProperty<String?>(null)

  public val optionalTitle: String?
    get() {
      if (pointer.isNull) {
        return backing_OptionalTitle.get()
      }
      return if (pointer.isNull) null else PlatformComInterop.invokeHStringMethod(pointer,
          14).getOrThrow().use { value -> value.takeUnless { it.isNull }?.toKotlinString() }
    }

  private val activatedEventSlot: ActivatedEvent = ActivatedEvent()

  public val activatedEvent: ActivatedEvent
    get() = activatedEventSlot

  private val closedEventSlot: ClosedEvent = ClosedEvent()

  public val closedEvent: ClosedEvent
    get() = closedEventSlot

  private val sizeChangedEventSlot: SizeChangedEvent = SizeChangedEvent()

  public val sizeChangedEvent: SizeChangedEvent
    get() = sizeChangedEventSlot

  private val visibilityChangedEventSlot: VisibilityChangedEvent = VisibilityChangedEvent()

  public val visibilityChangedEvent: VisibilityChangedEvent
    get() = visibilityChangedEventSlot

  public constructor() : this(Companion.factoryCreateInstance().pointer)

  public fun remove_Activated(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 19, token.value).getOrThrow()
  }

  public fun remove_Closed(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 21, token.value).getOrThrow()
  }

  public fun remove_SizeChanged(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 23, token.value).getOrThrow()
  }

  public fun remove_VisibilityChanged(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 25, token.value).getOrThrow()
  }

  public fun activate() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 26).getOrThrow()
  }

  public fun close() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 27).getOrThrow()
  }

  public fun setTitleBar(titleBar: UIElement) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 28, (titleBar as
        Inspectable).pointer).getOrThrow()
  }

  public inner class ActivatedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<Inspectable, WindowActivatedEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 18,
        handler.pointer).getOrThrow())

    public fun subscribeScoped(handler: TypedEventHandler<Inspectable, WindowActivatedEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (ComPtr, WindowActivatedEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("00000000-0000-0000-0000-000000000000"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(args[0] as ComPtr,
          WindowActivatedEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<Inspectable, WindowActivatedEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (ComPtr, WindowActivatedEventArgs) -> Unit): AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator
        fun plusAssign(handler: TypedEventHandler<Inspectable, WindowActivatedEventArgs>) {
      subscribe(handler)
    }

    public operator fun invoke(handler: TypedEventHandler<Inspectable, WindowActivatedEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (ComPtr, WindowActivatedEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 19, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class ClosedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<Inspectable, WindowEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 20,
        handler.pointer).getOrThrow())

    public fun subscribeScoped(handler: TypedEventHandler<Inspectable, WindowEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (ComPtr, WindowEventArgs) -> Unit): EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("00000000-0000-0000-0000-000000000000"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(args[0] as ComPtr,
          WindowEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<Inspectable, WindowEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (ComPtr, WindowEventArgs) -> Unit): AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator fun plusAssign(handler: TypedEventHandler<Inspectable, WindowEventArgs>) {
      subscribe(handler)
    }

    public operator fun invoke(handler: TypedEventHandler<Inspectable, WindowEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (ComPtr, WindowEventArgs) -> Unit): EventRegistrationToken =
        subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 21, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class SizeChangedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<Inspectable, WindowSizeChangedEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 22,
        handler.pointer).getOrThrow())

    public fun subscribeScoped(handler: TypedEventHandler<Inspectable, WindowSizeChangedEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (ComPtr, WindowSizeChangedEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("00000000-0000-0000-0000-000000000000"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(args[0] as ComPtr,
          WindowSizeChangedEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<Inspectable, WindowSizeChangedEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (ComPtr, WindowSizeChangedEventArgs) -> Unit):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator
        fun plusAssign(handler: TypedEventHandler<Inspectable, WindowSizeChangedEventArgs>) {
      subscribe(handler)
    }

    public operator fun invoke(handler: TypedEventHandler<Inspectable, WindowSizeChangedEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (ComPtr, WindowSizeChangedEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 23, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class VisibilityChangedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<Inspectable, WindowVisibilityChangedEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 24,
        handler.pointer).getOrThrow())

    public
        fun subscribeScoped(handler: TypedEventHandler<Inspectable, WindowVisibilityChangedEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (ComPtr, WindowVisibilityChangedEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("00000000-0000-0000-0000-000000000000"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(args[0] as ComPtr,
          WindowVisibilityChangedEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<Inspectable, WindowVisibilityChangedEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (ComPtr, WindowVisibilityChangedEventArgs) -> Unit):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator
        fun plusAssign(handler: TypedEventHandler<Inspectable, WindowVisibilityChangedEventArgs>) {
      subscribe(handler)
    }

    public operator
        fun invoke(handler: TypedEventHandler<Inspectable, WindowVisibilityChangedEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (ComPtr, WindowVisibilityChangedEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 25, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Window"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml", "Window")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.IWindow"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

    private val statics: IWindowStatics by lazy { WinRtRuntime.projectActivationFactory(this,
        IWindowStatics, ::IWindowStatics) }

    public val current: Window
      get() = statics.current

    private fun factoryCreateInstance(): Window {
      return WinRtRuntime.compose(this, guidOf("f0441536-afef-5222-918f-324a9b2dec75"),
          guidOf("61f0ec79-5d52-56b5-86fb-40fa4af288b0"), ::Window, 6, ComPtr.NULL)
    }
  }
}
