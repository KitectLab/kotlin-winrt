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
import microsoft.ui.composition.CompositionBrush
import microsoft.ui.xaml.DependencyProperty
import microsoft.ui.xaml.FrameworkElement
import microsoft.ui.xaml.LineStackingStrategy
import microsoft.ui.xaml.OpticalMarginAlignment
import microsoft.ui.xaml.RoutedEventHandler
import microsoft.ui.xaml.TextAlignment
import microsoft.ui.xaml.TextLineBounds
import microsoft.ui.xaml.TextReadingOrder
import microsoft.ui.xaml.TextTrimming
import microsoft.ui.xaml.TextWrapping
import microsoft.ui.xaml.Thickness
import microsoft.ui.xaml.controls.primitives.FlyoutBase
import microsoft.ui.xaml.documents.InlineCollection
import microsoft.ui.xaml.documents.TextHighlighter
import microsoft.ui.xaml.documents.TextPointer
import microsoft.ui.xaml.media.Brush
import microsoft.ui.xaml.media.FontFamily
import microsoft.ui.xaml.media.SolidColorBrush
import windows.foundation.TypedEventHandler
import windows.foundation.collections.IVector
import windows.ui.text.FontStretch
import windows.ui.text.FontStyle
import windows.ui.text.FontWeight
import windows.ui.text.TextDecorations

