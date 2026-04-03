package microsoft.ui.xaml.media

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr

open class MicaBackdrop(
    pointer: ComPtr,
) : SystemBackdrop(pointer) {
    constructor() : this(Companion.activate().pointer)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Media.MicaBackdrop"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Media", "MicaBackdrop")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Media.IMicaBackdrop"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

        fun activate(): MicaBackdrop = WinRtRuntime.activate(this, ::MicaBackdrop)
    }
}
