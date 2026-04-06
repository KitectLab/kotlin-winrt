package microsoft.ui.xaml.controls

import dev.winrt.core.EventRegistrationToken
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
import dev.winrt.kom.ComPtr
import dev.winrt.kom.ComStructValue
import dev.winrt.kom.PlatformComInterop
import java.lang.AutoCloseable
import kotlin.String
import kotlin.Unit
import kotlin.collections.MutableMap
import microsoft.ui.xaml.CornerRadius
import microsoft.ui.xaml.DependencyObject
import microsoft.ui.xaml.DependencyProperty
import microsoft.ui.xaml.DependencyPropertyChangedEventHandler
import microsoft.ui.xaml.DragEventArgs
import microsoft.ui.xaml.ElementSoundMode
import microsoft.ui.xaml.FrameworkElement
import microsoft.ui.xaml.HorizontalAlignment
import microsoft.ui.xaml.RoutedEventArgs
import microsoft.ui.xaml.Thickness
import microsoft.ui.xaml.VerticalAlignment
import microsoft.ui.xaml.input.CharacterReceivedRoutedEventArgs
import microsoft.ui.xaml.input.DoubleTappedRoutedEventArgs
import microsoft.ui.xaml.input.HoldingRoutedEventArgs
import microsoft.ui.xaml.input.KeyRoutedEventArgs
import microsoft.ui.xaml.input.KeyboardNavigationMode
import microsoft.ui.xaml.input.ManipulationCompletedRoutedEventArgs
import microsoft.ui.xaml.input.ManipulationDeltaRoutedEventArgs
import microsoft.ui.xaml.input.ManipulationInertiaStartingRoutedEventArgs
import microsoft.ui.xaml.input.ManipulationStartedRoutedEventArgs
import microsoft.ui.xaml.input.ManipulationStartingRoutedEventArgs
import microsoft.ui.xaml.input.PointerRoutedEventArgs
import microsoft.ui.xaml.input.RightTappedRoutedEventArgs
import microsoft.ui.xaml.input.TappedRoutedEventArgs
import microsoft.ui.xaml.media.Brush
import microsoft.ui.xaml.media.FontFamily
import windows.foundation.TypedEventHandler
import windows.foundation.Uri
import windows.ui.text.FontStretch
import windows.ui.text.FontStyle
import windows.ui.text.FontWeight

