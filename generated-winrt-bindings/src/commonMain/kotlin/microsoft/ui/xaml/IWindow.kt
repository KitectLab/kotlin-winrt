package microsoft.ui.xaml

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
import java.lang.AutoCloseable
import kotlin.String
import kotlin.Unit
import kotlin.collections.MutableMap
import microsoft.ui.composition.Compositor
import microsoft.ui.dispatching.DispatcherQueue
import windows.foundation.Rect
import windows.foundation.TypedEventHandler
import windows.ui.core.CoreDispatcher
import windows.ui.core.CoreWindow

public interface IWindow {
  public val bounds: Rect

  public val compositor: Compositor

  public var content: UIElement

  public val coreWindow: CoreWindow

  public val dispatcher: CoreDispatcher

  public val dispatcherQueue: DispatcherQueue

  public var extendsContentIntoTitleBar: WinRtBoolean

  public var title: String

  public val visible: WinRtBoolean

  public fun add_Activated(handler: TypedEventHandler<Inspectable, WindowActivatedEventArgs>):
      EventRegistrationToken

  public fun remove_Activated(token: EventRegistrationToken)

  public fun add_Closed(handler: TypedEventHandler<Inspectable, WindowEventArgs>):
      EventRegistrationToken

  public fun remove_Closed(token: EventRegistrationToken)

  public fun add_SizeChanged(handler: TypedEventHandler<Inspectable, WindowSizeChangedEventArgs>):
      EventRegistrationToken

  public fun remove_SizeChanged(token: EventRegistrationToken)

  public
      fun add_VisibilityChanged(handler: TypedEventHandler<Inspectable, WindowVisibilityChangedEventArgs>):
      EventRegistrationToken

  public fun remove_VisibilityChanged(token: EventRegistrationToken)

  public fun activate()

  public fun close()

  public fun setTitleBar(titleBar: UIElement)

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.IWindow"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.IWindow"

    override val iid: Guid = guidOf("61f0ec79-5d52-56b5-86fb-40fa4af288b0")

    public fun from(inspectable: Inspectable): IWindow = inspectable.projectInterface(this,
        ::IWindowProjection)

    public operator fun invoke(inspectable: Inspectable): IWindow = from(inspectable)
  }
}

private class IWindowProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IWindow {
  override val bounds: Rect
    get() = Rect.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 6,
        Rect.ABI_LAYOUT).getOrThrow())

  override val compositor: Compositor
    get() = Compositor(PlatformComInterop.invokeObjectMethod(pointer, 11).getOrThrow())

  override var content: UIElement
    get() = UIElement(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 9, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.UIElement",
          "rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b})")).getOrThrow()
    }

  override val coreWindow: CoreWindow
    get() = CoreWindow(PlatformComInterop.invokeObjectMethod(pointer, 10).getOrThrow())

  override val dispatcher: CoreDispatcher
    get() = CoreDispatcher(PlatformComInterop.invokeObjectMethod(pointer, 12).getOrThrow())

  override val dispatcherQueue: DispatcherQueue
    get() = DispatcherQueue(PlatformComInterop.invokeObjectMethod(pointer, 13).getOrThrow())

  override var extendsContentIntoTitleBar: WinRtBoolean
    get() = WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 16).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithUInt32Arg(pointer, 17, if (value.value) 1u else 0u).getOrThrow()
    }

  override var title: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 14).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }
    set(value) {
      PlatformComInterop.invokeUnitMethodWithStringArg(pointer, 15, value).getOrThrow()
    }

  override val visible: WinRtBoolean
    get() = WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 7).getOrThrow())

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

  override fun add_Activated(handler: TypedEventHandler<Inspectable, WindowActivatedEventArgs>):
      EventRegistrationToken =
      EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 18,
      EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
      "Windows.Foundation.TypedEventHandler`2<Object, Microsoft.UI.Xaml.WindowActivatedEventArgs>",
      "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};cinterface(IInspectable);rc(Microsoft.UI.Xaml.WindowActivatedEventArgs;{c723a5ea-82c4-5dd6-861b-70ef573b88d6}))")).getOrThrow())

  override fun remove_Activated(token: EventRegistrationToken) {
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 19, token.toAbi()).getOrThrow()
  }

  override fun add_Closed(handler: TypedEventHandler<Inspectable, WindowEventArgs>):
      EventRegistrationToken =
      EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 20,
      EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
      "Windows.Foundation.TypedEventHandler`2<Object, Microsoft.UI.Xaml.WindowEventArgs>",
      "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};cinterface(IInspectable);rc(Microsoft.UI.Xaml.WindowEventArgs;{1140827c-fe0a-5268-bc2b-f4492c2ccb49}))")).getOrThrow())

  override fun remove_Closed(token: EventRegistrationToken) {
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 21, token.toAbi()).getOrThrow()
  }

  override fun add_SizeChanged(handler: TypedEventHandler<Inspectable, WindowSizeChangedEventArgs>):
      EventRegistrationToken =
      EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 22,
      EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
      "Windows.Foundation.TypedEventHandler`2<Object, Microsoft.UI.Xaml.WindowSizeChangedEventArgs>",
      "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};cinterface(IInspectable);rc(Microsoft.UI.Xaml.WindowSizeChangedEventArgs;{542f6f2c-4b64-5c72-a7a5-3a7e0664b8ff}))")).getOrThrow())

  override fun remove_SizeChanged(token: EventRegistrationToken) {
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 23, token.toAbi()).getOrThrow()
  }

  override
      fun add_VisibilityChanged(handler: TypedEventHandler<Inspectable, WindowVisibilityChangedEventArgs>):
      EventRegistrationToken =
      EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 24,
      EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
      "Windows.Foundation.TypedEventHandler`2<Object, Microsoft.UI.Xaml.WindowVisibilityChangedEventArgs>",
      "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};cinterface(IInspectable);rc(Microsoft.UI.Xaml.WindowVisibilityChangedEventArgs;{7bb24a6d-070c-5cb6-8e9c-547905be8265}))")).getOrThrow())

  override fun remove_VisibilityChanged(token: EventRegistrationToken) {
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 25, token.toAbi()).getOrThrow()
  }

  override fun activate() {
    PlatformComInterop.invokeUnitMethod(pointer, 26).getOrThrow()
  }

  override fun close() {
    PlatformComInterop.invokeUnitMethod(pointer, 27).getOrThrow()
  }

  override fun setTitleBar(titleBar: UIElement) {
    PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 28, projectedObjectArgumentPointer(titleBar,
        "Microsoft.UI.Xaml.UIElement",
        "rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b})")).getOrThrow()
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
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
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
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
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
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
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
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
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
}
