package windows.`data`.json

import dev.winrt.core.Float64
import dev.winrt.core.Inspectable
import dev.winrt.core.RuntimeClassId
import dev.winrt.core.RuntimeProperty
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.String

public open class JsonObject(
  pointer: ComPtr,
) : Inspectable(pointer),
    IJsonObject {
  private val backing_Size: RuntimeProperty<UInt32> = RuntimeProperty<UInt32>(UInt32(0u))

  public val size: UInt32
    get() {
      if (pointer.isNull) {
        return backing_Size.get()
      }
      return UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())
    }

  private val backing_ValueType: RuntimeProperty<JsonValueType> =
      RuntimeProperty<JsonValueType>(error("Stub method not implemented: windows.`data`.json.JsonValueType"))

  override val valueType: JsonValueType
    get() {
      if (pointer.isNull) {
        return backing_ValueType.get()
      }
      return JsonValueType.fromValue(PlatformComInterop.invokeUInt32Method(pointer,
          6).getOrThrow().toInt())
    }

  public constructor() : this(Companion.activate().pointer)

  public fun lookup(key: String): IJsonValue {
    if (pointer.isNull) {
      error("Null runtime object pointer: Lookup")
    }
    return IJsonValue.from(Inspectable(PlatformComInterop.invokeObjectMethodWithStringArg(pointer,
        7, key).getOrThrow()))
  }

  public fun get_Size(): UInt32 {
    if (pointer.isNull) {
      return UInt32(0u)
    }
    return UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())
  }

  public fun hasKey(key: String): WinRtBoolean {
    if (pointer.isNull) {
      return WinRtBoolean.FALSE
    }
    return WinRtBoolean(PlatformComInterop.invokeBooleanMethodWithStringArg(pointer, 9,
        key).getOrThrow())
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

  override fun toString(): String {
    if (pointer.isNull) {
      return ""
    }
    return PlatformComInterop.invokeHStringMethod(pointer, 6).getOrThrow().use { it.toKotlinString()
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
    return PlatformComInterop.invokeHStringMethodWithStringArg(pointer, 10, name).getOrThrow().use {
        it.toKotlinString() }
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
    return JsonValueType.fromValue(PlatformComInterop.invokeUInt32Method(pointer,
        6).getOrThrow().toInt())
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