public open class Control(
  pointer: ComPtr,
) : FrameworkElement(pointer),
    IControlProtected {
  private val backing_DefaultStyleKey: RuntimeProperty<Inspectable> =
      RuntimeProperty<Inspectable>(Inspectable(ComPtr.NULL))

  override var defaultStyleKey: Inspectable
    get() {
      if (pointer.isNull) {
        return backing_DefaultStyleKey.get()
      }
      return Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_DefaultStyleKey.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 7, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_Background: RuntimeProperty<Brush> =
      RuntimeProperty<Brush>(Brush(ComPtr.NULL))

  public var background: Brush
    get() {
      if (pointer.isNull) {
        return backing_Background.get()
      }
      return Brush(PlatformComInterop.invokeObjectMethod(pointer, 40).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Background.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 41, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_BackgroundSizing: RuntimeProperty<BackgroundSizing> =
      RuntimeProperty<BackgroundSizing>(BackgroundSizing.fromValue(0))

  public var backgroundSizing: BackgroundSizing
    get() {
      if (pointer.isNull) {
        return backing_BackgroundSizing.get()
      }
      return BackgroundSizing.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          42).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_BackgroundSizing.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 43, value.value).getOrThrow()
    }

  private val backing_BorderBrush: RuntimeProperty<Brush> =
      RuntimeProperty<Brush>(Brush(ComPtr.NULL))

  public var borderBrush: Brush
    get() {
      if (pointer.isNull) {
        return backing_BorderBrush.get()
      }
      return Brush(PlatformComInterop.invokeObjectMethod(pointer, 46).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_BorderBrush.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 47, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_BorderThickness: RuntimeProperty<Thickness> =
      RuntimeProperty<Thickness>(Thickness.fromAbi(ComStructValue(Thickness.ABI_LAYOUT,
      ByteArray(Thickness.ABI_LAYOUT.byteSize))))

  public var borderThickness: Thickness
    get() {
      if (pointer.isNull) {
        return backing_BorderThickness.get()
      }
      return Thickness.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 44,
          Thickness.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_BorderThickness.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 45, value.toAbi()).getOrThrow()
    }

  private val backing_CharacterSpacing: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public var characterSpacing: Int32
    get() {
      if (pointer.isNull) {
        return backing_CharacterSpacing.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 22).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_CharacterSpacing.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 23, value.value).getOrThrow()
    }

  private val backing_CornerRadius: RuntimeProperty<CornerRadius> =
      RuntimeProperty<CornerRadius>(CornerRadius.fromAbi(ComStructValue(CornerRadius.ABI_LAYOUT,
      ByteArray(CornerRadius.ABI_LAYOUT.byteSize))))

  public var cornerRadius: CornerRadius
    get() {
      if (pointer.isNull) {
        return backing_CornerRadius.get()
      }
      return CornerRadius.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 52,
          CornerRadius.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_CornerRadius.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 53, value.toAbi()).getOrThrow()
    }

  private val backing_DefaultStyleResourceUri: RuntimeProperty<Uri> =
      RuntimeProperty<Uri>(Uri(ComPtr.NULL))

  public var defaultStyleResourceUri: Uri
    get() {
      if (pointer.isNull) {
        return backing_DefaultStyleResourceUri.get()
      }
      return Uri(PlatformComInterop.invokeObjectMethod(pointer, 48).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_DefaultStyleResourceUri.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 49, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_ElementSoundMode: RuntimeProperty<ElementSoundMode> =
      RuntimeProperty<ElementSoundMode>(ElementSoundMode.fromValue(0))

  public var elementSoundMode: ElementSoundMode
    get() {
      if (pointer.isNull) {
        return backing_ElementSoundMode.get()
      }
      return ElementSoundMode.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          50).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_ElementSoundMode.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 51, value.value).getOrThrow()
    }

  private val backing_FontFamily: RuntimeProperty<FontFamily> =
      RuntimeProperty<FontFamily>(FontFamily(ComPtr.NULL))

  public var fontFamily: FontFamily
    get() {
      if (pointer.isNull) {
        return backing_FontFamily.get()
      }
      return FontFamily(PlatformComInterop.invokeObjectMethod(pointer, 14).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_FontFamily.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 15, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_FontSize: RuntimeProperty<Float64> = RuntimeProperty<Float64>(Float64(0.0))

  public var fontSize: Float64
    get() {
      if (pointer.isNull) {
        return backing_FontSize.get()
      }
      return Float64(PlatformComInterop.invokeFloat64Method(pointer, 12).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_FontSize.set(value)
        return
      }
      PlatformComInterop.invokeFloat64Setter(pointer, 13, value.value).getOrThrow()
    }

  private val backing_FontStretch: RuntimeProperty<FontStretch> =
      RuntimeProperty<FontStretch>(FontStretch.fromValue(0))

  public var fontStretch: FontStretch
    get() {
      if (pointer.isNull) {
        return backing_FontStretch.get()
      }
      return FontStretch.fromValue(PlatformComInterop.invokeInt32Method(pointer, 20).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_FontStretch.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 21, value.value).getOrThrow()
    }

  private val backing_FontStyle: RuntimeProperty<FontStyle> =
      RuntimeProperty<FontStyle>(FontStyle.fromValue(0))

  public var fontStyle: FontStyle
    get() {
      if (pointer.isNull) {
        return backing_FontStyle.get()
      }
      return FontStyle.fromValue(PlatformComInterop.invokeInt32Method(pointer, 18).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_FontStyle.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 19, value.value).getOrThrow()
    }

  private val backing_FontWeight: RuntimeProperty<FontWeight> =
      RuntimeProperty<FontWeight>(FontWeight.fromAbi(ComStructValue(FontWeight.ABI_LAYOUT,
      ByteArray(FontWeight.ABI_LAYOUT.byteSize))))

  public var fontWeight: FontWeight
    get() {
      if (pointer.isNull) {
        return backing_FontWeight.get()
      }
      return FontWeight.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 16,
          FontWeight.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_FontWeight.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 17, value.toAbi()).getOrThrow()
    }

  private val backing_Foreground: RuntimeProperty<Brush> =
      RuntimeProperty<Brush>(Brush(ComPtr.NULL))

  public var foreground: Brush
    get() {
      if (pointer.isNull) {
        return backing_Foreground.get()
      }
      return Brush(PlatformComInterop.invokeObjectMethod(pointer, 24).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Foreground.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 25, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_HorizontalContentAlignment: RuntimeProperty<HorizontalAlignment> =
      RuntimeProperty<HorizontalAlignment>(HorizontalAlignment.fromValue(0))

  public var horizontalContentAlignment: HorizontalAlignment
    get() {
      if (pointer.isNull) {
        return backing_HorizontalContentAlignment.get()
      }
      return HorizontalAlignment.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          36).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_HorizontalContentAlignment.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 37, value.value).getOrThrow()
    }

  private val backing_IsEnabled: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  public var isEnabled: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsEnabled.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 28).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_IsEnabled.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 29, value.value).getOrThrow()
    }

  private val backing_IsFocusEngaged: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  public var isFocusEngaged: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsFocusEngaged.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 8).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_IsFocusEngaged.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 9, value.value).getOrThrow()
    }

  private val backing_IsFocusEngagementEnabled: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  public var isFocusEngagementEnabled: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsFocusEngagementEnabled.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 6).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_IsFocusEngagementEnabled.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 7, value.value).getOrThrow()
    }

  private val backing_IsTextScaleFactorEnabled: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  public var isTextScaleFactorEnabled: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsTextScaleFactorEnabled.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 26).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_IsTextScaleFactorEnabled.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 27, value.value).getOrThrow()
    }

  private val backing_Padding: RuntimeProperty<Thickness> =
      RuntimeProperty<Thickness>(Thickness.fromAbi(ComStructValue(Thickness.ABI_LAYOUT,
      ByteArray(Thickness.ABI_LAYOUT.byteSize))))

  public var padding: Thickness
    get() {
      if (pointer.isNull) {
        return backing_Padding.get()
      }
      return Thickness.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 34,
          Thickness.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Padding.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 35, value.toAbi()).getOrThrow()
    }

  private val backing_RequiresPointer: RuntimeProperty<RequiresPointer> =
      RuntimeProperty<RequiresPointer>(RequiresPointer.fromValue(0))

  public var requiresPointer: RequiresPointer
    get() {
      if (pointer.isNull) {
        return backing_RequiresPointer.get()
      }
      return RequiresPointer.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          10).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_RequiresPointer.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 11, value.value).getOrThrow()
    }

  private val backing_TabNavigation: RuntimeProperty<KeyboardNavigationMode> =
      RuntimeProperty<KeyboardNavigationMode>(KeyboardNavigationMode.fromValue(0))

  public var tabNavigation: KeyboardNavigationMode
    get() {
      if (pointer.isNull) {
        return backing_TabNavigation.get()
      }
      return KeyboardNavigationMode.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          30).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_TabNavigation.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 31, value.value).getOrThrow()
    }

  private val backing_Template: RuntimeProperty<ControlTemplate> =
      RuntimeProperty<ControlTemplate>(ControlTemplate(ComPtr.NULL))

  public var template: ControlTemplate
    get() {
      if (pointer.isNull) {
        return backing_Template.get()
      }
      return ControlTemplate(PlatformComInterop.invokeObjectMethod(pointer, 32).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Template.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 33, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_VerticalContentAlignment: RuntimeProperty<VerticalAlignment> =
      RuntimeProperty<VerticalAlignment>(VerticalAlignment.fromValue(0))

  public var verticalContentAlignment: VerticalAlignment
    get() {
      if (pointer.isNull) {
        return backing_VerticalContentAlignment.get()
      }
      return VerticalAlignment.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          38).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_VerticalContentAlignment.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 39, value.value).getOrThrow()
    }

  private val focusEngagedEventSlot: FocusEngagedEvent = FocusEngagedEvent()

  public val focusEngagedEvent: FocusEngagedEvent
    get() = focusEngagedEventSlot

  private val focusDisengagedEventSlot: FocusDisengagedEvent = FocusDisengagedEvent()

  public val focusDisengagedEvent: FocusDisengagedEvent
    get() = focusDisengagedEventSlot

  public constructor() : this(Companion.factoryCreateInstance().pointer)

  override fun getTemplateChild(childName: String): DependencyObject {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetTemplateChild")
    }
    return DependencyObject(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 8,
        childName).getOrThrow())
  }

  public fun onPointerEntered(e: PointerRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 6, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.PointerRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.PointerRoutedEventArgs;{66e78a9a-1bec-5f92-b1a1-ea6334ee511c})")).getOrThrow()
  }

  public fun onPointerPressed(e: PointerRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 7, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.PointerRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.PointerRoutedEventArgs;{66e78a9a-1bec-5f92-b1a1-ea6334ee511c})")).getOrThrow()
  }

  public fun onPointerMoved(e: PointerRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 8, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.PointerRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.PointerRoutedEventArgs;{66e78a9a-1bec-5f92-b1a1-ea6334ee511c})")).getOrThrow()
  }

  public fun onPointerReleased(e: PointerRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 9, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.PointerRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.PointerRoutedEventArgs;{66e78a9a-1bec-5f92-b1a1-ea6334ee511c})")).getOrThrow()
  }

  public fun onPointerExited(e: PointerRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 10, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.PointerRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.PointerRoutedEventArgs;{66e78a9a-1bec-5f92-b1a1-ea6334ee511c})")).getOrThrow()
  }

  public fun onPointerCaptureLost(e: PointerRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 11, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.PointerRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.PointerRoutedEventArgs;{66e78a9a-1bec-5f92-b1a1-ea6334ee511c})")).getOrThrow()
  }

  public fun onPointerCanceled(e: PointerRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 12, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.PointerRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.PointerRoutedEventArgs;{66e78a9a-1bec-5f92-b1a1-ea6334ee511c})")).getOrThrow()
  }

  public fun onPointerWheelChanged(e: PointerRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 13, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.PointerRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.PointerRoutedEventArgs;{66e78a9a-1bec-5f92-b1a1-ea6334ee511c})")).getOrThrow()
  }

  public fun onTapped(e: TappedRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 14, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.TappedRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.TappedRoutedEventArgs;{73f74b8c-3709-547e-8e0c-51c03c89126a})")).getOrThrow()
  }

  public fun onDoubleTapped(e: DoubleTappedRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 15, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.DoubleTappedRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.DoubleTappedRoutedEventArgs;{32b9549d-11d8-53a5-a953-02409537a11f})")).getOrThrow()
  }

  public fun onHolding(e: HoldingRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 16, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.HoldingRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.HoldingRoutedEventArgs;{8272a4b2-2221-551e-b0bb-16e29138ab20})")).getOrThrow()
  }

  public fun onRightTapped(e: RightTappedRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 17, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.RightTappedRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.RightTappedRoutedEventArgs;{3972fafb-2915-5c62-bb6b-54ad84ff400d})")).getOrThrow()
  }

  public fun onManipulationStarting(e: ManipulationStartingRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 18, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.ManipulationStartingRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.ManipulationStartingRoutedEventArgs;{93a99f86-f5a0-5326-91b0-851c897af79f})")).getOrThrow()
  }

  public fun onManipulationInertiaStarting(e: ManipulationInertiaStartingRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 19, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.ManipulationInertiaStartingRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.ManipulationInertiaStartingRoutedEventArgs;{17d510be-5514-5952-9afd-959b60ab9394})")).getOrThrow()
  }

  public fun onManipulationStarted(e: ManipulationStartedRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 20, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.ManipulationStartedRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.ManipulationStartedRoutedEventArgs;{61857950-5821-5652-9fdf-c6277c5886f5})")).getOrThrow()
  }

  public fun onManipulationDelta(e: ManipulationDeltaRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 21, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.ManipulationDeltaRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.ManipulationDeltaRoutedEventArgs;{51369745-960f-54ac-93fa-763d22910dea})")).getOrThrow()
  }

  public fun onManipulationCompleted(e: ManipulationCompletedRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 22, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.ManipulationCompletedRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.ManipulationCompletedRoutedEventArgs;{e3be9e4e-c5fb-5859-a81d-ce12fc3a2f4d})")).getOrThrow()
  }

  public fun onKeyUp(e: KeyRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 23, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.KeyRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.KeyRoutedEventArgs;{ee357007-a2d6-5c75-9431-05fd66ec7915})")).getOrThrow()
  }

  public fun onKeyDown(e: KeyRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 24, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.KeyRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.KeyRoutedEventArgs;{ee357007-a2d6-5c75-9431-05fd66ec7915})")).getOrThrow()
  }

  public fun onPreviewKeyDown(e: KeyRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 25, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.KeyRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.KeyRoutedEventArgs;{ee357007-a2d6-5c75-9431-05fd66ec7915})")).getOrThrow()
  }

  public fun onPreviewKeyUp(e: KeyRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 26, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.KeyRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.KeyRoutedEventArgs;{ee357007-a2d6-5c75-9431-05fd66ec7915})")).getOrThrow()
  }

  public fun onGotFocus(e: RoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 27, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.RoutedEventArgs",
        "rc(Microsoft.UI.Xaml.RoutedEventArgs;{0908c407-1c7d-5de3-9c50-d971c62ec8ec})")).getOrThrow()
  }

  public fun onLostFocus(e: RoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 28, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.RoutedEventArgs",
        "rc(Microsoft.UI.Xaml.RoutedEventArgs;{0908c407-1c7d-5de3-9c50-d971c62ec8ec})")).getOrThrow()
  }

  public fun onCharacterReceived(e: CharacterReceivedRoutedEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 29, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.Input.CharacterReceivedRoutedEventArgs",
        "rc(Microsoft.UI.Xaml.Input.CharacterReceivedRoutedEventArgs;{e26ca5bb-34c3-5c1e-9a16-00b80b07a899})")).getOrThrow()
  }

  public fun onDragEnter(e: DragEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 30, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.DragEventArgs",
        "rc(Microsoft.UI.Xaml.DragEventArgs;{47ac5757-e4bc-52ba-8ab9-1bf81aad7900})")).getOrThrow()
  }

  public fun onDragLeave(e: DragEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 31, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.DragEventArgs",
        "rc(Microsoft.UI.Xaml.DragEventArgs;{47ac5757-e4bc-52ba-8ab9-1bf81aad7900})")).getOrThrow()
  }

  public fun onDragOver(e: DragEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 32, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.DragEventArgs",
        "rc(Microsoft.UI.Xaml.DragEventArgs;{47ac5757-e4bc-52ba-8ab9-1bf81aad7900})")).getOrThrow()
  }

  public fun onDrop(e: DragEventArgs) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeObjectSetter(pointer, 33, projectedObjectArgumentPointer(e,
        "Microsoft.UI.Xaml.DragEventArgs",
        "rc(Microsoft.UI.Xaml.DragEventArgs;{47ac5757-e4bc-52ba-8ab9-1bf81aad7900})")).getOrThrow()
  }

  public fun add_FocusEngaged(handler: TypedEventHandler<Control, FocusEngagedEventArgs>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 54,
        EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.Controls.Control, Microsoft.UI.Xaml.Controls.FocusEngagedEventArgs>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.Controls.Control;{857d6e8a-d45a-5c69-a99c-bf6a5c54fb38});rc(Microsoft.UI.Xaml.Controls.FocusEngagedEventArgs;{1e71e8e4-74b2-50a1-8f2b-42c0118ab0ea}))")).getOrThrow())
  }

  public fun remove_FocusEngaged(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 55, token.toAbi()).getOrThrow()
  }

  public fun add_FocusDisengaged(handler: TypedEventHandler<Control, FocusDisengagedEventArgs>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 56,
        EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.Controls.Control, Microsoft.UI.Xaml.Controls.FocusDisengagedEventArgs>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.Controls.Control;{857d6e8a-d45a-5c69-a99c-bf6a5c54fb38});rc(Microsoft.UI.Xaml.Controls.FocusDisengagedEventArgs;{c0b4b88c-c195-5064-84c7-33cb262cb240}))")).getOrThrow())
  }

  public fun remove_FocusDisengaged(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 57, token.toAbi()).getOrThrow()
  }

  public fun add_IsEnabledChanged(handler: DependencyPropertyChangedEventHandler):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 58,
        EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.DependencyPropertyChangedEventHandler",
        "delegate({4be8dc75-373d-5f4e-a0b4-54b9eeafb4a9})")).getOrThrow())
  }

  public fun remove_IsEnabledChanged(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 59, token.toAbi()).getOrThrow()
  }

  public fun removeFocusEngagement() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 60).getOrThrow()
  }

  public fun applyTemplate(): WinRtBoolean {
    if (pointer.isNull) {
      return WinRtBoolean.FALSE
    }
    return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 61).getOrThrow())
  }

  public inner class FocusEngagedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<Control, FocusEngagedEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 54,
        handler.pointer).getOrThrow())

    public fun subscribeScoped(handler: TypedEventHandler<Control, FocusEngagedEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (Control, FocusEngagedEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(Control(args[0] as
          ComPtr), FocusEngagedEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<Control, FocusEngagedEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (Control, FocusEngagedEventArgs) -> Unit): AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator fun plusAssign(handler: TypedEventHandler<Control, FocusEngagedEventArgs>) {
      subscribe(handler)
    }

    public operator fun invoke(handler: TypedEventHandler<Control, FocusEngagedEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (Control, FocusEngagedEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 55, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public inner class FocusDisengagedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<Control, FocusDisengagedEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 56,
        handler.pointer).getOrThrow())

    public fun subscribeScoped(handler: TypedEventHandler<Control, FocusDisengagedEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (Control, FocusDisengagedEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(Control(args[0] as
          ComPtr), FocusDisengagedEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<Control, FocusDisengagedEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (Control, FocusDisengagedEventArgs) -> Unit):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator fun plusAssign(handler: TypedEventHandler<Control, FocusDisengagedEventArgs>) {
      subscribe(handler)
    }

    public operator fun invoke(handler: TypedEventHandler<Control, FocusDisengagedEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (Control, FocusDisengagedEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

    public fun unsubscribe(token: EventRegistrationToken) {
      try {
        PlatformComInterop.invokeUnitMethodWithInt64Arg(pointer, 57, token.value).getOrThrow()
      } finally {
        delegateHandles.remove(token)?.close()
      }
    }

    public operator fun minusAssign(token: EventRegistrationToken) {
      unsubscribe(token)
    }
  }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.Control"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "Control")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IControl"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

    private val statics: IControlStatics by lazy { WinRtRuntime.projectActivationFactory(this,
        IControlStatics, ::IControlStatics) }

    public val backgroundProperty: DependencyProperty
      get() = statics.backgroundProperty

    public val backgroundSizingProperty: DependencyProperty
      get() = statics.backgroundSizingProperty

    public val borderBrushProperty: DependencyProperty
      get() = statics.borderBrushProperty

    public val borderThicknessProperty: DependencyProperty
      get() = statics.borderThicknessProperty

    public val characterSpacingProperty: DependencyProperty
      get() = statics.characterSpacingProperty

    public val cornerRadiusProperty: DependencyProperty
      get() = statics.cornerRadiusProperty

    public val defaultStyleKeyProperty: DependencyProperty
      get() = statics.defaultStyleKeyProperty

    public val defaultStyleResourceUriProperty: DependencyProperty
      get() = statics.defaultStyleResourceUriProperty

    public val elementSoundModeProperty: DependencyProperty
      get() = statics.elementSoundModeProperty

    public val fontFamilyProperty: DependencyProperty
      get() = statics.fontFamilyProperty

    public val fontSizeProperty: DependencyProperty
      get() = statics.fontSizeProperty

    public val fontStretchProperty: DependencyProperty
      get() = statics.fontStretchProperty

    public val fontStyleProperty: DependencyProperty
      get() = statics.fontStyleProperty

    public val fontWeightProperty: DependencyProperty
      get() = statics.fontWeightProperty

    public val foregroundProperty: DependencyProperty
      get() = statics.foregroundProperty

    public val horizontalContentAlignmentProperty: DependencyProperty
      get() = statics.horizontalContentAlignmentProperty

    public val isEnabledProperty: DependencyProperty
      get() = statics.isEnabledProperty

    public val isFocusEngagedProperty: DependencyProperty
      get() = statics.isFocusEngagedProperty

    public val isFocusEngagementEnabledProperty: DependencyProperty
      get() = statics.isFocusEngagementEnabledProperty

    public val isTemplateFocusTargetProperty: DependencyProperty
      get() = statics.isTemplateFocusTargetProperty

    public val isTemplateKeyTipTargetProperty: DependencyProperty
      get() = statics.isTemplateKeyTipTargetProperty

    public val isTextScaleFactorEnabledProperty: DependencyProperty
      get() = statics.isTextScaleFactorEnabledProperty

    public val paddingProperty: DependencyProperty
      get() = statics.paddingProperty

    public val requiresPointerProperty: DependencyProperty
      get() = statics.requiresPointerProperty

    public val tabNavigationProperty: DependencyProperty
      get() = statics.tabNavigationProperty

    public val templateProperty: DependencyProperty
      get() = statics.templateProperty

    public val verticalContentAlignmentProperty: DependencyProperty
      get() = statics.verticalContentAlignmentProperty

    private fun factoryCreateInstance(): Control {
      return WinRtRuntime.compose(this, guidOf("25159233-9438-5534-aeb9-00eb059cf73f"),
          guidOf("857d6e8a-d45a-5c69-a99c-bf6a5c54fb38"), ::Control, 6, ComPtr.NULL)
    }

    public fun getIsTemplateFocusTarget(element: FrameworkElement): WinRtBoolean =
        statics.getIsTemplateFocusTarget(element)

    public fun setIsTemplateFocusTarget(element: FrameworkElement, value: WinRtBoolean) {
      statics.setIsTemplateFocusTarget(element, value)
    }

    public fun getIsTemplateKeyTipTarget(element: DependencyObject): WinRtBoolean =
        statics.getIsTemplateKeyTipTarget(element)

    public fun setIsTemplateKeyTipTarget(element: DependencyObject, value: WinRtBoolean) {
      statics.setIsTemplateKeyTipTarget(element, value)
    }
  }
}
