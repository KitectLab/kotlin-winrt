package windows.`data`.json

import dev.winrt.core.Float64
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.WinRtObject
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.String

public open class JsonArray(
  pointer: ComPtr,
) : WinRtObject(pointer),
  IJsonArray {
  override val valueType: JsonValueType
    get() = get_ValueType()

  override fun get_ValueType(): JsonValueType =
      JsonValueType.fromValue(PlatformComInterop.invokeUInt32Method(pointer, 6).getOrThrow().toInt())

  override fun stringify(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 7).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun getString(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 8).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun getNumber(): Float64 =
      Float64(PlatformComInterop.invokeFloat64Method(pointer, 9).getOrThrow())

  override fun getBoolean(): WinRtBoolean =
      WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 10).getOrThrow())

  override fun getArray(): JsonArray = JsonArray(PlatformComInterop.invokeObjectMethod(pointer, 11)
      .getOrThrow())

  override fun getObject(): JsonObject = JsonObject(PlatformComInterop.invokeObjectMethod(pointer,
      12).getOrThrow())

  override fun getObjectAt(index: UInt32): JsonObject =
      JsonObject(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 6,
      index.value).getOrThrow())

  override fun getArrayAt(index: UInt32): JsonArray =
      JsonArray(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 7,
      index.value).getOrThrow())

  override fun getStringAt(index: UInt32): String {
    val value = PlatformComInterop.invokeHStringMethodWithUInt32Arg(pointer, 8,
        index.value).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun getNumberAt(index: UInt32): Float64 =
      Float64(PlatformComInterop.invokeFloat64MethodWithUInt32Arg(pointer, 9,
      index.value).getOrThrow())

  override fun getBooleanAt(index: UInt32): WinRtBoolean =
      WinRtBoolean(PlatformComInterop.invokeBooleanMethodWithUInt32Arg(pointer, 10,
      index.value).getOrThrow())

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.Data.Json.JsonArray"

    override val classId = dev.winrt.core.RuntimeClassId("Windows.Data.Json", "JsonArray")

    override val defaultInterfaceName: String? = "Windows.Data.Json.IJsonArray"

    override val activationKind = dev.winrt.core.WinRtActivationKind.Factory

    public fun activate(): JsonArray = WinRtRuntime.activate(this, ::JsonArray)
  }
}
