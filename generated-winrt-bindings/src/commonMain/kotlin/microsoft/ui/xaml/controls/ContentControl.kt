package microsoft.ui.xaml.controls

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.guidOf
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import microsoft.ui.xaml.DataTemplate
import microsoft.ui.xaml.DependencyProperty
import microsoft.ui.xaml.UIElement
import microsoft.ui.xaml.media.animation.TransitionCollection

public open class ContentControl(
  pointer: ComPtr,
) : Control(pointer),
    IContentControl {
  private val backing_Content: RuntimeProperty<Inspectable> =
      RuntimeProperty<Inspectable>(Inspectable(ComPtr.NULL))

  override var content: Inspectable
    get() {
      if (pointer.isNull) {
        return backing_Content.get()
      }
      return Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Content.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 7, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_ContentTemplate: RuntimeProperty<DataTemplate> =
      RuntimeProperty<DataTemplate>(DataTemplate(ComPtr.NULL))

  override var contentTemplate: DataTemplate
    get() {
      if (pointer.isNull) {
        return backing_ContentTemplate.get()
      }
      return DataTemplate(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_ContentTemplate.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 9, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_ContentTemplateRoot: RuntimeProperty<UIElement> =
      RuntimeProperty<UIElement>(UIElement(ComPtr.NULL))

  override val contentTemplateRoot: UIElement
    get() {
      if (pointer.isNull) {
        return backing_ContentTemplateRoot.get()
      }
      return UIElement(PlatformComInterop.invokeObjectMethod(pointer, 14).getOrThrow())
    }

  private val backing_ContentTemplateSelector: RuntimeProperty<DataTemplateSelector> =
      RuntimeProperty<DataTemplateSelector>(DataTemplateSelector(ComPtr.NULL))

  override var contentTemplateSelector: DataTemplateSelector
    get() {
      if (pointer.isNull) {
        return backing_ContentTemplateSelector.get()
      }
      return DataTemplateSelector(PlatformComInterop.invokeObjectMethod(pointer, 10).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_ContentTemplateSelector.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 11, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_ContentTransitions: RuntimeProperty<TransitionCollection> =
      RuntimeProperty<TransitionCollection>(TransitionCollection(ComPtr.NULL))

  override var contentTransitions: TransitionCollection
    get() {
      if (pointer.isNull) {
        return backing_ContentTransitions.get()
      }
      return TransitionCollection(PlatformComInterop.invokeObjectMethod(pointer, 12).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_ContentTransitions.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 13, (value as
          Inspectable).pointer).getOrThrow()
    }

  public constructor() : this(Companion.factoryCreateInstance().pointer)

  public fun onContentChanged(oldContent: Inspectable, newContent: Inspectable) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithTwoObjectArgs(pointer, 6,
        projectedObjectArgumentPointer(oldContent, "Object", "cinterface(IInspectable)"),
        projectedObjectArgumentPointer(newContent, "Object",
        "cinterface(IInspectable)")).getOrThrow()
  }

  public fun onContentTemplateChanged(oldContentTemplate: DataTemplate,
      newContentTemplate: DataTemplate) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithTwoObjectArgs(pointer, 7,
        projectedObjectArgumentPointer(oldContentTemplate, "Microsoft.UI.Xaml.DataTemplate",
        "rc(Microsoft.UI.Xaml.DataTemplate;{08fa70fa-ee75-5e92-a101-f52d0e1e9fab})"),
        projectedObjectArgumentPointer(newContentTemplate, "Microsoft.UI.Xaml.DataTemplate",
        "rc(Microsoft.UI.Xaml.DataTemplate;{08fa70fa-ee75-5e92-a101-f52d0e1e9fab})")).getOrThrow()
  }

  public fun onContentTemplateSelectorChanged(oldContentTemplateSelector: DataTemplateSelector,
      newContentTemplateSelector: DataTemplateSelector) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithTwoObjectArgs(pointer, 8,
        projectedObjectArgumentPointer(oldContentTemplateSelector,
        "Microsoft.UI.Xaml.Controls.DataTemplateSelector",
        "rc(Microsoft.UI.Xaml.Controls.DataTemplateSelector;{86ca4fa4-7de0-5049-82f5-39ec78569028})"),
        projectedObjectArgumentPointer(newContentTemplateSelector,
        "Microsoft.UI.Xaml.Controls.DataTemplateSelector",
        "rc(Microsoft.UI.Xaml.Controls.DataTemplateSelector;{86ca4fa4-7de0-5049-82f5-39ec78569028})")).getOrThrow()
  }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.ContentControl"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls",
        "ContentControl")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IContentControl"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

    private val statics: IContentControlStatics by lazy {
        WinRtRuntime.projectActivationFactory(this, IContentControlStatics,
        ::IContentControlStatics) }

    public val contentProperty: DependencyProperty
      get() = statics.contentProperty

    public val contentTemplateProperty: DependencyProperty
      get() = statics.contentTemplateProperty

    public val contentTemplateSelectorProperty: DependencyProperty
      get() = statics.contentTemplateSelectorProperty

    public val contentTransitionsProperty: DependencyProperty
      get() = statics.contentTransitionsProperty

    private fun factoryCreateInstance(): ContentControl {
      return WinRtRuntime.compose(this, guidOf("3dea958e-5acd-5f80-8938-38634f51493a"),
          guidOf("07e81761-11b2-52ae-8f8b-4d53d2b5900a"), ::ContentControl, 6, ComPtr.NULL)
    }
  }
}
