package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.String
import windows.ui.xaml.interop.TypeName

public open class DependencyProperty(
  pointer: ComPtr,
) : Inspectable(pointer) {
  private val backing_UnsetValue: RuntimeProperty<Inspectable> =
      RuntimeProperty<Inspectable>(Inspectable(ComPtr.NULL))

  public val unsetValue: Inspectable
    get() = backing_UnsetValue.get()

  public fun getMetadata(forType: TypeName): PropertyMetadata {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetMetadata")
    }
    return PropertyMetadata(PlatformComInterop.invokeObjectMethodWithArgs(pointer, 6,
        forType.toAbi()).getOrThrow())
  }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.DependencyProperty"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml", "DependencyProperty")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.IDependencyProperty"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    private val statics: IDependencyPropertyStatics by lazy {
        WinRtRuntime.projectActivationFactory(this, IDependencyPropertyStatics,
        ::IDependencyPropertyStatics) }

    public val unsetValue: Inspectable
      get() = statics.unsetValue

    public fun register(
      name: String,
      propertyType: TypeName,
      ownerType: TypeName,
      typeMetadata: PropertyMetadata,
    ): DependencyProperty = statics.register(name, propertyType, ownerType, typeMetadata)

    public fun registerAttached(
      name: String,
      propertyType: TypeName,
      ownerType: TypeName,
      defaultMetadata: PropertyMetadata,
    ): DependencyProperty = statics.registerAttached(name, propertyType, ownerType, defaultMetadata)
  }
}
