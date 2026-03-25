package windows.data.json

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr

open class JsonObject(pointer: ComPtr) : Inspectable(pointer) {
    fun asIJsonObject(): IJsonObject = IJsonObject.from(this)

    fun asIJsonValue(): IJsonValue = IJsonValue.from(this)

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Windows.Data.Json.JsonObject"
        override val classId = RuntimeClassId("Windows.Data.Json", "JsonObject")
        override val defaultInterfaceName: String? = "Windows.Data.Json.IJsonObject"
        override val activationKind = WinRtActivationKind.Factory

        fun activate(): JsonObject = WinRtRuntime.activate(this, ::JsonObject)
    }
}
