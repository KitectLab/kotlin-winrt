package microsoft.ui.xaml.controls

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import microsoft.ui.xaml.FrameworkElement

open class TextBlock(pointer: ComPtr) : FrameworkElement(pointer) {
    constructor() : this(Companion.activate().pointer)

    private val backingText = RuntimeProperty("")

    var text: String
        get() {
            if (pointer.isNull) return backingText.get()
            val value = PlatformComInterop.invokeHStringMethod(pointer, 26).getOrThrow()
            return try {
                value.toKotlinString()
            } finally {
                value.close()
            }
        }
        set(value) {
            if (pointer.isNull) {
                backingText.set(value)
                return
            }
            PlatformComInterop.invokeStringSetter(pointer, 27, value).getOrThrow()
        }

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.TextBlock"
        override val classId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "TextBlock")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.ITextBlock"
        override val activationKind = WinRtActivationKind.Factory

        fun activate(): TextBlock = WinRtRuntime.activate(this, ::TextBlock)
    }
}
