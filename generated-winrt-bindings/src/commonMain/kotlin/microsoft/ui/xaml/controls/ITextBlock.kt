package microsoft.ui.xaml.controls

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

open class ITextBlock(pointer: ComPtr) : WinRtInterfaceProjection(pointer) {
    var text: String
        get() {
            val value = PlatformComInterop.invokeHStringMethod(pointer, 26).getOrThrow()
            return try {
                value.toKotlinString()
            } finally {
                value.close()
            }
        }
        set(value) {
            PlatformComInterop.invokeStringSetter(pointer, 27, value).getOrThrow()
        }

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.ITextBlock"
        override val iid: Guid = guidOf("1ac8d84f-392c-5c7e-83f5-a53e3bf0abb0")

        fun from(inspectable: Inspectable): ITextBlock = inspectable.projectInterface(this, ::ITextBlock)
    }
}
