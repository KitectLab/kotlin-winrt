package windows.`data`.json

import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import kotlin.String

public open class JsonObject(
  pointer: ComPtr,
) : Inspectable(pointer),
  IJsonValue by IJsonValue.from(Inspectable(pointer)),
  IJsonObject by IJsonObject.from(Inspectable(pointer)) {

  override fun toString(): String = stringify()

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.Data.Json.JsonObject"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Data.Json", "JsonObject")

    override val defaultInterfaceName: String? = "Windows.Data.Json.IJsonObject"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    public fun activate(): JsonObject = WinRtRuntime.activate(this, ::JsonObject)
  }
}
