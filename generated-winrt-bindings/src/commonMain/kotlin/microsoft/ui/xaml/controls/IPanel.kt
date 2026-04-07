package microsoft.ui.xaml.controls

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtBoolean
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
import microsoft.ui.xaml.media.Brush
import microsoft.ui.xaml.media.animation.TransitionCollection

public interface IPanel {
  public var background: Brush

  public var backgroundTransition: BrushTransition

  public val children: UIElementCollection

  public var childrenTransitions: TransitionCollection

  public val isItemsHost: WinRtBoolean

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.IPanel"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.Controls.IPanel"

    override val iid: Guid = guidOf("27a1b418-56f3-525e-b883-cefed905eed3")

    public fun from(inspectable: Inspectable): IPanel = inspectable.projectInterface(this,
        ::IPanelProjection)

    public operator fun invoke(inspectable: Inspectable): IPanel = from(inspectable)
  }
}

private class IPanelProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IPanel {
  override var background: Brush
    get() = Brush(PlatformComInterop.invokeObjectMethod(pointer, 7).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 8, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.Media.Brush",
          "rc(Microsoft.UI.Xaml.Media.Brush;{2de3cb83-1329-5679-88f8-c822bc5442cb})")).getOrThrow()
    }

  override var backgroundTransition: BrushTransition
    get() = BrushTransition(PlatformComInterop.invokeObjectMethod(pointer, 12).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 13, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.BrushTransition",
          "rc(Microsoft.UI.Xaml.BrushTransition;{a996a7ba-4567-5963-a112-76e3c0000204})")).getOrThrow()
    }

  override val children: UIElementCollection
    get() = UIElementCollection(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

  override var childrenTransitions: TransitionCollection
    get() = TransitionCollection(PlatformComInterop.invokeObjectMethod(pointer, 10).getOrThrow())
    set(value) {
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 11, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.Media.Animation.TransitionCollection",
          "rc(Microsoft.UI.Xaml.Media.Animation.TransitionCollection;pinterface({913337e9-11a1-4345-a3a2-4e7f956e222d};rc(Microsoft.UI.Xaml.Media.Animation.Transition;{e5b71956-8e44-5a38-b41e-274d706102bf})))")).getOrThrow()
    }

  override val isItemsHost: WinRtBoolean
    get() = WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 9).getOrThrow())
}
