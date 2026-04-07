package microsoft.ui.xaml.controls

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import dev.winrt.kom.ComStructValue
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import microsoft.ui.xaml.BrushTransition
import microsoft.ui.xaml.CornerRadius
import microsoft.ui.xaml.DependencyProperty
import microsoft.ui.xaml.FrameworkElement
import microsoft.ui.xaml.Thickness
import microsoft.ui.xaml.UIElement
import microsoft.ui.xaml.media.Brush
import microsoft.ui.xaml.media.animation.TransitionCollection

public open class Border(
  pointer: ComPtr,
) : FrameworkElement(pointer),
    IBorder {
  private val backing_Background: RuntimeProperty<Brush> =
      RuntimeProperty<Brush>(Brush(ComPtr.NULL))

  override var background: Brush
    get() {
      if (pointer.isNull) {
        return backing_Background.get()
      }
      return Brush(PlatformComInterop.invokeObjectMethod(pointer, 10).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Background.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 11, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_BackgroundSizing: RuntimeProperty<BackgroundSizing> =
      RuntimeProperty<BackgroundSizing>(BackgroundSizing.fromValue(0))

  override var backgroundSizing: BackgroundSizing
    get() {
      if (pointer.isNull) {
        return backing_BackgroundSizing.get()
      }
      return BackgroundSizing.fromValue(PlatformComInterop.invokeInt32Method(pointer,
          12).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_BackgroundSizing.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithInt32Arg(pointer, 13, value.value).getOrThrow()
    }

  private val backing_BackgroundTransition: RuntimeProperty<BrushTransition> =
      RuntimeProperty<BrushTransition>(BrushTransition(ComPtr.NULL))

  override var backgroundTransition: BrushTransition
    get() {
      if (pointer.isNull) {
        return backing_BackgroundTransition.get()
      }
      return BrushTransition(PlatformComInterop.invokeObjectMethod(pointer, 22).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_BackgroundTransition.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 23, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_BorderBrush: RuntimeProperty<Brush> =
      RuntimeProperty<Brush>(Brush(ComPtr.NULL))

  override var borderBrush: Brush
    get() {
      if (pointer.isNull) {
        return backing_BorderBrush.get()
      }
      return Brush(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_BorderBrush.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 7, (value as
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
      return Thickness.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 8,
          Thickness.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_BorderThickness.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 9, value.toAbi()).getOrThrow()
    }

  private val backing_Child: RuntimeProperty<UIElement> =
      RuntimeProperty<UIElement>(UIElement(ComPtr.NULL))

  override var child: UIElement
    get() {
      if (pointer.isNull) {
        return backing_Child.get()
      }
      return UIElement(PlatformComInterop.invokeObjectMethod(pointer, 18).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Child.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 19, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_ChildTransitions: RuntimeProperty<TransitionCollection> =
      RuntimeProperty<TransitionCollection>(TransitionCollection(ComPtr.NULL))

  override var childTransitions: TransitionCollection
    get() {
      if (pointer.isNull) {
        return backing_ChildTransitions.get()
      }
      return TransitionCollection(PlatformComInterop.invokeObjectMethod(pointer, 20).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_ChildTransitions.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithObjectArg(pointer, 21, (value as
          Inspectable).pointer).getOrThrow()
    }

  private val backing_CornerRadius: RuntimeProperty<CornerRadius> =
      RuntimeProperty<CornerRadius>(CornerRadius.fromAbi(ComStructValue(CornerRadius.ABI_LAYOUT,
      ByteArray(CornerRadius.ABI_LAYOUT.byteSize))))

  override var cornerRadius: CornerRadius
    get() {
      if (pointer.isNull) {
        return backing_CornerRadius.get()
      }
      return CornerRadius.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 14,
          CornerRadius.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_CornerRadius.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 15, value.toAbi()).getOrThrow()
    }

  private val backing_Padding: RuntimeProperty<Thickness> =
      RuntimeProperty<Thickness>(Thickness.fromAbi(ComStructValue(Thickness.ABI_LAYOUT,
      ByteArray(Thickness.ABI_LAYOUT.byteSize))))

  override var padding: Thickness
    get() {
      if (pointer.isNull) {
        return backing_Padding.get()
      }
      return Thickness.fromAbi(PlatformComInterop.invokeStructMethodWithArgs(pointer, 16,
          Thickness.ABI_LAYOUT).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Padding.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithArgs(pointer, 17, value.toAbi()).getOrThrow()
    }

  private val backing_BackgroundProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val backgroundProperty: DependencyProperty
    get() = backing_BackgroundProperty.get()

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

  private val backing_ChildTransitionsProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val childTransitionsProperty: DependencyProperty
    get() = backing_ChildTransitionsProperty.get()

  private val backing_CornerRadiusProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val cornerRadiusProperty: DependencyProperty
    get() = backing_CornerRadiusProperty.get()

  private val backing_PaddingProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val paddingProperty: DependencyProperty
    get() = backing_PaddingProperty.get()

  public constructor() : this(Companion.activate().pointer)

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.Border"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "Border")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IBorder"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    private val statics: IBorderStatics by lazy { WinRtRuntime.projectActivationFactory(this,
        IBorderStatics, ::IBorderStatics) }

    public val backgroundProperty: DependencyProperty
      get() = statics.backgroundProperty

    public val backgroundSizingProperty: DependencyProperty
      get() = statics.backgroundSizingProperty

    public val borderBrushProperty: DependencyProperty
      get() = statics.borderBrushProperty

    public val borderThicknessProperty: DependencyProperty
      get() = statics.borderThicknessProperty

    public val childTransitionsProperty: DependencyProperty
      get() = statics.childTransitionsProperty

    public val cornerRadiusProperty: DependencyProperty
      get() = statics.cornerRadiusProperty

    public val paddingProperty: DependencyProperty
      get() = statics.paddingProperty

    public fun activate(): Border = WinRtRuntime.activate(this, ::Border)
  }
}
