package microsoft.ui.xaml

import dev.winrt.core.EventRegistrationToken
import dev.winrt.core.Float64
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
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComMethodResultKind
import dev.winrt.kom.ComPtr
import dev.winrt.kom.ComStructValue
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.requireBoolean
import java.lang.AutoCloseable
import kotlin.String
import kotlin.Unit
import kotlin.collections.MutableMap
import microsoft.ui.xaml.data.BindingBase
import microsoft.ui.xaml.data.BindingExpression
import microsoft.ui.xaml.media.Brush
import windows.foundation.EventHandler
import windows.foundation.Size
import windows.foundation.TypedEventHandler
import windows.foundation.Uri

public open class FrameworkElement(
  pointer: ComPtr,
) : UIElement(pointer),
    IFrameworkElement,
    IFrameworkElementProtected {
  private val backing_ActualHeight: RuntimeProperty<Float64> =
      RuntimeProperty<Float64>(Float64(0.0))

  override val actualHeight: Float64
    get() {
      if (pointer.isNull) {
        return backing_ActualHeight.get()
      }
      return Float64(PlatformComInterop.invokeFloat64Method(pointer, 14).getOrThrow())
    }

  private val backing_ActualTheme: RuntimeProperty<ElementTheme> =
      RuntimeProperty<ElementTheme>(ElementTheme.fromValue(0))

  override val actualTheme: ElementTheme
    get() {
      if (pointer.isNull) {
        return backing_ActualTheme.get()
      }
      return ElementTheme.fromValue(PlatformComInterop.invokeInt32Method(pointer, 60).getOrThrow())
    }

  private val backing_ActualWidth: RuntimeProperty<Float64> = RuntimeProperty<Float64>(Float64(0.0))

  override val actualWidth: Float64
    get() {
      if (pointer.isNull) {
        return backing_ActualWidth.get()
      }
      return Float64(PlatformComInterop.invokeFloat64Method(pointer, 13).getOrThrow())
    }

  private val backing_AllowFocusOnInteraction: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override var allowFocusOnInteraction: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_AllowFocusOnInteraction.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 38).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_AllowFocusOnInteraction.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 39, value.value).getOrThrow()
    }

  private val backing_AllowFocusWhenDisabled: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override var allowFocusWhenDisabled: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_AllowFocusWhenDisabled.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 50).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_AllowFocusWhenDisabled.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 51, value.value).getOrThrow()
    }

  private val backing_BaseUri: RuntimeProperty<Uri> = RuntimeProperty<Uri>(Uri(ComPtr.NULL))

  override val baseUri: Uri
    get() {
      if (pointer.isNull) {
        return backing_BaseUri.get()
      }
      return Uri(PlatformComInterop.invokeObjectMethod(pointer, 35).getOrThrow())
    }

  private val backing_DataContext: RuntimeProperty<Inspectable> =
      RuntimeProperty<Inspectable>(Inspectable(ComPtr.NULL))

  override var dataContext: Inspectable
    get() {
      if (pointer.isNull) {
        return backing_DataContext.get()
      }
      return Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 36).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_DataContext.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 37, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_FlowDirection: RuntimeProperty<FlowDirection> =
      RuntimeProperty<FlowDirection>(FlowDirection.fromValue(0))

  override var flowDirection: FlowDirection
    get() {
      if (pointer.isNull) {
        return backing_FlowDirection.get()
      }
      return FlowDirection.fromValue(PlatformComInterop.invokeInt32Method(pointer, 55).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_FlowDirection.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 56, value.value).getOrThrow()
    }

  private val backing_FocusVisualMargin: RuntimeProperty<Thickness> =
      RuntimeProperty<Thickness>(Thickness.fromAbi(ComStructValue(Thickness.ABI_LAYOUT,
      ByteArray(Thickness.ABI_LAYOUT.byteSize))))

  override var focusVisualMargin: Thickness
    get() {
      if (pointer.isNull) {
        return backing_FocusVisualMargin.get()
      }
      return Thickness.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 40,
          Thickness.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_FocusVisualMargin.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 41, value.toAbi()).getOrThrow()
    }

  private val backing_FocusVisualPrimaryBrush: RuntimeProperty<Brush> =
      RuntimeProperty<Brush>(Brush(ComPtr.NULL))

  override var focusVisualPrimaryBrush: Brush
    get() {
      if (pointer.isNull) {
        return backing_FocusVisualPrimaryBrush.get()
      }
      return Brush(PlatformComInterop.invokeObjectMethod(pointer, 48).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_FocusVisualPrimaryBrush.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 49, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_FocusVisualPrimaryThickness: RuntimeProperty<Thickness> =
      RuntimeProperty<Thickness>(Thickness.fromAbi(ComStructValue(Thickness.ABI_LAYOUT,
      ByteArray(Thickness.ABI_LAYOUT.byteSize))))

  override var focusVisualPrimaryThickness: Thickness
    get() {
      if (pointer.isNull) {
        return backing_FocusVisualPrimaryThickness.get()
      }
      return Thickness.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 44,
          Thickness.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_FocusVisualPrimaryThickness.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 45, value.toAbi()).getOrThrow()
    }

  private val backing_FocusVisualSecondaryBrush: RuntimeProperty<Brush> =
      RuntimeProperty<Brush>(Brush(ComPtr.NULL))

  override var focusVisualSecondaryBrush: Brush
    get() {
      if (pointer.isNull) {
        return backing_FocusVisualSecondaryBrush.get()
      }
      return Brush(PlatformComInterop.invokeObjectMethod(pointer, 46).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_FocusVisualSecondaryBrush.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 47, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_FocusVisualSecondaryThickness: RuntimeProperty<Thickness> =
      RuntimeProperty<Thickness>(Thickness.fromAbi(ComStructValue(Thickness.ABI_LAYOUT,
      ByteArray(Thickness.ABI_LAYOUT.byteSize))))

  override var focusVisualSecondaryThickness: Thickness
    get() {
      if (pointer.isNull) {
        return backing_FocusVisualSecondaryThickness.get()
      }
      return Thickness.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 42,
          Thickness.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_FocusVisualSecondaryThickness.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 43, value.toAbi()).getOrThrow()
    }

  private val backing_Height: RuntimeProperty<Float64> = RuntimeProperty<Float64>(Float64(0.0))

  override var height: Float64
    get() {
      if (pointer.isNull) {
        return backing_Height.get()
      }
      return Float64(PlatformComInterop.invokeFloat64Method(pointer, 17).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Height.set(value)
        return
      }
      PlatformComInterop.invokeFloat64Setter(pointer, 18, value.value).getOrThrow()
    }

  private val backing_HorizontalAlignment: RuntimeProperty<HorizontalAlignment> =
      RuntimeProperty<HorizontalAlignment>(HorizontalAlignment.fromValue(0))

  override var horizontalAlignment: HorizontalAlignment
    get() {
      if (pointer.isNull) {
        return backing_HorizontalAlignment.get()
      }
      return HorizontalAlignment.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          27).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_HorizontalAlignment.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 28, value.value).getOrThrow()
    }

  private val backing_IsLoaded: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override val isLoaded: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsLoaded.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 59).getOrThrow())
    }

  private val backing_Language: RuntimeProperty<String> = RuntimeProperty<String>("")

  override var language: String
    get() {
      if (pointer.isNull) {
        return backing_Language.get()
      }
      return run {
            val value = PlatformComInterop.invokeHStringMethod(pointer, 11).getOrThrow()
            try {
              value.toKotlinString()
            } finally {
              value.close()
            }
          }
    }
    set(value) {
      if (pointer.isNull) {
        backing_Language.set(value)
        return
      }
      PlatformComInterop.invokeStringSetter(pointer, 12, value).getOrThrow()
    }

  private val backing_Margin: RuntimeProperty<Thickness> =
      RuntimeProperty<Thickness>(Thickness.fromAbi(ComStructValue(Thickness.ABI_LAYOUT,
      ByteArray(Thickness.ABI_LAYOUT.byteSize))))

  override var margin: Thickness
    get() {
      if (pointer.isNull) {
        return backing_Margin.get()
      }
      return Thickness.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 31,
          Thickness.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Margin.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 32, value.toAbi()).getOrThrow()
    }

  private val backing_MaxHeight: RuntimeProperty<Float64> = RuntimeProperty<Float64>(Float64(0.0))

  override var maxHeight: Float64
    get() {
      if (pointer.isNull) {
        return backing_MaxHeight.get()
      }
      return Float64(PlatformComInterop.invokeFloat64Method(pointer, 25).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_MaxHeight.set(value)
        return
      }
      PlatformComInterop.invokeFloat64Setter(pointer, 26, value.value).getOrThrow()
    }

  private val backing_MaxWidth: RuntimeProperty<Float64> = RuntimeProperty<Float64>(Float64(0.0))

  override var maxWidth: Float64
    get() {
      if (pointer.isNull) {
        return backing_MaxWidth.get()
      }
      return Float64(PlatformComInterop.invokeFloat64Method(pointer, 21).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_MaxWidth.set(value)
        return
      }
      PlatformComInterop.invokeFloat64Setter(pointer, 22, value.value).getOrThrow()
    }

  private val backing_MinHeight: RuntimeProperty<Float64> = RuntimeProperty<Float64>(Float64(0.0))

  override var minHeight: Float64
    get() {
      if (pointer.isNull) {
        return backing_MinHeight.get()
      }
      return Float64(PlatformComInterop.invokeFloat64Method(pointer, 23).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_MinHeight.set(value)
        return
      }
      PlatformComInterop.invokeFloat64Setter(pointer, 24, value.value).getOrThrow()
    }

  private val backing_MinWidth: RuntimeProperty<Float64> = RuntimeProperty<Float64>(Float64(0.0))

  override var minWidth: Float64
    get() {
      if (pointer.isNull) {
        return backing_MinWidth.get()
      }
      return Float64(PlatformComInterop.invokeFloat64Method(pointer, 19).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_MinWidth.set(value)
        return
      }
      PlatformComInterop.invokeFloat64Setter(pointer, 20, value.value).getOrThrow()
    }

  private val backing_Name: RuntimeProperty<String> = RuntimeProperty<String>("")

  override var name: String
    get() {
      if (pointer.isNull) {
        return backing_Name.get()
      }
      return run {
            val value = PlatformComInterop.invokeHStringMethod(pointer, 33).getOrThrow()
            try {
              value.toKotlinString()
            } finally {
              value.close()
            }
          }
    }
    set(value) {
      if (pointer.isNull) {
        backing_Name.set(value)
        return
      }
      PlatformComInterop.invokeStringSetter(pointer, 34, value).getOrThrow()
    }

  private val backing_Parent: RuntimeProperty<DependencyObject> =
      RuntimeProperty<DependencyObject>(DependencyObject(ComPtr.NULL))

  override val parent: DependencyObject
    get() {
      if (pointer.isNull) {
        return backing_Parent.get()
      }
      return DependencyObject(PlatformComInterop.invokeObjectMethod(pointer, 54).getOrThrow())
    }

  private val backing_RequestedTheme: RuntimeProperty<ElementTheme> =
      RuntimeProperty<ElementTheme>(ElementTheme.fromValue(0))

  override var requestedTheme: ElementTheme
    get() {
      if (pointer.isNull) {
        return backing_RequestedTheme.get()
      }
      return ElementTheme.fromValue(PlatformComInterop.invokeInt32Method(pointer, 57).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_RequestedTheme.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 58, value.value).getOrThrow()
    }

  private val backing_Resources: RuntimeProperty<ResourceDictionary> =
      RuntimeProperty<ResourceDictionary>(ResourceDictionary(ComPtr.NULL))

  override var resources: ResourceDictionary
    get() {
      if (pointer.isNull) {
        return backing_Resources.get()
      }
      return ResourceDictionary(PlatformComInterop.invokeObjectMethod(pointer, 7).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Resources.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 8, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_Style: RuntimeProperty<Style> = RuntimeProperty<Style>(Style(ComPtr.NULL))

  override var style: Style
    get() {
      if (pointer.isNull) {
        return backing_Style.get()
      }
      return Style(PlatformComInterop.invokeObjectMethod(pointer, 52).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Style.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 53, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_Tag: RuntimeProperty<Inspectable> =
      RuntimeProperty<Inspectable>(Inspectable(ComPtr.NULL))

  override var tag: Inspectable
    get() {
      if (pointer.isNull) {
        return backing_Tag.get()
      }
      return Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 9).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Tag.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 10, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_Triggers: RuntimeProperty<TriggerCollection> =
      RuntimeProperty<TriggerCollection>(TriggerCollection(ComPtr.NULL))

  override val triggers: TriggerCollection
    get() {
      if (pointer.isNull) {
        return backing_Triggers.get()
      }
      return TriggerCollection(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    }

  private val backing_VerticalAlignment: RuntimeProperty<VerticalAlignment> =
      RuntimeProperty<VerticalAlignment>(VerticalAlignment.fromValue(0))

  override var verticalAlignment: VerticalAlignment
    get() {
      if (pointer.isNull) {
        return backing_VerticalAlignment.get()
      }
      return VerticalAlignment.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          29).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_VerticalAlignment.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 30, value.value).getOrThrow()
    }

  private val backing_Width: RuntimeProperty<Float64> = RuntimeProperty<Float64>(Float64(0.0))

  override var width: Float64
    get() {
      if (pointer.isNull) {
        return backing_Width.get()
      }
      return Float64(PlatformComInterop.invokeFloat64Method(pointer, 15).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Width.set(value)
        return
      }
      PlatformComInterop.invokeFloat64Setter(pointer, 16, value.value).getOrThrow()
    }

  private val dataContextChangedEventSlot: DataContextChangedEvent = DataContextChangedEvent()

  public val dataContextChangedEvent: DataContextChangedEvent
    get() = dataContextChangedEventSlot

  private val layoutUpdatedEventSlot: LayoutUpdatedEvent = LayoutUpdatedEvent()

  public val layoutUpdatedEvent: LayoutUpdatedEvent
    get() = layoutUpdatedEventSlot

  private val loadingEventSlot: LoadingEvent = LoadingEvent()

  public val loadingEvent: LoadingEvent
    get() = loadingEventSlot

  private val actualThemeChangedEventSlot: ActualThemeChangedEvent = ActualThemeChangedEvent()

  public val actualThemeChangedEvent: ActualThemeChangedEvent
    get() = actualThemeChangedEventSlot

  private val effectiveViewportChangedEventSlot: EffectiveViewportChangedEvent =
      EffectiveViewportChangedEvent()

  public val effectiveViewportChangedEvent: EffectiveViewportChangedEvent
    get() = effectiveViewportChangedEventSlot

  public constructor() : this(Companion.factoryCreateInstance().pointer)

  override fun invalidateViewport() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 6).getOrThrow()
  }

  public fun measureOverride(availableSize: Size): Size {
    if (pointer.isNull) {
      return Size.fromAbi(ComStructValue(Size.ABI_LAYOUT, ByteArray(Size.ABI_LAYOUT.byteSize)))
    }
    return Size.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 6, Size.ABI_LAYOUT,
        availableSize.toAbi()).getOrThrow())
  }

  public fun arrangeOverride(finalSize: Size): Size {
    if (pointer.isNull) {
      return Size.fromAbi(ComStructValue(Size.ABI_LAYOUT, ByteArray(Size.ABI_LAYOUT.byteSize)))
    }
    return Size.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 7, Size.ABI_LAYOUT,
        finalSize.toAbi()).getOrThrow())
  }

  public fun onApplyTemplate() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 8).getOrThrow()
  }

  public fun goToElementStateCore(stateName: String, useTransitions: WinRtBoolean): WinRtBoolean {
    if (pointer.isNull) {
      return WinRtBoolean.FALSE
    }
    return WinRtBoolean(PlatformComInterop.invokeMethodWithStringAndBooleanArgs(pointer, 9,
        ComMethodResultKind.BOOLEAN, stateName, useTransitions.value).getOrThrow().requireBoolean())
  }

  override fun add_Loaded(handler: RoutedEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 61,
        EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.RoutedEventHandler",
        "delegate({dae23d85-69ca-5bdf-805b-6161a3a215cc})")).getOrThrow())
  }

  override fun remove_Loaded(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 62, token.toAbi()).getOrThrow()
  }

  override fun add_Unloaded(handler: RoutedEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 63,
        EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.RoutedEventHandler",
        "delegate({dae23d85-69ca-5bdf-805b-6161a3a215cc})")).getOrThrow())
  }

  override fun remove_Unloaded(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 64, token.toAbi()).getOrThrow()
  }

  override
      fun add_DataContextChanged(handler: TypedEventHandler<FrameworkElement, DataContextChangedEventArgs>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 65,
        EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.FrameworkElement, Microsoft.UI.Xaml.DataContextChangedEventArgs>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.FrameworkElement;{fe08f13d-dc6a-5495-ad44-c2d8d21863b0});rc(Microsoft.UI.Xaml.DataContextChangedEventArgs;{a1be80f4-cf83-5022-b113-9233f1d4fafa}))")).getOrThrow())
  }

  override fun remove_DataContextChanged(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 66, token.toAbi()).getOrThrow()
  }

  override fun add_SizeChanged(handler: SizeChangedEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 67,
        EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.SizeChangedEventHandler",
        "delegate({8d7b1a58-14c6-51c9-892c-9fcce368e77d})")).getOrThrow())
  }

  override fun remove_SizeChanged(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 68, token.toAbi()).getOrThrow()
  }

  override fun add_LayoutUpdated(handler: EventHandler<Inspectable>): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 69,
        EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.EventHandler`1<Object>",
        "pinterface({9de1c535-6ae1-11e0-84e1-18a905bcc53f};cinterface(IInspectable))")).getOrThrow())
  }

  override fun remove_LayoutUpdated(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 70, token.toAbi()).getOrThrow()
  }

  override fun add_Loading(handler: TypedEventHandler<FrameworkElement, Inspectable>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 71,
        EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.FrameworkElement, Object>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.FrameworkElement;{fe08f13d-dc6a-5495-ad44-c2d8d21863b0});cinterface(IInspectable))")).getOrThrow())
  }

  override fun remove_Loading(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 72, token.toAbi()).getOrThrow()
  }

  override fun add_ActualThemeChanged(handler: TypedEventHandler<FrameworkElement, Inspectable>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 73,
        EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.FrameworkElement, Object>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.FrameworkElement;{fe08f13d-dc6a-5495-ad44-c2d8d21863b0});cinterface(IInspectable))")).getOrThrow())
  }

  override fun remove_ActualThemeChanged(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 74, token.toAbi()).getOrThrow()
  }

  override
      fun add_EffectiveViewportChanged(handler: TypedEventHandler<FrameworkElement, EffectiveViewportChangedEventArgs>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 75,
        EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.FrameworkElement, Microsoft.UI.Xaml.EffectiveViewportChangedEventArgs>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.FrameworkElement;{fe08f13d-dc6a-5495-ad44-c2d8d21863b0});rc(Microsoft.UI.Xaml.EffectiveViewportChangedEventArgs;{636e8159-2d82-538a-8483-cd576e41d0df}))")).getOrThrow())
  }

  override fun remove_EffectiveViewportChanged(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 76, token.toAbi()).getOrThrow()
  }

  override fun findName(name: String): Inspectable {
    if (pointer.isNull) {
      error("Null runtime object pointer: FindName")
    }
    return Inspectable(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 77,
        name).getOrThrow())
  }

  override fun setBinding(dp: DependencyProperty, binding: BindingBase) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithTwoObjectArgs(pointer, 78,
        projectedObjectArgumentPointer(dp, "Microsoft.UI.Xaml.DependencyProperty",
        "rc(Microsoft.UI.Xaml.DependencyProperty;{960eab49-9672-58a0-995b-3a42e5ea6278})"),
        projectedObjectArgumentPointer(binding, "Microsoft.UI.Xaml.Data.BindingBase",
        "rc(Microsoft.UI.Xaml.Data.BindingBase;{91ddd141-5944-50ef-b85e-218e463f7a73})")).getOrThrow()
  }

  override fun getBindingExpression(dp: DependencyProperty): BindingExpression {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetBindingExpression")
    }
    return BindingExpression(PlatformComInterop.invokeObjectMethodWithObjectArg(pointer, 79,
        projectedObjectArgumentPointer(dp, "Microsoft.UI.Xaml.DependencyProperty",
        "rc(Microsoft.UI.Xaml.DependencyProperty;{960eab49-9672-58a0-995b-3a42e5ea6278})")).getOrThrow())
  }

  public inner class DataContextChangedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<FrameworkElement, DataContextChangedEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 65,
        handler.pointer).getOrThrow())

    public
        fun subscribeScoped(handler: TypedEventHandler<FrameworkElement, DataContextChangedEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (FrameworkElement, DataContextChangedEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(FrameworkElement(args[0]
          as ComPtr), DataContextChangedEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<FrameworkElement, DataContextChangedEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (FrameworkElement, DataContextChangedEventArgs) -> Unit):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator
        fun plusAssign(handler: TypedEventHandler<FrameworkElement, DataContextChangedEventArgs>) {
      subscribe(handler)
    }

    public operator
        fun invoke(handler: TypedEventHandler<FrameworkElement, DataContextChangedEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (FrameworkElement, DataContextChangedEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 66, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class LayoutUpdatedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: EventHandler<Inspectable>): EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 69,
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
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 70, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class LoadingEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<FrameworkElement, Inspectable>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 71,
        handler.pointer).getOrThrow())

    public fun subscribeScoped(handler: TypedEventHandler<FrameworkElement, Inspectable>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (FrameworkElement, ComPtr) -> Unit): EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(FrameworkElement(args[0]
          as ComPtr), args[1] as ComPtr) }
      try {
        val token =
            subscribe(TypedEventHandler<FrameworkElement, Inspectable>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (FrameworkElement, ComPtr) -> Unit): AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator fun plusAssign(handler: TypedEventHandler<FrameworkElement, Inspectable>) {
      subscribe(handler)
    }

    public operator fun invoke(handler: TypedEventHandler<FrameworkElement, Inspectable>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (FrameworkElement, ComPtr) -> Unit): EventRegistrationToken
        = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 72, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class ActualThemeChangedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<FrameworkElement, Inspectable>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 73,
        handler.pointer).getOrThrow())

    public fun subscribeScoped(handler: TypedEventHandler<FrameworkElement, Inspectable>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (FrameworkElement, ComPtr) -> Unit): EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(FrameworkElement(args[0]
          as ComPtr), args[1] as ComPtr) }
      try {
        val token =
            subscribe(TypedEventHandler<FrameworkElement, Inspectable>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (FrameworkElement, ComPtr) -> Unit): AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator fun plusAssign(handler: TypedEventHandler<FrameworkElement, Inspectable>) {
      subscribe(handler)
    }

    public operator fun invoke(handler: TypedEventHandler<FrameworkElement, Inspectable>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (FrameworkElement, ComPtr) -> Unit): EventRegistrationToken
        = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 74, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class EffectiveViewportChangedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public
        fun subscribe(handler: TypedEventHandler<FrameworkElement, EffectiveViewportChangedEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 75,
        handler.pointer).getOrThrow())

    public
        fun subscribeScoped(handler: TypedEventHandler<FrameworkElement, EffectiveViewportChangedEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (FrameworkElement, EffectiveViewportChangedEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(FrameworkElement(args[0]
          as ComPtr), EffectiveViewportChangedEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<FrameworkElement, EffectiveViewportChangedEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (FrameworkElement,
        EffectiveViewportChangedEventArgs) -> Unit): AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator
        fun plusAssign(handler: TypedEventHandler<FrameworkElement, EffectiveViewportChangedEventArgs>) {
      subscribe(handler)
    }

    public operator
        fun invoke(handler: TypedEventHandler<FrameworkElement, EffectiveViewportChangedEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (FrameworkElement,
        EffectiveViewportChangedEventArgs) -> Unit): EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 76, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.FrameworkElement"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml", "FrameworkElement")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.IFrameworkElement"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

    private val statics: IFrameworkElementStatics by lazy {
        WinRtRuntime.projectActivationFactory(this, IFrameworkElementStatics,
        ::IFrameworkElementStatics) }

    public val actualHeightProperty: DependencyProperty
      get() = statics.actualHeightProperty

    public val actualThemeProperty: DependencyProperty
      get() = statics.actualThemeProperty

    public val actualWidthProperty: DependencyProperty
      get() = statics.actualWidthProperty

    public val allowFocusOnInteractionProperty: DependencyProperty
      get() = statics.allowFocusOnInteractionProperty

    public val allowFocusWhenDisabledProperty: DependencyProperty
      get() = statics.allowFocusWhenDisabledProperty

    public val dataContextProperty: DependencyProperty
      get() = statics.dataContextProperty

    public val flowDirectionProperty: DependencyProperty
      get() = statics.flowDirectionProperty

    public val focusVisualMarginProperty: DependencyProperty
      get() = statics.focusVisualMarginProperty

    public val focusVisualPrimaryBrushProperty: DependencyProperty
      get() = statics.focusVisualPrimaryBrushProperty

    public val focusVisualPrimaryThicknessProperty: DependencyProperty
      get() = statics.focusVisualPrimaryThicknessProperty

    public val focusVisualSecondaryBrushProperty: DependencyProperty
      get() = statics.focusVisualSecondaryBrushProperty

    public val focusVisualSecondaryThicknessProperty: DependencyProperty
      get() = statics.focusVisualSecondaryThicknessProperty

    public val heightProperty: DependencyProperty
      get() = statics.heightProperty

    public val horizontalAlignmentProperty: DependencyProperty
      get() = statics.horizontalAlignmentProperty

    public val languageProperty: DependencyProperty
      get() = statics.languageProperty

    public val marginProperty: DependencyProperty
      get() = statics.marginProperty

    public val maxHeightProperty: DependencyProperty
      get() = statics.maxHeightProperty

    public val maxWidthProperty: DependencyProperty
      get() = statics.maxWidthProperty

    public val minHeightProperty: DependencyProperty
      get() = statics.minHeightProperty

    public val minWidthProperty: DependencyProperty
      get() = statics.minWidthProperty

    public val nameProperty: DependencyProperty
      get() = statics.nameProperty

    public val requestedThemeProperty: DependencyProperty
      get() = statics.requestedThemeProperty

    public val styleProperty: DependencyProperty
      get() = statics.styleProperty

    public val tagProperty: DependencyProperty
      get() = statics.tagProperty

    public val verticalAlignmentProperty: DependencyProperty
      get() = statics.verticalAlignmentProperty

    public val widthProperty: DependencyProperty
      get() = statics.widthProperty

    private fun factoryCreateInstance(): FrameworkElement {
      return WinRtRuntime.compose(this, guidOf("bd3f2272-3efa-5f92-b759-90b1cc3e784c"),
          guidOf("fe08f13d-dc6a-5495-ad44-c2d8d21863b0"), ::FrameworkElement, 6, ComPtr.NULL)
    }

    public fun deferTree(element: DependencyObject) {
      statics.deferTree(element)
    }
  }
}
