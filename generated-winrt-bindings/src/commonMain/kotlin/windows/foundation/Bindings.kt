package windows.foundation

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr

open class IStringable(pointer: ComPtr) : WinRtInterfaceProjection(pointer) {
    fun toStringValue(): String
        = ""

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Windows.Foundation.IStringable"
        override val iid = guidOf("96369f54-8eb6-48f0-abce-c1b211e627c3")

        fun from(Inspectable: Inspectable): IStringable = Inspectable.projectInterface(this, ::IStringable)
    }
}
