package windows.data.json

import dev.winrt.core.Float64
import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComMethodResultKind
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.requireBoolean
import dev.winrt.kom.requireObject
import kotlin.String
import kotlin.collections.Iterator
import kotlin.collections.Map
import windows.foundation.IStringable

public open class JsonObject(
  pointer: ComPtr,
) : Inspectable(pointer),
    IJsonObject,
    IStringable {
  private val backing_Size: RuntimeProperty<UInt32> = RuntimeProperty<UInt32>(UInt32(0u))

  public val size: UInt32
    get() {
      if (pointer.isNull) {
        return backing_Size.get()
      }
      return UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())
    }

  private val backing_ValueType: RuntimeProperty<JsonValueType> =
      RuntimeProperty<JsonValueType>(JsonValueType.fromValue(0))

  override val valueType: JsonValueType
    get() {
      if (pointer.isNull) {
        return backing_ValueType.get()
      }
      return JsonValueType.fromValue(PlatformComInterop.invokeInt32Method(pointer, 6).getOrThrow())
    }

  public constructor() : this(Companion.activate().pointer)

  public fun lookup(key: String): IJsonValue {
    if (pointer.isNull) {
      error("Null runtime object pointer: Lookup")
    }
    return IJsonValue.from(Inspectable(PlatformComInterop.invokeObjectMethodWithStringArg(pointer,
        7, key).getOrThrow()))
  }

  public fun hasKey(key: String): WinRtBoolean {
    if (pointer.isNull) {
      return WinRtBoolean.FALSE
    }
    return WinRtBoolean(PlatformComInterop.invokeBooleanMethodWithStringArg(pointer, 9,
        key).getOrThrow())
  }

  public fun getView(): Map<String, IJsonValue> {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetView")
    }
    return Map<String, IJsonValue>.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        10).getOrThrow()))
  }

  public fun insert(key: String, value: IJsonValue): WinRtBoolean {
    if (pointer.isNull) {
      return WinRtBoolean.FALSE
    }
    return WinRtBoolean(PlatformComInterop.invokeMethodWithStringAndObjectArgs(pointer, 11,
        ComMethodResultKind.BOOLEAN, key, projectedObjectArgumentPointer(value,
        "Windows.Data.Json.IJsonValue",
        "{a3219ecb-f0b3-4dcd-beee-19d48cd3ed1e}")).getOrThrow().requireBoolean())
  }

  public fun remove(key: String) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithStringArg(pointer, 12, key).getOrThrow()
  }

  public fun clear() {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethod(pointer, 13).getOrThrow()
  }

  public fun first(): Iterator<Map.Entry<String, IJsonValue>> {
    if (pointer.isNull) {
      error("Null runtime object pointer: First")
    }
    return Iterator<Map.Entry<String, IJsonValue>>.from(Inspectable(PlatformComInterop.invokeObjectMethod(pointer,
        6).getOrThrow()))
  }

  public fun getNamedValue(name: String, defaultValue: JsonValue): JsonValue {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetNamedValue")
    }
    return JsonValue(PlatformComInterop.invokeMethodWithStringAndObjectArgs(pointer, 27,
        ComMethodResultKind.OBJECT, name, projectedObjectArgumentPointer(defaultValue,
        "Windows.Data.Json.JsonValue",
        "rc(Windows.Data.Json.JsonValue;{a3219ecb-f0b3-4dcd-beee-19d48cd3ed1e})")).getOrThrow().requireObject())
  }

  public fun getNamedObject(name: String, defaultValue: JsonObject): JsonObject {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetNamedObject")
    }
    return JsonObject(PlatformComInterop.invokeMethodWithStringAndObjectArgs(pointer, 28,
        ComMethodResultKind.OBJECT, name, projectedObjectArgumentPointer(defaultValue,
        "Windows.Data.Json.JsonObject",
        "rc(Windows.Data.Json.JsonObject;{064e24dd-29c2-4f83-9ac1-9ee11578beb3})")).getOrThrow().requireObject())
  }

  public fun getNamedArray(name: String, defaultValue: JsonArray): JsonArray {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetNamedArray")
    }
    return JsonArray(PlatformComInterop.invokeMethodWithStringAndObjectArgs(pointer, 30,
        ComMethodResultKind.OBJECT, name, projectedObjectArgumentPointer(defaultValue,
        "Windows.Data.Json.JsonArray",
        "rc(Windows.Data.Json.JsonArray;{08c1ddb6-0cbd-4a9a-b5d3-2f852dc37e81})")).getOrThrow().requireObject())
  }

  public fun getNamedBoolean(name: String, defaultValue: WinRtBoolean): WinRtBoolean {
    if (pointer.isNull) {
      return WinRtBoolean.FALSE
    }
    return WinRtBoolean(PlatformComInterop.invokeMethodWithStringAndBooleanArgs(pointer, 32,
        ComMethodResultKind.BOOLEAN, name, defaultValue.value).getOrThrow().requireBoolean())
  }

  public fun setNamedValue(name: String, value: IJsonValue) {
    if (pointer.isNull) {
      return
    }
    PlatformComInterop.invokeUnitMethodWithStringAndObjectArgs(pointer, 14, name,
        projectedObjectArgumentPointer(value, "Windows.Data.Json.IJsonValue",
        "{a3219ecb-f0b3-4dcd-beee-19d48cd3ed1e}")).getOrThrow()
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

  override fun getNamedValue(name: String): IJsonValue {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetNamedValue")
    }
    return IJsonValue.from(Inspectable(PlatformComInterop.invokeObjectMethodWithStringArg(pointer,
        6, name).getOrThrow()))
  }

  override fun getNamedString(name: String): String {
    if (pointer.isNull) {
      return ""
    }
    return run {
          val value = PlatformComInterop.invokeHStringMethodWithStringArg(pointer, 10,
              name).getOrThrow()
          try {
            value.toKotlinString()
          } finally {
            value.close()
          }
        }
  }

  override fun getNamedObject(name: String): JsonObject {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetNamedObject")
    }
    return JsonObject(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 8,
        name).getOrThrow())
  }

  override fun getNamedArray(name: String): JsonArray {
    if (pointer.isNull) {
      error("Null runtime object pointer: GetNamedArray")
    }
    return JsonArray(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 9,
        name).getOrThrow())
  }

  override fun getNamedNumber(name: String): Float64 {
    if (pointer.isNull) {
      return Float64(0.0)
    }
    return Float64(PlatformComInterop.invokeFloat64MethodWithStringArg(pointer, 11,
        name).getOrThrow())
  }

  override fun getNamedBoolean(name: String): WinRtBoolean {
    if (pointer.isNull) {
      return WinRtBoolean.FALSE
    }
    return WinRtBoolean(PlatformComInterop.invokeBooleanMethodWithStringArg(pointer, 12,
        name).getOrThrow())
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
    override val qualifiedName: String = "Windows.Data.Json.JsonObject"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Data.Json", "JsonObject")

    override val defaultInterfaceName: String? = "Windows.Data.Json.IJsonObject"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    private val statics: IJsonObjectStatics by lazy { WinRtRuntime.projectActivationFactory(this,
        IJsonObjectStatics, ::IJsonObjectStatics) }

    public fun activate(): JsonObject = WinRtRuntime.activate(this, ::JsonObject)

    public fun parse(input: String): JsonObject = statics.parse(input)
  }
}
