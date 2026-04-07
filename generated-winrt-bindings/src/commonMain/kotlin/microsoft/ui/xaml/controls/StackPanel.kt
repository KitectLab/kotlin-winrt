package microsoft.ui.xaml.controls

import dev.winrt.core.EventRegistrationToken
import dev.winrt.core.Float32
import dev.winrt.core.Float64
import dev.winrt.core.Inspectable
import dev.winrt.core.Int32
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtDelegateBridge
import dev.winrt.core.WinRtDelegateHandle
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.guidOf
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComMethodResultKind
import dev.winrt.kom.ComPtr
import dev.winrt.kom.ComStructValue
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.requireObject
import java.lang.AutoCloseable
import kotlin.String
import kotlin.Unit
import kotlin.collections.MutableMap
import microsoft.ui.xaml.CornerRadius
import microsoft.ui.xaml.DependencyProperty
import microsoft.ui.xaml.Thickness
import microsoft.ui.xaml.controls.primitives.IScrollSnapPointsInfo
import microsoft.ui.xaml.controls.primitives.SnapPointsAlignment
import microsoft.ui.xaml.media.Brush
import windows.foundation.EventHandler
import windows.foundation.Point
import windows.foundation.collections.IVectorView

public open class StackPanel(
  pointer: ComPtr,
) : Panel(pointer),
    IStackPanel,
    IScrollSnapPointsInfo,
    IInsertionPanel {
  private val backing_AreHorizontalSnapPointsRegular: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override val areHorizontalSnapPointsRegular: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_AreHorizontalSnapPointsRegular.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 6).getOrThrow())
    }

  private val backing_AreVerticalSnapPointsRegular: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override val areVerticalSnapPointsRegular: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_AreVerticalSnapPointsRegular.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 7).getOrThrow())
    }

  private val backing_AreScrollSnapPointsRegular: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override var areScrollSnapPointsRegular: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_AreScrollSnapPointsRegular.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 6).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_AreScrollSnapPointsRegular.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithUInt32Arg(pointer, 7, if (value.value) 1u else 0u).getOrThrow()
    }

  private val backing_BackgroundSizing: RuntimeProperty<BackgroundSizing> =
      RuntimeProperty<BackgroundSizing>(BackgroundSizing.fromValue(0))

  override var backgroundSizing: BackgroundSizing
    get() {
      if (pointer.isNull) {
        return backing_BackgroundSizing.get()
      }
      return BackgroundSizing.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          10).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_BackgroundSizing.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 11, value.value).getOrThrow()
    }

  private val backing_BorderBrush: RuntimeProperty<Brush> =
      RuntimeProperty<Brush>(Brush(ComPtr.NULL))

  override var borderBrush: Brush
    get() {
      if (pointer.isNull) {
        return backing_BorderBrush.get()
      }
      return Brush(PlatformComInterop.invokeObjectMethod(pointer, 12).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_BorderBrush.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 13, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_BorderThickness: RuntimeProperty<Thickness> =
      RuntimeProperty<Thickness>(Thickness.fromAbi(ComStructValue(Thickness.ABI_LAYOUT,
      ByteArray(Thickness.ABI_LAYOUT.byteSize))))

  override var borderThickness: Thickness
    get() {
      if (pointer.isNull) {
        return backing_BorderThickness.get()
      }
      return Thickness.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 14,
          Thickness.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_BorderThickness.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 15, value.toAbi()).getOrThrow()
    }

  private val backing_CornerRadius: RuntimeProperty<CornerRadius> =
      RuntimeProperty<CornerRadius>(CornerRadius.fromAbi(ComStructValue(CornerRadius.ABI_LAYOUT,
      ByteArray(CornerRadius.ABI_LAYOUT.byteSize))))

  override var cornerRadius: CornerRadius
    get() {
      if (pointer.isNull) {
        return backing_CornerRadius.get()
      }
      return CornerRadius.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 16,
          CornerRadius.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_CornerRadius.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 17, value.toAbi()).getOrThrow()
    }

  private val backing_Orientation: RuntimeProperty<Orientation> =
      RuntimeProperty<Orientation>(Orientation.fromValue(0))

  override var orientation: Orientation
    get() {
      if (pointer.isNull) {
        return backing_Orientation.get()
      }
      return Orientation.fromValue(PlatformComInterop.invokeInt32Method(pointer, 8).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Orientation.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 9, value.value).getOrThrow()
    }

  private val backing_Padding: RuntimeProperty<Thickness> =
      RuntimeProperty<Thickness>(Thickness.fromAbi(ComStructValue(Thickness.ABI_LAYOUT,
      ByteArray(Thickness.ABI_LAYOUT.byteSize))))

  override var padding: Thickness
    get() {
      if (pointer.isNull) {
        return backing_Padding.get()
      }
      return Thickness.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 18,
          Thickness.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Padding.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 19, value.toAbi()).getOrThrow()
    }

  private val backing_Spacing: RuntimeProperty<Float64> = RuntimeProperty<Float64>(Float64(0.0))

  override var spacing: Float64
    get() {
      if (pointer.isNull) {
        return backing_Spacing.get()
      }
      return Float64(PlatformComInterop.invokeFloat64Method(pointer, 20).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Spacing.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithFloat64Arg(pointer, 21, value.value).getOrThrow()
    }

  private val backing_AreScrollSnapPointsRegularProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val areScrollSnapPointsRegularProperty: DependencyProperty
    get() = backing_AreScrollSnapPointsRegularProperty.get()

  private val backing_BackgroundSizingProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val backgroundSizingProperty: DependencyProperty
    get() = backing_BackgroundSizingProperty.get()

  private val backing_BorderBrushProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val borderBrushProperty: DependencyProperty
    get() = backing_BorderBrushProperty.get()

  private val backing_BorderThicknessProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val borderThicknessProperty: DependencyProperty
    get() = backing_BorderThicknessProperty.get()

  private val backing_CornerRadiusProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val cornerRadiusProperty: DependencyProperty
    get() = backing_CornerRadiusProperty.get()

  private val backing_OrientationProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val orientationProperty: DependencyProperty
    get() = backing_OrientationProperty.get()

  private val backing_PaddingProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val paddingProperty: DependencyProperty
    get() = backing_PaddingProperty.get()

  private val backing_SpacingProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val spacingProperty: DependencyProperty
    get() = backing_SpacingProperty.get()

  private val horizontalSnapPointsChangedEventSlot: HorizontalSnapPointsChangedEvent =
      HorizontalSnapPointsChangedEvent()

  public val horizontalSnapPointsChangedEvent: HorizontalSnapPointsChangedEvent
    get() = horizontalSnapPointsChangedEventSlot

  private val verticalSnapPointsChangedEventSlot: VerticalSnapPointsChangedEvent =
      VerticalSnapPointsChangedEvent()

  public val verticalSnapPointsChangedEvent: VerticalSnapPointsChangedEvent
    get() = verticalSnapPointsChangedEventSlot

  public constructor() : this(Companion.factoryCreateInstance().pointer)

  override fun add_HorizontalSnapPointsChanged(handler: EventHandler<Inspectable>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 8,
        EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.EventHandler`1<Object>",
        "pinterface({9de1c535-6ae1-11e0-84e1-18a905bcc53f};cinterface(IInspectable))")).getOrThrow())
  }

  override fun remove_HorizontalSnapPointsChanged(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 9, token.toAbi()).getOrThrow()
  }

  override fun add_VerticalSnapPointsChanged(handler: EventHandler<Inspectable>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 10,
        EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.EventHandler`1<Object>",
        "pinterface({9de1c535-6ae1-11e0-84e1-18a905bcc53f};cinterface(IInspectable))")).getOrThrow())
  }

  override fun remove_VerticalSnapPointsChanged(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 11, token.toAbi()).getOrThrow()
  }

  override fun getIrregularSnapPoints(orientation: Orientation, alignment: SnapPointsAlignment):
      IVectorView<Float32> {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetIrregularSnapPoints")
    }
    return IVectorView<Float32>.from(Inspectable(PlatformComInterop.invokeMethodWithTwoInt32Args(pointer,
        12, ComMethodResultKind.OBJECT, orientation.value,
        alignment.value).getOrThrow().requireObject()))
  }

  override fun getInsertionIndexes(
    position: Point,
    first: Int32,
    second: Int32,
  ) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 6, position.toAbi(), first.value,
        second.value).getOrThrow()
  }

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

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.StackPanel"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls",
        "StackPanel")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IStackPanel"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

    private val statics: IStackPanelStatics by lazy { WinRtRuntime.projectActivationFactory(this,
        IStackPanelStatics, ::IStackPanelStatics) }

    public val areScrollSnapPointsRegularProperty: DependencyProperty
      get() = statics.areScrollSnapPointsRegularProperty

    public val backgroundSizingProperty: DependencyProperty
      get() = statics.backgroundSizingProperty

    public val borderBrushProperty: DependencyProperty
      get() = statics.borderBrushProperty

    public val borderThicknessProperty: DependencyProperty
      get() = statics.borderThicknessProperty

    public val cornerRadiusProperty: DependencyProperty
      get() = statics.cornerRadiusProperty

    public val orientationProperty: DependencyProperty
      get() = statics.orientationProperty

    public val paddingProperty: DependencyProperty
      get() = statics.paddingProperty

    public val spacingProperty: DependencyProperty
      get() = statics.spacingProperty

    private fun factoryCreateInstance(): StackPanel {
      return WinRtRuntime.compose(this, guidOf("64c1d388-47a2-5a74-a75b-559d151ee5ac"),
          guidOf("493ab00b-3a6a-5e4a-9452-407cd5197406"), ::StackPanel, 6, ComPtr.NULL)
    }
  }
}
