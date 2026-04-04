package microsoft.ui.xaml.controls

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr
import microsoft.ui.xaml.ResourceDictionary

open class XamlControlsResources(
    pointer: ComPtr,
) : ResourceDictionary(pointer) {
    constructor() : this(Companion.factoryCreateInstance().pointer)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.XamlControlsResources"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "XamlControlsResources")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IXamlControlsResources"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

        private fun factoryCreateInstance(): XamlControlsResources =
            WinRtRuntime.compose(
                ResourceDictionary.Companion,
                guidOf("ea22a48f-ab71-56f6-a392-d82310c8aa7b"),
                null,
                ::XamlControlsResources,
                6,
                ComPtr.NULL,
            )
    }
}
