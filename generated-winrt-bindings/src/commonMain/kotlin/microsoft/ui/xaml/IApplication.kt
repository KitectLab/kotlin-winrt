package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

interface IApplication {
    var resources: ResourceDictionary

    fun get_Resources(): ResourceDictionary

    fun put_Resources(value: ResourceDictionary)

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.IApplication"
        override val projectionTypeKey: String = "Microsoft.UI.Xaml.IApplication"
        override val iid: Guid = guidOf("06a8f4e7-1146-55af-820d-ebd55643b021")

        fun from(inspectable: Inspectable): IApplication =
            inspectable.projectInterface(this, ::IApplicationProjection)

        operator fun invoke(inspectable: Inspectable): IApplication = from(inspectable)
    }
}

private class IApplicationProjection(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IApplication {
    override var resources: ResourceDictionary
        get() = ResourceDictionary(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())
        set(value) {
            PlatformComInterop.invokeObjectSetter(pointer, 7, value.pointer).getOrThrow()
        }

    override fun get_Resources(): ResourceDictionary =
        ResourceDictionary(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

    override fun put_Resources(value: ResourceDictionary) {
        PlatformComInterop.invokeObjectSetter(pointer, 7, value.pointer).getOrThrow()
    }
}
