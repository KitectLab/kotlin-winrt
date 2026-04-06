package windows.data.json

import dev.winrt.core.Float64
import dev.winrt.core.Inspectable
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String

public interface IJsonArray : IJsonValue {
  public fun getObjectAt(index: UInt32): JsonObject

  public fun getArrayAt(index: UInt32): JsonArray

  public fun getStringAt(index: UInt32): String

  public fun getNumberAt(index: UInt32): Float64

  public fun getBooleanAt(index: UInt32): WinRtBoolean

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Data.Json.IJsonArray"

    override val projectionTypeKey: String = "Windows.Data.Json.IJsonArray"

    override val iid: Guid = guidOf("08c1ddb6-0cbd-4a9a-b5d3-2f852dc37e81")

    public fun from(inspectable: Inspectable): IJsonArray = inspectable.projectInterface(this,
        ::IJsonArrayProjection)

    public operator fun invoke(inspectable: Inspectable): IJsonArray = from(inspectable)
  }
}

private class IJsonArrayProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IJsonArray {
  override val valueType: JsonValueType
    get() = JsonValueType.fromValue(PlatformComInterop.invokeInt32Method(pointer, 6).getOrThrow())

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

  override fun get_ValueType(): JsonValueType =
      JsonValueType.fromValue(PlatformComInterop.invokeInt32Method(pointer, 6).getOrThrow())

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

  override fun getNumber(): Float64 = Float64(PlatformComInterop.invokeFloat64Method(pointer,
      9).getOrThrow())

  override fun getBoolean(): WinRtBoolean =
      WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, 10).getOrThrow())

  override fun getObject(): JsonObject = JsonObject(PlatformComInterop.invokeObjectMethod(pointer,
      12).getOrThrow())

  override fun getArray(): JsonArray = JsonArray(PlatformComInterop.invokeObjectMethod(pointer,
      11).getOrThrow())
}
