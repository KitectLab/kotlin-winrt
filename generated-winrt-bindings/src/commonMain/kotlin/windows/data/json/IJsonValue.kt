package windows.data.json

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr

open class IJsonValue(pointer: ComPtr) : WinRtInterfaceProjection(pointer) {
    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Windows.Data.Json.IJsonValue"
        override val iid = guidOf("a3219a91-eccd-42e5-b553-261d0aefde37")

        fun from(inspectable: Inspectable): IJsonValue = inspectable.projectInterface(this, ::IJsonValue)
    }
}
