package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop

open class PropertyMetadata(
    pointer: ComPtr,
) : Inspectable(pointer) {
    private val backingDefaultValue = RuntimeProperty<Inspectable>(Inspectable(ComPtr.NULL))

    val defaultValue: Inspectable
        get() = if (pointer.isNull) backingDefaultValue.get() else Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

    fun get_DefaultValue(): Inspectable {
        if (pointer.isNull) error("Null runtime object pointer: get_DefaultValue")
        return Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    }

    fun get_CreateDefaultValueCallback(): CreateDefaultValueCallback {
        if (pointer.isNull) error("Null runtime object pointer: get_CreateDefaultValueCallback")
        return CreateDefaultValueCallback(PlatformComInterop.invokeObjectMethod(pointer, 7).getOrThrow())
    }

    constructor(defaultValue: Inspectable) : this(Companion.create(defaultValue).pointer)

    constructor(defaultValue: Inspectable, propertyChangedCallback: PropertyChangedCallback) :
        this(Companion.create(defaultValue, propertyChangedCallback).pointer)

    constructor(createDefaultValueCallback: CreateDefaultValueCallback) :
        this(Companion.create(createDefaultValueCallback).pointer)

    constructor(createDefaultValueCallback: CreateDefaultValueCallback, propertyChangedCallback: PropertyChangedCallback) :
        this(Companion.create(createDefaultValueCallback, propertyChangedCallback).pointer)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.PropertyMetadata"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml", "PropertyMetadata")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.IPropertyMetadata"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

        private val factory: IPropertyMetadataFactory by lazy {
            WinRtRuntime.projectActivationFactory(this, IPropertyMetadataFactory, ::IPropertyMetadataFactory)
        }
        private val statics: IPropertyMetadataStatics by lazy {
            WinRtRuntime.projectActivationFactory(this, IPropertyMetadataStatics, ::IPropertyMetadataStatics)
        }

        fun activate(): PropertyMetadata = WinRtRuntime.activate(this, ::PropertyMetadata)

        fun create(defaultValue: Inspectable): PropertyMetadata = statics.create(defaultValue)
        fun create(defaultValue: Inspectable, propertyChangedCallback: PropertyChangedCallback): PropertyMetadata =
            statics.create(defaultValue, propertyChangedCallback)
        fun create(createDefaultValueCallback: CreateDefaultValueCallback): PropertyMetadata =
            statics.create(createDefaultValueCallback)
        fun create(createDefaultValueCallback: CreateDefaultValueCallback, propertyChangedCallback: PropertyChangedCallback): PropertyMetadata =
            statics.create(createDefaultValueCallback, propertyChangedCallback)

        private fun factoryCreateInstanceWithDefaultValue(defaultValue: Inspectable, baseInterface: Inspectable, innerInterface: Inspectable): PropertyMetadata =
            factory.createInstanceWithDefaultValue(defaultValue, baseInterface, innerInterface)

        private fun factoryCreateInstanceWithDefaultValueAndCallback(
            defaultValue: Inspectable,
            propertyChangedCallback: PropertyChangedCallback,
            baseInterface: Inspectable,
            innerInterface: Inspectable,
        ): PropertyMetadata = factory.createInstanceWithDefaultValueAndCallback(defaultValue, propertyChangedCallback, baseInterface, innerInterface)
    }
}
