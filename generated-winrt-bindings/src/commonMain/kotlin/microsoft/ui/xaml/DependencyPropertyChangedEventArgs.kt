package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop

open class DependencyPropertyChangedEventArgs(
    pointer: ComPtr,
) : Inspectable(pointer) {
    private val backingNewValue = RuntimeProperty<Inspectable>(Inspectable(ComPtr.NULL))
    private val backingOldValue = RuntimeProperty<Inspectable>(Inspectable(ComPtr.NULL))

    val newValue: Inspectable
        get() {
            if (pointer.isNull) return backingNewValue.get()
            return Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())
        }

    val oldValue: Inspectable
        get() {
            if (pointer.isNull) return backingOldValue.get()
            return Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 7).getOrThrow())
        }

    fun get_Property(): DependencyProperty {
        if (pointer.isNull) error("Null runtime object pointer: get_Property")
        return DependencyProperty(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
    }

    fun get_OldValue(): Inspectable {
        if (pointer.isNull) error("Null runtime object pointer: get_OldValue")
        return Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 7).getOrThrow())
    }

    fun get_NewValue(): Inspectable {
        if (pointer.isNull) error("Null runtime object pointer: get_NewValue")
        return Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())
    }

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.DependencyPropertyChangedEventArgs"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml", "DependencyPropertyChangedEventArgs")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.IDependencyPropertyChangedEventArgs"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

        fun activate(): DependencyPropertyChangedEventArgs = WinRtRuntime.activate(this, ::DependencyPropertyChangedEventArgs)
    }
}