public open class TextBlock(
  pointer: ComPtr,
) : FrameworkElement(pointer) {
  private val backing_BaselineOffset: RuntimeProperty<Float64> =
      RuntimeProperty<Float64>(Float64(0.0))

  public val baselineOffset: Float64
    get() {
      if (pointer.isNull) {
        return backing_BaselineOffset.get()
      }
      return Float64(PlatformComInterop.invokeFloat64Method(pointer, 42).getOrThrow())
    }

  private val backing_CharacterSpacing: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public var characterSpacing: Int32
    get() {
      if (pointer.isNull) {
        return backing_CharacterSpacing.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 16).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_CharacterSpacing.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 17, value.value).getOrThrow()
    }

  private val backing_ContentEnd: RuntimeProperty<TextPointer> =
      RuntimeProperty<TextPointer>(TextPointer(ComPtr.NULL))

  public val contentEnd: TextPointer
    get() {
      if (pointer.isNull) {
        return backing_ContentEnd.get()
      }
      return TextPointer(PlatformComInterop.invokeObjectMethod(pointer, 39).getOrThrow())
    }

  private val backing_ContentStart: RuntimeProperty<TextPointer> =
      RuntimeProperty<TextPointer>(TextPointer(ComPtr.NULL))

  public val contentStart: TextPointer
    get() {
      if (pointer.isNull) {
        return backing_ContentStart.get()
      }
      return TextPointer(PlatformComInterop.invokeObjectMethod(pointer, 38).getOrThrow())
    }

  private val backing_FontFamily: RuntimeProperty<FontFamily> =
      RuntimeProperty<FontFamily>(FontFamily(ComPtr.NULL))

  public var fontFamily: FontFamily
    get() {
      if (pointer.isNull) {
        return backing_FontFamily.get()
      }
      return FontFamily(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_FontFamily.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 9, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_FontSize: RuntimeProperty<Float64> = RuntimeProperty<Float64>(Float64(0.0))

  public var fontSize: Float64
    get() {
      if (pointer.isNull) {
        return backing_FontSize.get()
      }
      return Float64(PlatformComInterop.invokeFloat64Method(pointer, 6).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_FontSize.set(value)
        return
      }
      PlatformComInterop.invokeFloat64Setter(pointer, 7, value.value).getOrThrow()
    }

  private val backing_FontStretch: RuntimeProperty<FontStretch> =
      RuntimeProperty<FontStretch>(FontStretch.fromValue(0))

  public var fontStretch: FontStretch
    get() {
      if (pointer.isNull) {
        return backing_FontStretch.get()
      }
      return FontStretch.fromValue(PlatformComInterop.invokeInt32Method(pointer, 14).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_FontStretch.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 15, value.value).getOrThrow()
    }

  private val backing_FontStyle: RuntimeProperty<FontStyle> =
      RuntimeProperty<FontStyle>(FontStyle.fromValue(0))

  public var fontStyle: FontStyle
    get() {
      if (pointer.isNull) {
        return backing_FontStyle.get()
      }
      return FontStyle.fromValue(PlatformComInterop.invokeInt32Method(pointer, 12).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_FontStyle.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 13, value.value).getOrThrow()
    }

  private val backing_FontWeight: RuntimeProperty<FontWeight> =
      RuntimeProperty<FontWeight>(FontWeight.fromAbi(ComStructValue(FontWeight.ABI_LAYOUT,
      ByteArray(FontWeight.ABI_LAYOUT.byteSize))))

  public var fontWeight: FontWeight
    get() {
      if (pointer.isNull) {
        return backing_FontWeight.get()
      }
      return FontWeight.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 10,
          FontWeight.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_FontWeight.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 11, value.toAbi()).getOrThrow()
    }

  private val backing_Foreground: RuntimeProperty<Brush> =
      RuntimeProperty<Brush>(Brush(ComPtr.NULL))

  public var foreground: Brush
    get() {
      if (pointer.isNull) {
        return backing_Foreground.get()
      }
      return Brush(PlatformComInterop.invokeObjectMethod(pointer, 18).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Foreground.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 19, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_HorizontalTextAlignment: RuntimeProperty<TextAlignment> =
      RuntimeProperty<TextAlignment>(TextAlignment.fromValue(0))

  public var horizontalTextAlignment: TextAlignment
    get() {
      if (pointer.isNull) {
        return backing_HorizontalTextAlignment.get()
      }
      return TextAlignment.fromValue(PlatformComInterop.invokeInt32Method(pointer, 60).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_HorizontalTextAlignment.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 61, value.value).getOrThrow()
    }

  private val backing_Inlines: RuntimeProperty<InlineCollection> =
      RuntimeProperty<InlineCollection>(InlineCollection(ComPtr.NULL))

  public val inlines: InlineCollection
    get() {
      if (pointer.isNull) {
        return backing_Inlines.get()
      }
      return InlineCollection(PlatformComInterop.invokeObjectMethod(pointer, 28).getOrThrow())
    }

  private val backing_IsColorFontEnabled: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  public var isColorFontEnabled: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsColorFontEnabled.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 51).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_IsColorFontEnabled.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 52, value.value).getOrThrow()
    }

  private val backing_IsTextScaleFactorEnabled: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  public var isTextScaleFactorEnabled: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsTextScaleFactorEnabled.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 55).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_IsTextScaleFactorEnabled.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 56, value.value).getOrThrow()
    }

  private val backing_IsTextSelectionEnabled: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  public var isTextSelectionEnabled: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsTextSelectionEnabled.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 35).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_IsTextSelectionEnabled.set(value)
        return
      }
      PlatformComInterop.invokeBooleanSetter(pointer, 36, value.value).getOrThrow()
    }

  private val backing_IsTextTrimmed: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  public val isTextTrimmed: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsTextTrimmed.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 59).getOrThrow())
    }

  private val backing_LineHeight: RuntimeProperty<Float64> = RuntimeProperty<Float64>(Float64(0.0))

  public var lineHeight: Float64
    get() {
      if (pointer.isNull) {
        return backing_LineHeight.get()
      }
      return Float64(PlatformComInterop.invokeFloat64Method(pointer, 31).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_LineHeight.set(value)
        return
      }
      PlatformComInterop.invokeFloat64Setter(pointer, 32, value.value).getOrThrow()
    }

  private val backing_LineStackingStrategy: RuntimeProperty<LineStackingStrategy> =
      RuntimeProperty<LineStackingStrategy>(LineStackingStrategy.fromValue(0))

  public var lineStackingStrategy: LineStackingStrategy
    get() {
      if (pointer.isNull) {
        return backing_LineStackingStrategy.get()
      }
      return LineStackingStrategy.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          33).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_LineStackingStrategy.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 34, value.value).getOrThrow()
    }

  private val backing_MaxLines: RuntimeProperty<Int32> = RuntimeProperty<Int32>(Int32(0))

  public var maxLines: Int32
    get() {
      if (pointer.isNull) {
        return backing_MaxLines.get()
      }
      return Int32(PlatformComInterop.invokeInt32Method(pointer, 45).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_MaxLines.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 46, value.value).getOrThrow()
    }

  private val backing_OpticalMarginAlignment: RuntimeProperty<OpticalMarginAlignment> =
      RuntimeProperty<OpticalMarginAlignment>(OpticalMarginAlignment.fromValue(0))

  public var opticalMarginAlignment: OpticalMarginAlignment
    get() {
      if (pointer.isNull) {
        return backing_OpticalMarginAlignment.get()
      }
      return OpticalMarginAlignment.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          49).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_OpticalMarginAlignment.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 50, value.value).getOrThrow()
    }

  private val backing_Padding: RuntimeProperty<Thickness> =
      RuntimeProperty<Thickness>(Thickness.fromAbi(ComStructValue(Thickness.ABI_LAYOUT,
      ByteArray(Thickness.ABI_LAYOUT.byteSize))))

  public var padding: Thickness
    get() {
      if (pointer.isNull) {
        return backing_Padding.get()
      }
      return Thickness.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 29,
          Thickness.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Padding.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 30, value.toAbi()).getOrThrow()
    }

  private val backing_SelectedText: RuntimeProperty<String> = RuntimeProperty<String>("")

  public val selectedText: String
    get() {
      if (pointer.isNull) {
        return backing_SelectedText.get()
      }
      return run {
            val value = PlatformComInterop.invokeHStringMethod(pointer, 37).getOrThrow()
            try {
              value.toKotlinString()
            } finally {
              value.close()
            }
          }
    }

  private val backing_SelectionEnd: RuntimeProperty<TextPointer> =
      RuntimeProperty<TextPointer>(TextPointer(ComPtr.NULL))

  public val selectionEnd: TextPointer
    get() {
      if (pointer.isNull) {
        return backing_SelectionEnd.get()
      }
      return TextPointer(PlatformComInterop.invokeObjectMethod(pointer, 41).getOrThrow())
    }

  private val backing_SelectionFlyout: RuntimeProperty<FlyoutBase> =
      RuntimeProperty<FlyoutBase>(FlyoutBase(ComPtr.NULL))

  public var selectionFlyout: FlyoutBase
    get() {
      if (pointer.isNull) {
        return backing_SelectionFlyout.get()
      }
      return FlyoutBase(PlatformComInterop.invokeObjectMethod(pointer, 63).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_SelectionFlyout.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 64, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_SelectionHighlightColor: RuntimeProperty<SolidColorBrush> =
      RuntimeProperty<SolidColorBrush>(SolidColorBrush(ComPtr.NULL))

  public var selectionHighlightColor: SolidColorBrush
    get() {
      if (pointer.isNull) {
        return backing_SelectionHighlightColor.get()
      }
      return SolidColorBrush(PlatformComInterop.invokeObjectMethod(pointer, 43).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_SelectionHighlightColor.set(value)
        return
      }
      PlatformComInterop.invokeObjectSetter(pointer, 44, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_SelectionStart: RuntimeProperty<TextPointer> =
      RuntimeProperty<TextPointer>(TextPointer(ComPtr.NULL))

  public val selectionStart: TextPointer
    get() {
      if (pointer.isNull) {
        return backing_SelectionStart.get()
      }
      return TextPointer(PlatformComInterop.invokeObjectMethod(pointer, 40).getOrThrow())
    }

  private val backing_Text: RuntimeProperty<String> = RuntimeProperty<String>("")

  public var text: String
    get() {
      if (pointer.isNull) {
        return backing_Text.get()
      }
      return run {
            val value = PlatformComInterop.invokeHStringMethod(pointer, 26).getOrThrow()
            try {
              value.toKotlinString()
            } finally {
              value.close()
            }
          }
    }
    set(value) {
      if (pointer.isNull) {
        backing_Text.set(value)
        return
      }
      PlatformComInterop.invokeStringSetter(pointer, 27, value).getOrThrow()
    }

  private val backing_TextAlignment: RuntimeProperty<TextAlignment> =
      RuntimeProperty<TextAlignment>(TextAlignment.fromValue(0))

  public var textAlignment: TextAlignment
    get() {
      if (pointer.isNull) {
        return backing_TextAlignment.get()
      }
      return TextAlignment.fromValue(PlatformComInterop.invokeInt32Method(pointer, 24).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_TextAlignment.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 25, value.value).getOrThrow()
    }

  private val backing_TextDecorations: RuntimeProperty<TextDecorations> =
      RuntimeProperty<TextDecorations>(TextDecorations.fromValue(0u))

  public var textDecorations: TextDecorations
    get() {
      if (pointer.isNull) {
        return backing_TextDecorations.get()
      }
      return TextDecorations.fromValue(PlatformComInterop.invokeUInt32Method(pointer,
          57).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_TextDecorations.set(value)
        return
      }
      PlatformComInterop.invokeUInt32Setter(pointer, 58, value.value).getOrThrow()
    }

  private val backing_TextLineBounds: RuntimeProperty<TextLineBounds> =
      RuntimeProperty<TextLineBounds>(TextLineBounds.fromValue(0))

  public var textLineBounds: TextLineBounds
    get() {
      if (pointer.isNull) {
        return backing_TextLineBounds.get()
      }
      return TextLineBounds.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          47).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_TextLineBounds.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 48, value.value).getOrThrow()
    }

  private val backing_TextReadingOrder: RuntimeProperty<TextReadingOrder> =
      RuntimeProperty<TextReadingOrder>(TextReadingOrder.fromValue(0))

  public var textReadingOrder: TextReadingOrder
    get() {
      if (pointer.isNull) {
        return backing_TextReadingOrder.get()
      }
      return TextReadingOrder.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          53).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_TextReadingOrder.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 54, value.value).getOrThrow()
    }

  private val backing_TextTrimming: RuntimeProperty<TextTrimming> =
      RuntimeProperty<TextTrimming>(TextTrimming.fromValue(0))

  public var textTrimming: TextTrimming
    get() {
      if (pointer.isNull) {
        return backing_TextTrimming.get()
      }
      return TextTrimming.fromValue(PlatformComInterop.invokeInt32Method(pointer, 22).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_TextTrimming.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 23, value.value).getOrThrow()
    }

  private val backing_TextWrapping: RuntimeProperty<TextWrapping> =
      RuntimeProperty<TextWrapping>(TextWrapping.fromValue(0))

  public var textWrapping: TextWrapping
    get() {
      if (pointer.isNull) {
        return backing_TextWrapping.get()
      }
      return TextWrapping.fromValue(PlatformComInterop.invokeInt32Method(pointer, 20).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_TextWrapping.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 21, value.value).getOrThrow()
    }

  private val backing_CharacterSpacingProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val characterSpacingProperty: DependencyProperty
    get() = backing_CharacterSpacingProperty.get()

  private val backing_FontFamilyProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val fontFamilyProperty: DependencyProperty
    get() = backing_FontFamilyProperty.get()

  private val backing_FontSizeProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val fontSizeProperty: DependencyProperty
    get() = backing_FontSizeProperty.get()

  private val backing_FontStretchProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val fontStretchProperty: DependencyProperty
    get() = backing_FontStretchProperty.get()

  private val backing_FontStyleProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val fontStyleProperty: DependencyProperty
    get() = backing_FontStyleProperty.get()

  private val backing_FontWeightProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val fontWeightProperty: DependencyProperty
    get() = backing_FontWeightProperty.get()

  private val backing_ForegroundProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val foregroundProperty: DependencyProperty
    get() = backing_ForegroundProperty.get()

  private val backing_HorizontalTextAlignmentProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val horizontalTextAlignmentProperty: DependencyProperty
    get() = backing_HorizontalTextAlignmentProperty.get()

  private val backing_IsColorFontEnabledProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val isColorFontEnabledProperty: DependencyProperty
    get() = backing_IsColorFontEnabledProperty.get()

  private val backing_IsTextScaleFactorEnabledProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val isTextScaleFactorEnabledProperty: DependencyProperty
    get() = backing_IsTextScaleFactorEnabledProperty.get()

  private val backing_IsTextSelectionEnabledProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val isTextSelectionEnabledProperty: DependencyProperty
    get() = backing_IsTextSelectionEnabledProperty.get()

  private val backing_IsTextTrimmedProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val isTextTrimmedProperty: DependencyProperty
    get() = backing_IsTextTrimmedProperty.get()

  private val backing_LineHeightProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val lineHeightProperty: DependencyProperty
    get() = backing_LineHeightProperty.get()

  private val backing_LineStackingStrategyProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val lineStackingStrategyProperty: DependencyProperty
    get() = backing_LineStackingStrategyProperty.get()

  private val backing_MaxLinesProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val maxLinesProperty: DependencyProperty
    get() = backing_MaxLinesProperty.get()

  private val backing_OpticalMarginAlignmentProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val opticalMarginAlignmentProperty: DependencyProperty
    get() = backing_OpticalMarginAlignmentProperty.get()

  private val backing_PaddingProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val paddingProperty: DependencyProperty
    get() = backing_PaddingProperty.get()

  private val backing_SelectedTextProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val selectedTextProperty: DependencyProperty
    get() = backing_SelectedTextProperty.get()

  private val backing_SelectionFlyoutProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val selectionFlyoutProperty: DependencyProperty
    get() = backing_SelectionFlyoutProperty.get()

  private val backing_SelectionHighlightColorProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val selectionHighlightColorProperty: DependencyProperty
    get() = backing_SelectionHighlightColorProperty.get()

  private val backing_TextAlignmentProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val textAlignmentProperty: DependencyProperty
    get() = backing_TextAlignmentProperty.get()

  private val backing_TextDecorationsProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val textDecorationsProperty: DependencyProperty
    get() = backing_TextDecorationsProperty.get()

  private val backing_TextLineBoundsProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val textLineBoundsProperty: DependencyProperty
    get() = backing_TextLineBoundsProperty.get()

  private val backing_TextProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val textProperty: DependencyProperty
    get() = backing_TextProperty.get()

  private val backing_TextReadingOrderProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val textReadingOrderProperty: DependencyProperty
    get() = backing_TextReadingOrderProperty.get()

  private val backing_TextTrimmingProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val textTrimmingProperty: DependencyProperty
    get() = backing_TextTrimmingProperty.get()

  private val backing_TextWrappingProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val textWrappingProperty: DependencyProperty
    get() = backing_TextWrappingProperty.get()

  private val isTextTrimmedChangedEventSlot: IsTextTrimmedChangedEvent = IsTextTrimmedChangedEvent()

  public val isTextTrimmedChangedEvent: IsTextTrimmedChangedEvent
    get() = isTextTrimmedChangedEventSlot

  public constructor() : this(Companion.activate().pointer)

  public fun get_TextHighlighters(): IVector<TextHighlighter> {
    if (pointer.isNull) {
      error("Null runtime object pointer: get_TextHighlighters")
    }
    return IVector<TextHighlighter>.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        62).getOrThrow()))
  }

  public fun add_SelectionChanged(handler: RoutedEventHandler): EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 65,
        EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.RoutedEventHandler",
        "delegate({dae23d85-69ca-5bdf-805b-6161a3a215cc})")).getOrThrow())
  }

  public fun remove_SelectionChanged(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 66, token.toAbi()).getOrThrow()
  }

  public fun add_ContextMenuOpening(handler: ContextMenuOpeningEventHandler):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 67,
        EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Microsoft.UI.Xaml.Controls.ContextMenuOpeningEventHandler",
        "delegate({d010ff61-4067-526a-95a3-517577bc5273})")).getOrThrow())
  }

  public fun remove_ContextMenuOpening(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 68, token.toAbi()).getOrThrow()
  }

  public
      fun add_IsTextTrimmedChanged(handler: TypedEventHandler<TextBlock, IsTextTrimmedChangedEventArgs>):
      EventRegistrationToken {
    if (pointer.isNull) {
      return EventRegistrationToken.fromAbi(ComStructValue(EventRegistrationToken.ABI_LAYOUT,
          ByteArray(EventRegistrationToken.ABI_LAYOUT.byteSize)))
    }
    return EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 69,
        EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
        "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.Controls.TextBlock, Microsoft.UI.Xaml.Controls.IsTextTrimmedChangedEventArgs>",
        "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.Controls.TextBlock;{1ac8d84f-392c-5c7e-83f5-a53e3bf0abb0});rc(Microsoft.UI.Xaml.Controls.IsTextTrimmedChangedEventArgs;{3c709b2f-16ba-55d2-b6f6-dfc54a1ed021}))")).getOrThrow())
  }

  public fun remove_IsTextTrimmedChanged(token: EventRegistrationToken) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 70, token.toAbi()).getOrThrow()
  }

  public fun selectAll() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 71).getOrThrow()
  }

  public fun select(start: TextPointer, end: TextPointer) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithTwoObjectArgs(pointer, 72,
        projectedObjectArgumentPointer(start, "Microsoft.UI.Xaml.Documents.TextPointer",
        "rc(Microsoft.UI.Xaml.Documents.TextPointer;{842eb385-ee41-5930-979b-438fa7525a51})"),
        projectedObjectArgumentPointer(end, "Microsoft.UI.Xaml.Documents.TextPointer",
        "rc(Microsoft.UI.Xaml.Documents.TextPointer;{842eb385-ee41-5930-979b-438fa7525a51})")).getOrThrow()
  }

  public fun getAlphaMask(): CompositionBrush {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetAlphaMask")
    }
    return CompositionBrush(PlatformComInterop.invokeObjectMethod(pointer, 73).getOrThrow())
  }

  public fun copySelectionToClipboard() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 74).getOrThrow()
  }

  public inner class IsTextTrimmedChangedEvent {
    private val delegateHandles: MutableMap<EventRegistrationToken, WinRtDelegateHandle> =
        mutableMapOf()

    public fun subscribe(handler: TypedEventHandler<TextBlock, IsTextTrimmedChangedEventArgs>):
        EventRegistrationToken =
        EventRegistrationToken(PlatformComInterop.invokeInt64MethodWithObjectArg(pointer, 69,
        handler.pointer).getOrThrow())

    public
        fun subscribeScoped(handler: TypedEventHandler<TextBlock, IsTextTrimmedChangedEventArgs>):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public fun subscribe(handler: (TextBlock, IsTextTrimmedChangedEventArgs) -> Unit):
        EventRegistrationToken {
      val delegateHandle =
          WinRtDelegateBridge.createUnitDelegate(guidOf("9de1c534-6ae1-11e0-84e1-18a905bcc53f"),
          listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
          dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args -> handler(TextBlock(args[0] as
          ComPtr), IsTextTrimmedChangedEventArgs(args[1] as ComPtr)) }
      try {
        val token =
            subscribe(TypedEventHandler<TextBlock, IsTextTrimmedChangedEventArgs>(delegateHandle.pointer))
        delegateHandles[token] = delegateHandle
        return token
      } catch (t: Throwable) {
        delegateHandle.close()
        throw t
      }
    }

    public fun subscribeScoped(handler: (TextBlock, IsTextTrimmedChangedEventArgs) -> Unit):
        AutoCloseable {
      val token = subscribe(handler)
      return AutoCloseable { unsubscribe(token) }
    }

    public operator
        fun plusAssign(handler: TypedEventHandler<TextBlock, IsTextTrimmedChangedEventArgs>) {
      subscribe(handler)
    }

    public operator
        fun invoke(handler: TypedEventHandler<TextBlock, IsTextTrimmedChangedEventArgs>):
        EventRegistrationToken = subscribe(handler)

    public operator fun invoke(handler: (TextBlock, IsTextTrimmedChangedEventArgs) -> Unit):
        EventRegistrationToken = subscribe(handler)

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

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.TextBlock"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "TextBlock")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.ITextBlock"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    private val statics: ITextBlockStatics by lazy { WinRtRuntime.projectActivationFactory(this,
        ITextBlockStatics, ::ITextBlockStatics) }

    public val characterSpacingProperty: DependencyProperty
      get() = statics.characterSpacingProperty

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

    public val horizontalTextAlignmentProperty: DependencyProperty
      get() = statics.horizontalTextAlignmentProperty

    public val isColorFontEnabledProperty: DependencyProperty
      get() = statics.isColorFontEnabledProperty

    public val isTextScaleFactorEnabledProperty: DependencyProperty
      get() = statics.isTextScaleFactorEnabledProperty

    public val isTextSelectionEnabledProperty: DependencyProperty
      get() = statics.isTextSelectionEnabledProperty

    public val isTextTrimmedProperty: DependencyProperty
      get() = statics.isTextTrimmedProperty

    public val lineHeightProperty: DependencyProperty
      get() = statics.lineHeightProperty

    public val lineStackingStrategyProperty: DependencyProperty
      get() = statics.lineStackingStrategyProperty

    public val maxLinesProperty: DependencyProperty
      get() = statics.maxLinesProperty

    public val opticalMarginAlignmentProperty: DependencyProperty
      get() = statics.opticalMarginAlignmentProperty

    public val paddingProperty: DependencyProperty
      get() = statics.paddingProperty

    public val selectedTextProperty: DependencyProperty
      get() = statics.selectedTextProperty

    public val selectionFlyoutProperty: DependencyProperty
      get() = statics.selectionFlyoutProperty

    public val selectionHighlightColorProperty: DependencyProperty
      get() = statics.selectionHighlightColorProperty

    public val textAlignmentProperty: DependencyProperty
      get() = statics.textAlignmentProperty

    public val textDecorationsProperty: DependencyProperty
      get() = statics.textDecorationsProperty

    public val textLineBoundsProperty: DependencyProperty
      get() = statics.textLineBoundsProperty

    public val textProperty: DependencyProperty
      get() = statics.textProperty

    public val textReadingOrderProperty: DependencyProperty
      get() = statics.textReadingOrderProperty

    public val textTrimmingProperty: DependencyProperty
      get() = statics.textTrimmingProperty

    public val textWrappingProperty: DependencyProperty
      get() = statics.textWrappingProperty

    public fun activate(): TextBlock = WinRtRuntime.activate(this, ::TextBlock)
  }
}
