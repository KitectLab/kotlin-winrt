package windows.foundation

import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.guidOf

interface IStringable {
    fun toStringValue(): String

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Windows.Foundation.IStringable"
        override val iid = guidOf("96369f54-8eb6-48f0-abce-c1b211e627c3")
    }
}
