package microsoft.ui.xaml.controls.primitives

import dev.winrt.core.EventRegistrationToken
import dev.winrt.core.Float32
import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtDelegateBridge
import dev.winrt.core.WinRtDelegateHandle
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComMethodResultKind
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.requireObject
import java.lang.AutoCloseable
import kotlin.String
import kotlin.Unit
import kotlin.collections.MutableMap
import microsoft.ui.xaml.controls.Orientation
import windows.foundation.EventHandler
import windows.foundation.collections.IVectorView

public interface IScrollSnapPointsInfo {
  public val areHorizontalSnapPointsRegular: WinRtBoolean

  public val areVerticalSnapPointsRegular: WinRtBoolean

  public fun add_HorizontalSnapPointsChanged(handler: EventHandler<Inspectable>):
      EventRegistrationToken

  public fun remove_HorizontalSnapPointsChanged(token: EventRegistrationToken)

  public fun add_VerticalSnapPointsChanged(handler: EventHandler<Inspectable>):
      EventRegistrationToken

  public fun remove_VerticalSnapPointsChanged(token: EventRegistrationToken)

  public fun getIrregularSnapPoints(orientation: Orientation, alignment: SnapPointsAlignment):
      IVectorView<Float32>

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String =
        "Microsoft.UI.Xaml.Controls.Primitives.IScrollSnapPointsInfo"

    override val projectionTypeKey: String =
        "Microsoft.UI.Xaml.Controls.Primitives.IScrollSnapPointsInfo"

    override val iid: Guid = guidOf("d3ea6e09-ecf7-51a8-bd54-fc84b9653766")

    public fun from(inspectable: Inspectable): IScrollSnapPointsInfo =
        inspectable.projectInterface(this, ::IScrollSnapPointsInfoProjection)

    public operator fun invoke(inspectable: Inspectable): IScrollSnapPointsInfo = from(inspectable)
  }
}

private class IScrollSnapPointsInfoProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IScrollSnapPointsInfo {
  override val areHorizontalSnapPointsRegular: WinRtBoolean
    get() = WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 6).getOrThrow())

  override val areVerticalSnapPointsRegular: WinRtBoolean
    get() = WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 7).getOrThrow())

  private val horizontalSnapPointsChangedEventSlot: HorizontalSnapPointsChangedEvent =
      HorizontalSnapPointsChangedEvent()

  public val horizontalSnapPointsChangedEvent: HorizontalSnapPointsChangedEvent
    get() = horizontalSnapPointsChangedEventSlot

  private val verticalSnapPointsChangedEventSlot: VerticalSnapPointsChangedEvent =
      VerticalSnapPointsChangedEvent()

  public val verticalSnapPointsChangedEvent: VerticalSnapPointsChangedEvent
    get() = verticalSnapPointsChangedEventSlot

  override fun add_HorizontalSnapPointsChanged(handler: EventHandler<Inspectable>):
      EventRegistrationToken =
      EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 8,
      EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
      "Windows.Foundation.EventHandler`1<Object>",
      "pinterface({9de1c535-6ae1-11e0-84e1-18a905bcc53f};cinterface(IInspectable))")).getOrThrow())

  override fun remove_HorizontalSnapPointsChanged(token: EventRegistrationToken) {
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 9, token.toAbi()).getOrThrow()
  }

  override fun add_VerticalSnapPointsChanged(handler: EventHandler<Inspectable>):
      EventRegistrationToken =
      EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 10,
      EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
      "Windows.Foundation.EventHandler`1<Object>",
      "pinterface({9de1c535-6ae1-11e0-84e1-18a905bcc53f};cinterface(IInspectable))")).getOrThrow())

  override fun remove_VerticalSnapPointsChanged(token: EventRegistrationToken) {
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 11, token.toAbi()).getOrThrow()
  }

  override fun getIrregularSnapPoints(orientation: Orientation, alignment: SnapPointsAlignment):
      IVectorView<Float32> =
      IVectorView.from(Inspectable(PlatformComInterop.invokeMethodWithTwoInt32Args(pointer, 12,
      ComMethodResultKind.OBJECT, orientation.value, alignment.value).getOrThrow().requireObject()),
      "f4", "Float32")

  public inner class HorizontalSnapPointsChangedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: EventHandler<Inspectable>): EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 8,
        handler.pointer).getOrThrow())

    public fun subscribeScoped(handler: EventHandler<Inspectable>): AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (ComPtr, ComPtr) -> Unit): EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c535-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(args[0] as ComPtr,
          args[1] as ComPtr) }
      try {
        val token = subscribe(EventHandler<Inspectable>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (ComPtr, ComPtr) -> Unit): AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator fun plusAssign(handler: EventHandler<Inspectable>) {
      subscribe(handler)
    }

    public operator fun invoke(handler: EventHandler<Inspectable>): EventRegistrationToken =
        subscribe(handler)

    public operator fun invoke(handler: (ComPtr, ComPtr) -> Unit): EventRegistrationToken =
        subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 9, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class VerticalSnapPointsChangedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: EventHandler<Inspectable>): EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 10,
        handler.pointer).getOrThrow())

    public fun subscribeScoped(handler: EventHandler<Inspectable>): AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (ComPtr, ComPtr) -> Unit): EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c535-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(args[0] as ComPtr,
          args[1] as ComPtr) }
      try {
        val token = subscribe(EventHandler<Inspectable>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (ComPtr, ComPtr) -> Unit): AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator fun plusAssign(handler: EventHandler<Inspectable>) {
      subscribe(handler)
    }

    public operator fun invoke(handler: EventHandler<Inspectable>): EventRegistrationToken =
        subscribe(handler)

    public operator fun invoke(handler: (ComPtr, ComPtr) -> Unit): EventRegistrationToken =
        subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 11, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }
}
