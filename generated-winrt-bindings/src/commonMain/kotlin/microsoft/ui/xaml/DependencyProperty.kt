package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import windows.ui.xaml.interop.TypeName

open class DependencyProperty(
    pointer: ComPtr,
) : Inspectable(pointer) {
    val unsetValue: Inspectable
        get() = if (pointer.isNull) Inspectable(ComPtr.NULL) else Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 7).getOrThrow())

    fun getMetadata(forType: TypeName): PropertyMetadata {
        if (pointer.isNull) error("Null runtime object pointer: GetMetadata")
        return PropertyMetadata(PlatformComInterop.invokeObjectMethodWithObjectArg(pointer, 6, forType.pointer).getOrThrow())
    }

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.DependencyProperty"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml", "DependencyProperty")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.IDependencyProperty"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

        private val statics: IDependencyPropertyStatics by lazy {
            WinRtRuntime.projectActivationFactory(this, IDependencyPropertyStatics, ::IDependencyPropertyStatics)
        }

        val unsetValue: Inspectable
            get() = statics.unsetValue

        fun activate(): DependencyProperty = WinRtRuntime.activate(this, ::DependencyProperty)

        fun register(
            name: String,
            propertyType: TypeName,
            ownerType: TypeName,
            typeMetadata: PropertyMetadata,
        ): DependencyProperty = statics.register(name, propertyType, ownerType, typeMetadata)

        fun registerAttached(
            name: String,
            propertyType: TypeName,
            ownerType: TypeName,
            defaultMetadata: PropertyMetadata,
        ): DependencyProperty = statics.registerAttached(name, propertyType, ownerType, defaultMetadata)
    }
}
