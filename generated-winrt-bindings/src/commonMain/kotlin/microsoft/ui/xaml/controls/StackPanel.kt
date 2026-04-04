package microsoft.ui.xaml.controls

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr

open class StackPanel(
    pointer: ComPtr,
) : Panel(pointer) {
    constructor() : this(Companion.factoryCreateInstance().pointer)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.StackPanel"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "StackPanel")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IStackPanel"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

        private fun factoryCreateInstance(): StackPanel =
            WinRtRuntime.compose(
                this,
                guidOf("64c1d388-47a2-5a74-a75b-559d151ee5ac"),
                guidOf("493ab00b-3a6a-5e4a-9452-407cd5197406"),
                ::StackPanel,
                6,
                ComPtr.NULL,
            )
    }
}
