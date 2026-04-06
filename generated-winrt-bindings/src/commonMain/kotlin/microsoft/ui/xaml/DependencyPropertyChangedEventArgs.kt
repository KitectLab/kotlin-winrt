package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.String

public open class DependencyPropertyChangedEventArgs(
  pointer: ComPtr,
) : Inspectable(pointer) {
  private val backing_NewValue: RuntimeProperty<Inspectable> =
      RuntimeProperty<Inspectable>(Inspectable(ComPtr.NULL))

  public val newValue: Inspectable
    get() {
      if (pointer.isNull) {
        return backing_NewValue.get()
      }
      return Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())
    }

  private val backing_OldValue: RuntimeProperty<Inspectable> =
      RuntimeProperty<Inspectable>(Inspectable(ComPtr.NULL))

  public val oldValue: Inspectable
    get() {
      if (pointer.isNull) {
        return backing_OldValue.get()
      }
      return Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 7).getOrThrow())
    }

  private val backing_Property: RuntimeProperty<DependencyProperty> =
      RuntimeProperty<DependencyProperty>(DependencyProperty(ComPtr.NULL))

  public val property: DependencyProperty
    get() {
      if (pointer.isNull) {
        return backing_Property.get()
      }
      return DependencyProperty(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.DependencyPropertyChangedEventArgs"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml",
        "DependencyPropertyChangedEventArgs")

    override val defaultInterfaceName: String? =
        "Microsoft.UI.Xaml.IDependencyPropertyChangedEventArgs"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory
  }
}
