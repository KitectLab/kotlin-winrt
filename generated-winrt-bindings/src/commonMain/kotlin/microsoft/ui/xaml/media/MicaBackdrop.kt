package microsoft.ui.xaml.media

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import microsoft.ui.composition.systembackdrops.MicaKind
import microsoft.ui.xaml.DependencyProperty

public open class MicaBackdrop(
  pointer: ComPtr,
) : SystemBackdrop(pointer) {
  private val backing_Kind: RuntimeProperty<MicaKind> =
      RuntimeProperty<MicaKind>(MicaKind.fromValue(0))

  public var kind: MicaKind
    get() {
      if (pointer.isNull) {
        return backing_Kind.get()
      }
      return MicaKind.fromValue(PlatformComInterop.invokeInt32Method(pointer, 6).getOrThrow())
    }
    set(value) {
      if (pointer.isNull) {
        backing_Kind.set(value)
        return
      }
      PlatformComInterop.invokeInt32Setter(pointer, 7, value.value).getOrThrow()
    }

  private val backing_KindProperty: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val kindProperty: DependencyProperty
    get() = backing_KindProperty.get()

  public constructor() : this(Companion.factoryCreateInstance().pointer)

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.Media.MicaBackdrop"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Media", "MicaBackdrop")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Media.IMicaBackdrop"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

    private val statics: IMicaBackdropStatics by lazy { WinRtRuntime.projectActivationFactory(this,
        IMicaBackdropStatics, ::IMicaBackdropStatics) }

    public val kindProperty: DependencyProperty
      get() = statics.kindProperty

    private fun factoryCreateInstance(): MicaBackdrop {
      return WinRtRuntime.compose(this, guidOf("774379ce-74bd-59d4-849d-d99c4184d838"),
          guidOf("c156a404-3dac-593a-b1f3-7a33c289dc83"), ::MicaBackdrop, 6, ComPtr.NULL)
    }
  }
}
