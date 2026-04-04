package microsoft.ui.xaml.controls

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr

open class Button(
    pointer: ComPtr,
) : ContentControl(pointer) {
    constructor() : this(Companion.activate().pointer)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.Button"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "Button")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IButton"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Composable

        fun activate(): Button = WinRtRuntime.activate(this, ::Button)

        private fun factoryCreateInstance(): Button =
            WinRtRuntime.compose(
                this,
                guidOf("fe393422-d91c-57b1-9a9c-2c7e3f41f77c"),
                guidOf("216c183d-d07a-5aa5-b8a4-0300a2683e87"),
                ::Button,
                6,
                ComPtr.NULL,
            )
    }
}
