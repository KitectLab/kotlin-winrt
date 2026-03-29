package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

open class IWindow(pointer: ComPtr) : WinRtInterfaceProjection(pointer) {
    val visible: WinRtBoolean
        get() = WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 7).getOrThrow())

    var title: String
        get() {
            val value = PlatformComInterop.invokeHStringMethod(pointer, 14).getOrThrow()
            return try {
                value.toKotlinString()
            } finally {
                value.close()
            }
        }
        set(value) {
            PlatformComInterop.invokeStringSetter(pointer, 15, value).getOrThrow()
        }

    fun setContent(content: Inspectable) {
        PlatformComInterop.invokeObjectSetter(pointer, 9, content.pointer).getOrThrow()
    }

    fun activate() {
        PlatformComInterop.invokeUnitMethod(pointer, 26).getOrThrow()
    }

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.IWindow"
        override val iid: Guid = guidOf("61f0ec79-5d52-56b5-86fb-40fa4af288b0")

        fun from(inspectable: Inspectable): IWindow = inspectable.projectInterface(this, ::IWindow)
    }
}
