package windows.data.json

import dev.winrt.core.Float64
import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String

public interface IJsonObject : IJsonValue {
  public fun getNamedValue(name: String): IJsonValue

  public fun getNamedString(name: String): String

  public fun getNamedObject(name: String): JsonObject

  public fun getNamedArray(name: String): JsonArray

  public fun getNamedNumber(name: String): Float64

  public fun getNamedBoolean(name: String): WinRtBoolean

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Data.Json.IJsonObject"

    override val projectionTypeKey: String = "Windows.Data.Json.IJsonObject"

    override val iid: Guid = guidOf("064e24dd-29c2-4f83-9ac1-9ee11578beb3")

    public fun from(inspectable: Inspectable): IJsonObject = inspectable.projectInterface(this,
        ::IJsonObjectProjection)

    public operator fun invoke(inspectable: Inspectable): IJsonObject = from(inspectable)
  }
}

private class IJsonObjectProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IJsonObject {
  override val valueType: JsonValueType
    get() = JsonValueType.fromValue(PlatformComInterop.invokeInt32Method(pointer, 6).getOrThrow())

  override fun getNamedValue(name: String): IJsonValue =
      IJsonValue.from(Inspectable(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 6,
      name).getOrThrow()))

  override fun getNamedString(name: String): String {
    val value = PlatformComInterop.invokeHStringMethodWithStringArg(pointer, 10, name).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun getNamedObject(name: String): JsonObject =
      JsonObject(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 8, name).getOrThrow())

  override fun getNamedArray(name: String): JsonArray =
      JsonArray(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 9, name).getOrThrow())

  override fun getNamedNumber(name: String): Float64 =
      Float64(PlatformComInterop.invokeFloat64MethodWithStringArg(pointer, 11, name).getOrThrow())

  override fun getNamedBoolean(name: String): WinRtBoolean =
      WinRtBoolean(PlatformComInterop.invokeBooleanMethodWithStringArg(pointer, 12,
      name).getOrThrow())

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
