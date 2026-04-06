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
import microsoft.ui.xaml.DataTemplate
import microsoft.ui.xaml.UIElement
import microsoft.ui.xaml.media.animation.TransitionCollection

public interface IContentControl {
  public var content: Inspectable

  public var contentTemplate: DataTemplate

  public val contentTemplateRoot: UIElement

  public var contentTemplateSelector: DataTemplateSelector

  public var contentTransitions: TransitionCollection

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.IContentControl"

    override val projectionTypeKey: String = "Microsoft.UI.Xaml.Controls.IContentControl"

    override val iid: Guid = guidOf("07e81761-11b2-52ae-8f8b-4d53d2b5900a")

    public fun from(inspectable: Inspectable): IContentControl = inspectable.projectInterface(this,
        ::IContentControlProjection)

    public operator fun invoke(inspectable: Inspectable): IContentControl = from(inspectable)
  }
}

private class IContentControlProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IContentControl {
  override var content: Inspectable
    get() = Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    set(value) {
      PlatformComInterop.invokeObjectSetter(pointer, 7, projectedObjectArgumentPointer(value,
          "Object", "cinterface(IInspectable)")).getOrThrow()
    }

  override var contentTemplate: DataTemplate
    get() = DataTemplate(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())
    set(value) {
      PlatformComInterop.invokeObjectSetter(pointer, 9, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.DataTemplate",
          "rc(Microsoft.UI.Xaml.DataTemplate;{08fa70fa-ee75-5e92-a101-f52d0e1e9fab})")).getOrThrow()
    }

  override val contentTemplateRoot: UIElement
    get() = UIElement(PlatformComInterop.invokeObjectMethod(pointer, 14).getOrThrow())

  override var contentTemplateSelector: DataTemplateSelector
    get() = DataTemplateSelector(PlatformComInterop.invokeObjectMethod(pointer, 10).getOrThrow())
    set(value) {
      PlatformComInterop.invokeObjectSetter(pointer, 11, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.Controls.DataTemplateSelector",
          "rc(Microsoft.UI.Xaml.Controls.DataTemplateSelector;{86ca4fa4-7de0-5049-82f5-39ec78569028})")).getOrThrow()
    }

  override var contentTransitions: TransitionCollection
    get() = TransitionCollection(PlatformComInterop.invokeObjectMethod(pointer, 12).getOrThrow())
    set(value) {
      PlatformComInterop.invokeObjectSetter(pointer, 13, projectedObjectArgumentPointer(value,
          "Microsoft.UI.Xaml.Media.Animation.TransitionCollection",
          "rc(Microsoft.UI.Xaml.Media.Animation.TransitionCollection;pinterface({913337e9-11a1-4345-a3a2-4e7f956e222d};rc(Microsoft.UI.Xaml.Media.Animation.Transition;{e5b71956-8e44-5a38-b41e-274d706102bf})))")).getOrThrow()
    }
}
