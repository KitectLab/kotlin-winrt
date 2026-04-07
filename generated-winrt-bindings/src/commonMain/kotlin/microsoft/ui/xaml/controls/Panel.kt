package microsoft.ui.xaml.controls

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import microsoft.ui.xaml.BrushTransition
import microsoft.ui.xaml.DependencyProperty
import microsoft.ui.xaml.FrameworkElement
import microsoft.ui.xaml.media.Brush
import microsoft.ui.xaml.media.animation.TransitionCollection

public open class Panel(
  pointer: ComPtr,
) : FrameworkElement(pointer),
    IPanel {
  private val backing_Background: RuntimeProperty<Brush> =
      RuntimeProperty<Brush>(Brush(ComPtr.NULL))

  override var background: Brush
    get() {
      if (pointer.isNull) {
        return backing_Background.get()
      }
      return Brush(PlatformComInterop.invokeObjectMethod(pointer, 7).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Background.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 8, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_BackgroundTransition: RuntimeProperty<BrushTransition> =
      RuntimeProperty<BrushTransition>(BrushTransition(ComPtr.NULL))

  override var backgroundTransition: BrushTransition
    get() {
      if (pointer.isNull) {
        return backing_BackgroundTransition.get()
      }
      return BrushTransition(PlatformComInterop.invokeObjectMethod(pointer, 12).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_BackgroundTransition.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 13, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_Children: RuntimeProperty<UIElementCollection> =
      RuntimeProperty<UIElementCollection>(UIElementCollection(ComPtr.NULL))

  override val children: UIElementCollection
    get() {
      if (pointer.isNull) {
        return backing_Children.get()
      }
      return UIElementCollection(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    }

  private val backing_ChildrenTransitions: RuntimeProperty<TransitionCollection> =
      RuntimeProperty<TransitionCollection>(TransitionCollection(ComPtr.NULL))

  override var childrenTransitions: TransitionCollection
    get() {
      if (pointer.isNull) {
        return backing_ChildrenTransitions.get()
      }
      return TransitionCollection(PlatformComInterop.invokeObjectMethod(pointer, 10).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_ChildrenTransitions.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 11, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_IsItemsHost: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override val isItemsHost: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_IsItemsHost.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 9).getOrThrow())
    }

  public constructor() : this(Companion.factoryCreateInstance().pointer)

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.Panel"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "Panel")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IPanel"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

    private val statics: IPanelStatics by lazy { WinRtRuntime.projectActivationFactory(this,
        IPanelStatics, ::IPanelStatics) }

    public val backgroundProperty: DependencyProperty
      get() = statics.backgroundProperty

    public val childrenTransitionsProperty: DependencyProperty
      get() = statics.childrenTransitionsProperty

    public val isItemsHostProperty: DependencyProperty
      get() = statics.isItemsHostProperty

    private fun factoryCreateInstance(): Panel {
      return WinRtRuntime.compose(this, guidOf("f5e7e21c-4c97-5d20-bee6-3e4fc6ab14e9"),
          guidOf("27a1b418-56f3-525e-b883-cefed905eed3"), ::Panel, 6, ComPtr.NULL)
    }
  }
}
