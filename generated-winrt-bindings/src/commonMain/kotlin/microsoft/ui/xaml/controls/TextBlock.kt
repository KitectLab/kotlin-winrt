package microsoft.ui.xaml.controls

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr

open class TextBlock(pointer: ComPtr) : Inspectable(pointer) {
    constructor() : this(Companion.activate().pointer)

    private val backingText = RuntimeProperty("")

    var text: String
        get() {
            if (pointer.isNull) return backingText.get()
            return asITextBlock().text
        }
        set(value) {
            if (pointer.isNull) {
                backingText.set(value)
                return
            }
            asITextBlock().text = value
        }

    fun asITextBlock(): ITextBlock = ITextBlock.from(this)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.TextBlock"
        override val classId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "TextBlock")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.ITextBlock"
        override val activationKind = WinRtActivationKind.Factory

        fun activate(): TextBlock = WinRtRuntime.activate(this, ::TextBlock)
    }
}
