package microsoft.ui.xaml.controls

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import microsoft.ui.xaml.ResourceDictionary

open class XamlControlsResources(
    pointer: ComPtr,
) : ResourceDictionary(pointer) {
    constructor() : this(Companion.activate().pointer)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.XamlControlsResources"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "XamlControlsResources")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IXamlControlsResources"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

        fun activate(): XamlControlsResources = WinRtRuntime.activate(this, ::XamlControlsResources)
    }
}
