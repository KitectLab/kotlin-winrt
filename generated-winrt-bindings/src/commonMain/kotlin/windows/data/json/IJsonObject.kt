package windows.data.json

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.WinRtStrings
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop

open class IJsonObject(pointer: ComPtr) : WinRtInterfaceProjection(pointer) {
    fun getNamedString(name: String): String {
        val value = PlatformComInterop.invokeHStringMethodWithStringArg(pointer, 10, name).getOrThrow()
        return try {
            WinRtStrings.toKotlin(value)
        } finally {
            WinRtStrings.release(value)
        }
    }

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Windows.Data.Json.IJsonObject"
        override val iid = guidOf("064e24dd-29c2-4f83-9ac1-9ee11578beb3")

        fun from(inspectable: Inspectable): IJsonObject = inspectable.projectInterface(this, ::IJsonObject)
    }
}
