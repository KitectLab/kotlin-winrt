package windows.`data`.json

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import kotlin.String

public open class JsonArray(
  pointer: ComPtr,
) : IJsonArray(pointer) {

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.Data.Json.JsonArray"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Data.Json", "JsonArray")

    override val defaultInterfaceName: String? = "Windows.Data.Json.IJsonArray"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    public fun activate(): JsonArray = WinRtRuntime.activate(this, ::JsonArray)
  }
}
