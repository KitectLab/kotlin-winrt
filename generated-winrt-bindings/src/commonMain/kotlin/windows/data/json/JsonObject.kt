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
) : Inspectable(pointer) {
  private val jsonValue by lazy { IJsonValue.from(this) }
  private val jsonObject by lazy { IJsonObject.from(this) }

  public val valueType: JsonValueType
    get() = jsonValue.valueType

  public fun get_ValueType(): JsonValueType = jsonValue.get_ValueType()

  public fun stringify(): String = jsonValue.stringify()

  public fun getString(): String = jsonValue.getString()

  public fun getNumber(): dev.winrt.core.Float64 = jsonValue.getNumber()

  public fun getBoolean(): dev.winrt.core.WinRtBoolean = jsonValue.getBoolean()

  public fun getArray(): JsonArray = jsonValue.getArray()

  public fun getObject(): JsonObject = jsonValue.getObject()

  public fun getNamedValue(name: String): IJsonValue = jsonObject.getNamedValue(name)

  public fun getNamedObject(name: String): JsonObject = jsonObject.getNamedObject(name)

  public fun getNamedArray(name: String): JsonArray = jsonObject.getNamedArray(name)

  public fun getNamedString(name: String): String = jsonObject.getNamedString(name)

  public fun getNamedNumber(name: String): dev.winrt.core.Float64 = jsonObject.getNamedNumber(name)

  public fun getNamedBoolean(name: String): dev.winrt.core.WinRtBoolean = jsonObject.getNamedBoolean(name)

  override fun toString(): String = stringify()

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.Data.Json.JsonObject"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Data.Json", "JsonObject")

    override val defaultInterfaceName: String? = "Windows.Data.Json.IJsonObject"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    public fun activate(): JsonObject = WinRtRuntime.activate(this, ::JsonObject)
  }
}
