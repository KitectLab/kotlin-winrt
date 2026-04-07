package microsoft.ui.xaml.controls

import dev.winrt.core.EventRegistrationToken
import dev.winrt.core.Float64
import dev.winrt.core.Inspectable
import dev.winrt.core.Int32
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
import microsoft.ui.composition.CompositionBrush
import microsoft.ui.xaml.LineStackingStrategy
import microsoft.ui.xaml.OpticalMarginAlignment
import microsoft.ui.xaml.RoutedEventArgs
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

public interface ITextBlock {
  public val baselineOffset: Float64

  public var characterSpacing: Int32

  public val contentEnd: TextPointer

  public val contentStart: TextPointer

  public var fontFamily: FontFamily

  public var fontSize: Float64

  public var fontStretch: FontStretch

  public var fontStyle: FontStyle

  public var fontWeight: FontWeight

  public var foreground: Brush

  public var horizontalTextAlignment: TextAlignment

  public val inlines: InlineCollection

  public var isColorFontEnabled: WinRtBoolean

  public var isTextScaleFactorEnabled: WinRtBoolean

  public var isTextSelectionEnabled: WinRtBoolean

  public val isTextTrimmed: WinRtBoolean

  public var lineHeight: Float64

  public var lineStackingStrategy: LineStackingStrategy

  public var maxLines: Int32

  public var opticalMarginAlignment: OpticalMarginAlignment

  public var padding: Thickness

  public val selectedText: String

  public val selectionEnd: TextPointer

  public var selectionFlyout: FlyoutBase

  public var selectionHighlightColor: SolidColorBrush

  public val selectionStart: TextPointer

  public var text: String

  public var textAlignment: TextAlignment

  public var textDecorations: TextDecorations

  public val textHighlighters: IVector<TextHighlighter>

  public var textLineBounds: TextLineBounds

  public var textReadingOrder: TextReadingOrder

  public var textTrimming: TextTrimming

  public var textWrapping: TextWrapping

  public fun add_SelectionChanged(handler: RoutedEventHandler): EventRegistrationToken

  public fun remove_SelectionChanged(token: EventRegistrationToken)

  public fun add_ContextMenuOpening(handler: ContextMenuOpeningEventHandler): EventRegistrationToken

  public fun remove_ContextMenuOpening(token: EventRegistrationToken)

  public
      fun add_IsTextTrimmedChanged(handler: TypedEventHandler<TextBlock, IsTextTrimmedChangedEventArgs>):
      EventRegistrationToken

  public fun remove_IsTextTrimmedChanged(token: EventRegistrationToken)

  public fun selectAll()

  public fun select(start: TextPointer, end: TextPointer)

  public fun getAlphaMask(): CompositionBrush

  public fun copySelectionToClipboard()

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.ITextBlock"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.Controls.ITextBlock"

    override val iid: Guid = guidOf("1ac8d84f-392c-5c7e-83f5-a53e3bf0abb0")

    public fun from(inspectable: Inspectable): ITextBlock = inspectable.projectInterface(this,
        ::ITextBlockProjection)

    public operator fun invoke(inspectable: Inspectable): ITextBlock = from(inspectable)
  }
}

private class ITextBlockProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    ITextBlock {
  override val baselineOffset: Float64
    get() = Float64(PlatformComInterop.invokeFloat64Method(pointer, 42).getOrThrow())

