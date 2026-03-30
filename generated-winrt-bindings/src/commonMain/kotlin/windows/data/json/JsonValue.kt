package windows.`data`.json

import dev.winrt.core.Float64
import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.String

public open class JsonValue(
  pointer: ComPtr,
) : Inspectable(pointer),
    IJsonValue {
  private val backing_ValueType: RuntimeProperty<JsonValueType> =
      RuntimeProperty<JsonValueType>(JsonValueType.fromValue(0))

  override val valueType: JsonValueType
    get() {
      if (pointer.isNull) {
        return backing_ValueType.get()
      }
      return JsonValueType.fromValue(PlatformComInterop.invokeUInt32Method(pointer,
          6).getOrThrow().toInt())
    }

  public constructor() : this(Companion.activate().pointer)

  override fun toString(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow().use { it.toKotlinString()
        }
  }

  override fun stringify(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 7).getOrThrow().use { it.toKotlinString()
        }
  }

  override fun getString(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 8).getOrThrow().use { it.toKotlinString()
        }
  }

  override fun getNumber(): Float64 {
    if (pointer.isNull) {
      return Float64(0.0)
    }
    return Float64(PlatformComInterop.invokeFloat64Method(pointer, 9).getOrThrow())
  }

  override fun getBoolean(): WinRtBoolean {
    if (pointer.isNull) {
      return WinRtBoolean.FALSE
    }
    return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 10).getOrThrow())
  }

  override fun getObject(): JsonObject {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetObject")
    }
    return JsonObject(PlatformComInterop.invokeObjectMethod(pointer, 12).getOrThrow())
  }

  override fun getArray(): JsonArray {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetArray")
    }
    return JsonArray(PlatformComInterop.invokeObjectMethod(pointer, 11).getOrThrow())
  }

  override fun get_ValueType(): JsonValueType {
    if (pointer.isNull) {
      error("Null runtime object pointer: get_ValueType")
    }
    return JsonValueType.fromValue(PlatformComInterop.invokeUInt32Method(pointer,
        6).getOrThrow().toInt())
  }

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.Data.Json.JsonValue"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Data.Json", "JsonValue")

    override val defaultInterfaceName: String? = "Windows.Data.Json.IJsonValue"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    private val statics: IJsonValueStatics by lazy { WinRtRuntime.projectActivationFactory(this,
        IJsonValueStatics, ::IJsonValueStatics) }

    public fun activate(): JsonValue = WinRtRuntime.activate(this, ::JsonValue)

    public fun parse(input: String): JsonValue = statics.parse(input)

    public fun createBooleanValue(input: WinRtBoolean): JsonValue =
        statics.createBooleanValue(input)

    public fun createStringValue(input: String): JsonValue = statics.createStringValue(input)
  }
}
