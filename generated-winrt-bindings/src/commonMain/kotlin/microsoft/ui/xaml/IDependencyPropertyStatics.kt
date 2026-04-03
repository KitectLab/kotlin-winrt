package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.requireObject

internal open class IDependencyPropertyStatics(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
    val unsetValue: Inspectable
        get() = Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

    fun get_UnsetValue(): Inspectable =
        Inspectable(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

    fun register(
        name: String,
        propertyType: windows.ui.xaml.interop.TypeName,
        ownerType: windows.ui.xaml.interop.TypeName,
        typeMetadata: PropertyMetadata,
    ): DependencyProperty = DependencyProperty(
        PlatformComInterop.invokeMethodWithTwoObjectArgs(
            pointer,
            7,
            dev.winrt.kom.ComMethodResultKind.OBJECT,
            propertyType.pointer,
            ownerType.pointer,
        ).getOrThrow().requireObject(),
    )

    fun registerAttached(
        name: String,
        propertyType: windows.ui.xaml.interop.TypeName,
        ownerType: windows.ui.xaml.interop.TypeName,
        defaultMetadata: PropertyMetadata,
    ): DependencyProperty = DependencyProperty(
        PlatformComInterop.invokeMethodWithTwoObjectArgs(
            pointer,
            8,
            dev.winrt.kom.ComMethodResultKind.OBJECT,
            propertyType.pointer,
            ownerType.pointer,
        ).getOrThrow().requireObject(),
    )

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.IDependencyPropertyStatics"
        override val projectionTypeKey: String = "Microsoft.UI.Xaml.IDependencyPropertyStatics"
        override val iid: Guid = guidOf("61ddc651-0383-5d6f-98ce-5c046aaaaa8f")

        fun from(inspectable: Inspectable): IDependencyPropertyStatics =
            inspectable.projectInterface(this, ::IDependencyPropertyStatics)

        operator fun invoke(inspectable: Inspectable): IDependencyPropertyStatics = from(inspectable)
    }
}
