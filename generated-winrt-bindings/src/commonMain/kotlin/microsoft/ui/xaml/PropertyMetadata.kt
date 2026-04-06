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

public open class PropertyMetadata(
  pointer: ComPtr,
) : Inspectable(pointer),
    IPropertyMetadata {
  private val backing_CreateDefaultValueCallback: RuntimeProperty<CreateDefaultValueCallback> =
      RuntimeProperty<CreateDefaultValueCallback>(CreateDefaultValueCallback(ComPtr.NULL))

  override val createDefaultValueCallback: CreateDefaultValueCallback
    get() {
      if (pointer.isNull) {
        return backing_CreateDefaultValueCallback.get()
      }
      return CreateDefaultValueCallback(PlatformComInterop.invokeObjectMethod(pointer,
          7).getOrThrow())
    }

  private val backing_DefaultValue: RuntimeProperty<Inspectable> =
      RuntimeProperty<Inspectable>(Inspectable(ComPtr.NULL))

  override val defaultValue: Inspectable
    get() {
      if (pointer.isNull) {
        return backing_DefaultValue.get()
      }
      return Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    }

  public constructor(defaultValue: Inspectable) :
      this(Companion.factoryCreateInstanceWithDefaultValue(defaultValue).pointer)

  public constructor(defaultValue: Inspectable, propertyChangedCallback: PropertyChangedCallback) :
      this(Companion.factoryCreateInstanceWithDefaultValueAndCallback(defaultValue,
      propertyChangedCallback).pointer)

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Microsoft.UI.Xaml.PropertyMetadata"

    override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml", "PropertyMetadata")

    override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.IPropertyMetadata"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

    private val statics: IPropertyMetadataStatics by lazy {
        WinRtRuntime.projectActivationFactory(this, IPropertyMetadataStatics,
        ::IPropertyMetadataStatics) }

    public fun create(defaultValue: Inspectable): PropertyMetadata = statics.create(defaultValue)

    public fun create(defaultValue: Inspectable, propertyChangedCallback: PropertyChangedCallback):
        PropertyMetadata = statics.create(defaultValue, propertyChangedCallback)

    public fun create(createDefaultValueCallback: CreateDefaultValueCallback): PropertyMetadata =
        statics.create(createDefaultValueCallback)

    public fun create(createDefaultValueCallback: CreateDefaultValueCallback,
        propertyChangedCallback: PropertyChangedCallback): PropertyMetadata =
        statics.create(createDefaultValueCallback, propertyChangedCallback)
  }
}
