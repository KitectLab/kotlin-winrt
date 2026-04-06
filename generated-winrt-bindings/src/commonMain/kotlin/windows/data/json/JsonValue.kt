package windows.data.json

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
import windows.foundation.IStringable

public open class JsonValue(
  pointer: ComPtr,
) : Inspectable(pointer),
    IJsonValue,
    IStringable {
  private val backing_ValueType: RuntimeProperty<JsonValueType> =
      RuntimeProperty<JsonValueType>(JsonValueType.fromValue(0))

  override val valueType: JsonValueType
    get() {
      if (pointer.isNull) {
        return backing_ValueType.get()
      }
      return JsonValueType.fromValue(PlatformComInterop.invokeInt32Method(pointer, 6).getOrThrow())
    }

  override fun toString(): String {
    if (pointer.isNull) {
      return ""
    }
    return run {
          val value = PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow()
          try {
            value.toKotlinString()
          } finally {
            value.close()
          }
        }
  }

  override fun get_ValueType(): JsonValueType {
    if (pointer.isNull) {
      error("Null runtime object pointer: Get_ValueType")
    }
    return JsonValueType.fromValue(PlatformComInterop.invokeInt32Method(pointer, 6).getOrThrow())
  }

  override fun stringify(): String {
    if (pointer.isNull) {
      return ""
    }
    return run {
          val value = PlatformComInterop.invokeHStringMethod(pointer, 7).getOrThrow()
          try {
            value.toKotlinString()
          } finally {
            value.close()
          }
        }
  }

  override fun getString(): String {
    if (pointer.isNull) {
      return ""
    }
    return run {
          val value = PlatformComInterop.invokeHStringMethod(pointer, 8).getOrThrow()
          try {
            value.toKotlinString()
          } finally {
            value.close()
          }
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

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.Data.Json.JsonValue"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Data.Json", "JsonValue")

    override val defaultInterfaceName: String? = "Windows.Data.Json.IJsonValue"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    private val statics2: IJsonValueStatics2 by lazy { WinRtRuntime.projectActivationFactory(this,
        IJsonValueStatics2, ::IJsonValueStatics2) }

    private val statics: IJsonValueStatics by lazy { WinRtRuntime.projectActivationFactory(this,
        IJsonValueStatics, ::IJsonValueStatics) }

    public fun createNullValue(): JsonValue = statics2.createNullValue()

    public fun parse(input: String): JsonValue = statics.parse(input)

    public fun createBooleanValue(input: WinRtBoolean): JsonValue =
        statics.createBooleanValue(input)

    public fun createStringValue(input: String): JsonValue = statics.createStringValue(input)
  }
}
