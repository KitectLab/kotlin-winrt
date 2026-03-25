package windows.foundation

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtStrings
import dev.winrt.core.Float64
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop

open class IStringable(pointer: ComPtr) : WinRtInterfaceProjection(pointer) {
    fun toStringValue(): String {
        val value = PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow()
        return try {
            WinRtStrings.toKotlin(value)
        } finally {
            WinRtStrings.release(value)
        }
    }

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Windows.Foundation.IStringable"
        override val iid = guidOf("96369f54-8eb6-48f0-abce-c1b211e627c3")

        fun from(Inspectable: Inspectable): IStringable = Inspectable.projectInterface(this, ::IStringable)
    }
}

data class Point(
    val x: Float64,
    val y: Float64,
)

enum class AsyncStatus(val value: Int) {
    Started(0),
    Completed(1),
    Canceled(2),
    Error(3)
}
