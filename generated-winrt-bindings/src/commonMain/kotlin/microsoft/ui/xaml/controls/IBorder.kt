package microsoft.ui.xaml.controls

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import microsoft.ui.xaml.BrushTransition
import microsoft.ui.xaml.CornerRadius
import microsoft.ui.xaml.Thickness
import microsoft.ui.xaml.UIElement
import microsoft.ui.xaml.media.Brush
import microsoft.ui.xaml.media.animation.TransitionCollection

public interface IBorder {
  public var background: Brush

  public var backgroundSizing: BackgroundSizing

  public var backgroundTransition: BrushTransition

  public var borderBrush: Brush

  public var borderThickness: Thickness

  public var child: UIElement

  public var childTransitions: TransitionCollection

  public var cornerRadius: CornerRadius

  public var padding: Thickness

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.IBorder"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.Controls.IBorder"

    override val iid: Guid = guidOf("1ca13b47-ff5c-5abc-a411-a177df9482a9")

    public fun from(inspectable: Inspectable): IBorder = inspectable.projectInterface(this,
        ::IBorderProjection)

    public operator fun invoke(inspectable: Inspectable): IBorder = from(inspectable)
  }
}

private class IBorderProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IBorder {
  override var background: Brush
    get() = Brush(PlatformComInterop.invokeObjectMethod(pointer, 10).getOrThrow())
    set(value) {
      PlatformComInterop.invokeObjectSetter(pointer, 11, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.Media.Brush",
          "rc(Microsoft.UI.Xaml.Media.Brush;{2de3cb83-1329-5679-88f8-c822bc5442cb})")).getOrThrow()
    }

  override var backgroundSizing: BackgroundSizing
    get() = BackgroundSizing.fromValue(PlatformComInterop.invokeInt32Method(pointer,
        12).getOrThrow())
    set(value) {
      PlatformComInterop.invokeInt32Setter(pointer, 13, value.value).getOrThrow()
    }

  override var backgroundTransition: BrushTransition
    get() = BrushTransition(PlatformComInterop.invokeObjectMethod(pointer, 22).getOrThrow())
    set(value) {
      PlatformComInterop.invokeObjectSetter(pointer, 23, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.BrushTransition",
          "rc(Microsoft.UI.Xaml.BrushTransition;{a996a7ba-4567-5963-a112-76e3c0000204})")).getOrThrow()
    }

  override var borderBrush: Brush
    get() = Brush(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    set(value) {
      PlatformComInterop.invokeObjectSetter(pointer, 7, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.Media.Brush",
          "rc(Microsoft.UI.Xaml.Media.Brush;{2de3cb83-1329-5679-88f8-c822bc5442cb})")).getOrThrow()
    }

  override var borderThickness: Thickness
    get() = Thickness.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 8,
        Thickness.ABI_LAYOUT).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 9, value.toAbi()).getOrThrow()
    }

  override var child: UIElement
    get() = UIElement(PlatformComInterop.invokeObjectMethod(pointer, 18).getOrThrow())
    set(value) {
      PlatformComInterop.invokeObjectSetter(pointer, 19, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.UIElement",
          "rc(Microsoft.UI.Xaml.UIElement;{c3c01020-320c-5cf6-9d24-d396bbfa4d8b})")).getOrThrow()
    }

  override var childTransitions: TransitionCollection
    get() = TransitionCollection(PlatformComInterop.invokeObjectMethod(pointer, 20).getOrThrow())
    set(value) {
      PlatformComInterop.invokeObjectSetter(pointer, 21, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.Media.Animation.TransitionCollection",
          "rc(Microsoft.UI.Xaml.Media.Animation.TransitionCollection;pinterface({913337e9-11a1-4345-a3a2-4e7f956e222d};rc(Microsoft.UI.Xaml.Media.Animation.Transition;{e5b71956-8e44-5a38-b41e-274d706102bf})))")).getOrThrow()
    }

  override var cornerRadius: CornerRadius
    get() = CornerRadius.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 14,
        CornerRadius.ABI_LAYOUT).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 15, value.toAbi()).getOrThrow()
    }

  override var padding: Thickness
    get() = Thickness.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 16,
        Thickness.ABI_LAYOUT).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 17, value.toAbi()).getOrThrow()
    }
}