  override var characterSpacing: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 16).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 17, value.value).getOrThrow()
    }

  override val contentEnd: TextPointer
    get() = TextPointer(PlatformComInterop.invokeObjectMethod(pointer, 39).getOrThrow())

  override val contentStart: TextPointer
    get() = TextPointer(PlatformComInterop.invokeObjectMethod(pointer, 38).getOrThrow())

  override var fontFamily: FontFamily
    get() = FontFamily(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 9, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.Media.FontFamily",
          "rc(Microsoft.UI.Xaml.Media.FontFamily;{18fa5bc1-7294-527c-bb02-b213e0b3a2a3})")).getOrThrow()
    }

  override var fontSize: Float64
    get() = Float64(PlatformComInterop.invokeFloat64Method(pointer, 6).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithFloat64Arg(pointer, 7, value.value).getOrThrow()
    }

  override var fontStretch: FontStretch
    get() = FontStretch.fromValue(PlatformComInterop.invokeInt32Method(pointer, 14).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 15, value.value).getOrThrow()
    }

  override var fontStyle: FontStyle
    get() = FontStyle.fromValue(PlatformComInterop.invokeInt32Method(pointer, 12).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 13, value.value).getOrThrow()
    }

  override var fontWeight: FontWeight
    get() = FontWeight.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 10,
        FontWeight.ABI_LAYOUT).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 11, value.toAbi()).getOrThrow()
    }

  override var foreground: Brush
    get() = Brush(PlatformComInterop.invokeObjectMethod(pointer, 18).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 19, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.Media.Brush",
          "rc(Microsoft.UI.Xaml.Media.Brush;{2de3cb83-1329-5679-88f8-c822bc5442cb})")).getOrThrow()
    }

  override var horizontalTextAlignment: TextAlignment
    get() = TextAlignment.fromValue(PlatformComInterop.invokeInt32Method(pointer, 60).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 61, value.value).getOrThrow()
    }

  override val inlines: InlineCollection
    get() = InlineCollection(PlatformComInterop.invokeObjectMethod(pointer, 28).getOrThrow())

  override var isColorFontEnabled: WinRtBoolean
    get() = WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 51).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithUInt32Arg(pointer, 52, if (value.value) 1u else 0u).getOrThrow()
    }

  override var isTextScaleFactorEnabled: WinRtBoolean
    get() = WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 55).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithUInt32Arg(pointer, 56, if (value.value) 1u else 0u).getOrThrow()
    }

  override var isTextSelectionEnabled: WinRtBoolean
    get() = WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 35).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithUInt32Arg(pointer, 36, if (value.value) 1u else 0u).getOrThrow()
    }

  override val isTextTrimmed: WinRtBoolean
    get() = WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 59).getOrThrow())

  override var lineHeight: Float64
    get() = Float64(PlatformComInterop.invokeFloat64Method(pointer, 31).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithFloat64Arg(pointer, 32, value.value).getOrThrow()
    }

  override var lineStackingStrategy: LineStackingStrategy
    get() = LineStackingStrategy.fromValue(PlatformComInterop.invokeInt32Method(pointer,
        33).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 34, value.value).getOrThrow()
    }

  override var maxLines: Int32
    get() = Int32(PlatformComInterop.invokeInt32Method(pointer, 45).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 46, value.value).getOrThrow()
    }

  override var opticalMarginAlignment: OpticalMarginAlignment
    get() = OpticalMarginAlignment.fromValue(PlatformComInterop.invokeInt32Method(pointer,
        49).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 50, value.value).getOrThrow()
    }

  override var padding: Thickness
    get() = Thickness.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 29,
        Thickness.ABI_LAYOUT).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 30, value.toAbi()).getOrThrow()
    }

  override val selectedText: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 37).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }

  override val selectionEnd: TextPointer
    get() = TextPointer(PlatformComInterop.invokeObjectMethod(pointer, 41).getOrThrow())

  override var selectionFlyout: FlyoutBase
    get() = FlyoutBase(PlatformComInterop.invokeObjectMethod(pointer, 63).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 64, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.Controls.Primitives.FlyoutBase",
          "rc(Microsoft.UI.Xaml.Controls.Primitives.FlyoutBase;{bb6603bf-744d-5c31-a87d-744394634d77})")).getOrThrow()
    }

  override var selectionHighlightColor: SolidColorBrush
    get() = SolidColorBrush(PlatformComInterop.invokeObjectMethod(pointer, 43).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 44, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.Media.SolidColorBrush",
          "rc(Microsoft.UI.Xaml.Media.SolidColorBrush;{b3865c31-37c8-55c1-8a72-d41c67642e2a})")).getOrThrow()
    }

  override val selectionStart: TextPointer
    get() = TextPointer(PlatformComInterop.invokeObjectMethod(pointer, 40).getOrThrow())

  override var text: String
    get() = run {
      val value = PlatformComInterop.invokeHStringMethod(pointer, 26).getOrThrow()
      try {
        value.toKotlinString()
      } finally {
        value.close()
      }
    }
    set(value) {
      PlatformComInterop.invokeUnitMethodWithStringArg(pointer, 27, value).getOrThrow()
    }

  override var textAlignment: TextAlignment
    get() = TextAlignment.fromValue(PlatformComInterop.invokeInt32Method(pointer, 24).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 25, value.value).getOrThrow()
    }

  override var textDecorations: TextDecorations
    get() = TextDecorations.fromValue(PlatformComInterop.invokeUInt32Method(pointer,
        57).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithUInt32Arg(pointer, 58, value.value).getOrThrow()
    }

  override val textHighlighters: IVector<TextHighlighter>
    get() = IVector.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        62).getOrThrow()),
        "rc(Microsoft.UI.Xaml.Documents.TextHighlighter;{b756e861-1d2b-5f6f-81fd-c51a5bc068ff})",
        "Microsoft.UI.Xaml.Documents.TextHighlighter")

  override var textLineBounds: TextLineBounds
    get() = TextLineBounds.fromValue(PlatformComInterop.invokeInt32Method(pointer, 47).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 48, value.value).getOrThrow()
    }

  override var textReadingOrder: TextReadingOrder
    get() = TextReadingOrder.fromValue(PlatformComInterop.invokeInt32Method(pointer,
        53).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 54, value.value).getOrThrow()
    }

  override var textTrimming: TextTrimming
    get() = TextTrimming.fromValue(PlatformComInterop.invokeInt32Method(pointer, 22).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 23, value.value).getOrThrow()
    }

  override var textWrapping: TextWrapping
    get() = TextWrapping.fromValue(PlatformComInterop.invokeInt32Method(pointer, 20).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 21, value.value).getOrThrow()
    }

  private val isTextTrimmedChangedEventSlot: IsTextTrimmedChangedEvent = IsTextTrimmedChangedEvent()

  public val isTextTrimmedChangedEvent: IsTextTrimmedChangedEvent
    get() = isTextTrimmedChangedEventSlot

  override fun add_SelectionChanged(handler: RoutedEventHandler): EventRegistrationToken =
      EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 65,
      EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
      "Microsoft.UI.Xaml.RoutedEventHandler",
      "delegate({dae23d85-69ca-5bdf-805b-6161a3a215cc})")).getOrThrow())

  override fun remove_SelectionChanged(token: EventRegistrationToken) {
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 66, token.toAbi()).getOrThrow()
  }

  override fun add_ContextMenuOpening(handler: ContextMenuOpeningEventHandler):
      EventRegistrationToken =
      EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 67,
      EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
      "Microsoft.UI.Xaml.Controls.ContextMenuOpeningEventHandler",
      "delegate({d010ff61-4067-526a-95a3-517577bc5273})")).getOrThrow())

  override fun remove_ContextMenuOpening(token: EventRegistrationToken) {
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 68, token.toAbi()).getOrThrow()
  }

  override
      fun add_IsTextTrimmedChanged(handler: TypedEventHandler<TextBlock, IsTextTrimmedChangedEventArgs>):
      EventRegistrationToken =
      EventRegistrationToken.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 69,
      EventRegistrationToken.ABI_LAYOUT, projectedObjectArgumentPointer(handler,
      "Windows.Foundation.TypedEventHandler`2<Microsoft.UI.Xaml.Controls.TextBlock, Microsoft.UI.Xaml.Controls.IsTextTrimmedChangedEventArgs>",
      "pinterface({9de1c534-6ae1-11e0-84e1-18a905bcc53f};rc(Microsoft.UI.Xaml.Controls.TextBlock;{1ac8d84f-392c-5c7e-83f5-a53e3bf0abb0});rc(Microsoft.UI.Xaml.Controls.IsTextTrimmedChangedEventArgs;{3c709b2f-16ba-55d2-b6f6-dfc54a1ed021}))")).getOrThrow())

  override fun remove_IsTextTrimmedChanged(token: EventRegistrationToken) {
    PlatformComInterop.invokeUnitMethodWithArgs(pointer, 70, token.toAbi()).getOrThrow()
  }

  override fun selectAll() {
    PlatformComInterop.invokeUnitMethod(pointer, 71).getOrThrow()
  }

  override fun select(start: TextPointer, end: TextPointer) {
    PlatformComInterop.invokeUnitMethodWithTwoObjectArgs(pointer, 72,
        projectedObjectArgumentPointer(start, "Microsoft.UI.Xaml.Documents.TextPointer",
        "rc(Microsoft.UI.Xaml.Documents.TextPointer;{842eb385-ee41-5930-979b-438fa7525a51})"),
        projectedObjectArgumentPointer(end, "Microsoft.UI.Xaml.Documents.TextPointer",
        "rc(Microsoft.UI.Xaml.Documents.TextPointer;{842eb385-ee41-5930-979b-438fa7525a51})")).getOrThrow()
  }

  override fun getAlphaMask(): CompositionBrush =
      CompositionBrush(PlatformComInterop.invokeObjectMethod(pointer, 73).getOrThrow())

  override fun copySelectionToClipboard() {
    PlatformComInterop.invokeUnitMethod(pointer, 74).getOrThrow()
  }

  public fun add_SelectionChanged(callback: (Inspectable, RoutedEventArgs) -> Unit):
      WinRtDelegateHandle {
    val delegateHandle = WinRtDelegateBridge.createUnitDelegate(RoutedEventHandler.iid,
        listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
        dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args ->
        callback(dev.winrt.core.Inspectable(args[0] as ComPtr),
        microsoft.ui.xaml.RoutedEventArgs(args[1] as ComPtr)) }
    try {
      add_SelectionChanged(RoutedEventHandler(delegateHandle.pointer))
    } catch (t: Throwable) {
      delegateHandle.close()
      throw t
    }
    return delegateHandle
  }

  public fun add_ContextMenuOpening(callback: (Inspectable, ContextMenuEventArgs) -> Unit):
      WinRtDelegateHandle {
    val delegateHandle = WinRtDelegateBridge.createUnitDelegate(ContextMenuOpeningEventHandler.iid,
        listOf(dev.winrt.core.WinRtDelegateValueKind.OBJECT,
        dev.winrt.core.WinRtDelegateValueKind.OBJECT)) { args ->
        callback(dev.winrt.core.Inspectable(args[0] as ComPtr),
        microsoft.ui.xaml.controls.ContextMenuEventArgs(args[1] as ComPtr)) }
    try {
      add_ContextMenuOpening(ContextMenuOpeningEventHandler(delegateHandle.pointer))
    } catch (t: Throwable) {
      delegateHandle.close()
      throw t
    }
    return delegateHandle
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
}
