package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

interface IDependencyPropertyChangedEventArgs {
    val newValue: Inspectable
    val oldValue: Inspectable
    val property: DependencyProperty

    fun get_Property(): DependencyProperty
    fun get_OldValue(): Inspectable
    fun get_NewValue(): Inspectable

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String =
            "Microsoft.UI.Xaml.IDependencyPropertyChangedEventArgs"

        override val projectionTypeKey: String =
            "Microsoft.UI.Xaml.IDependencyPropertyChangedEventArgs"

        override val iid: Guid = guidOf("84ead020-7849-5e98-8030-488a80d164ec")

        fun from(inspectable: Inspectable): IDependencyPropertyChangedEventArgs =
            inspectable.projectInterface(this, ::IDependencyPropertyChangedEventArgsProjection)

        operator fun invoke(inspectable: Inspectable): IDependencyPropertyChangedEventArgs =
            from(inspectable)
    }
}

private class IDependencyPropertyChangedEventArgsProjection(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer), IDependencyPropertyChangedEventArgs {
    override val newValue: Inspectable
        get() = Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())

    override val oldValue: Inspectable
        get() = Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 7).getOrThrow())

    override val property: DependencyProperty
        get() = DependencyProperty(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

    override fun get_Property(): DependencyProperty =
        DependencyProperty(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

    override fun get_OldValue(): Inspectable =
        Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 7).getOrThrow())

    override fun get_NewValue(): Inspectable =
        Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 8).getOrThrow())
}
