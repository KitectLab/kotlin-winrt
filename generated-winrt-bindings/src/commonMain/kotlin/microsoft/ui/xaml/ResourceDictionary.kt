package microsoft.ui.xaml

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr

open class ResourceDictionary(
    pointer: ComPtr,
) : DependencyObject(pointer) {
    constructor() : this(Companion.activate().pointer)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.ResourceDictionary"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml", "ResourceDictionary")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.IResourceDictionary"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

        fun activate(): ResourceDictionary = WinRtRuntime.activate(this, ::ResourceDictionary)
    }
}
