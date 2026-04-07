package microsoft.ui.xaml.controls

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import microsoft.ui.xaml.DependencyProperty
import microsoft.ui.xaml.ResourceDictionary
import microsoft.ui.xaml.UIElement
import microsoft.ui.xaml.media.Brush

public open class XamlControlsResources(
  pointer: ComPtr,
) : ResourceDictionary(pointer),
    IXamlControlsResources {
  private val backing_UseCompactResources: RuntimeProperty<WinRtBoolean> =
      RuntimeProperty<WinRtBoolean>(WinRtBoolean.FALSE)

  override var useCompactResources: WinRtBoolean
    get() {
      if (pointer.isNull) {
        return backing_UseCompactResources.get()
      }
      return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 6).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_UseCompactResources.set(value)
        return
      }
      PlatformComInterop.invokeUnitMethodWithUInt32Arg(pointer, 7, if (value.value) 1u else 0u).getOrThrow()
    }

  private val backing_UseCompactResourcesProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val useCompactResourcesProperty: DependencyProperty
    get() = backing_UseCompactResourcesProperty.get()

  public constructor() : this(Companion.activate().pointer)

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.XamlControlsResources"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls",
        "XamlControlsResources")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IXamlControlsResources"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    private val statics: IXamlControlsResourcesStatics by lazy {
        WinRtRuntime.projectActivationFactory(this, IXamlControlsResourcesStatics,
        ::IXamlControlsResourcesStatics) }

    public val useCompactResourcesProperty: DependencyProperty
      get() = statics.useCompactResourcesProperty

    public var foreground: Brush
      get() = statics.foreground
      set(value) {
        statics.foreground = value
      }

    public val foregroundProperty: DependencyProperty
      get() = statics.foregroundProperty

    public var foreground: Brush
      get() = statics.foreground
      set(value) {
        statics.foreground = value
      }

    public val foregroundProperty: DependencyProperty
      get() = statics.foregroundProperty

    public fun activate(): XamlControlsResources = WinRtRuntime.activate(this,
        ::XamlControlsResources)

    public fun ensureRevealLights(element: UIElement) {
      statics.ensureRevealLights(element)
    }
  }
}
