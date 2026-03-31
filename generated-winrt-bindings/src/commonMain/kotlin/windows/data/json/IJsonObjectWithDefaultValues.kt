package windows.`data`.json

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

public interface IJsonObjectWithDefaultValues : IJsonObject {
  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Data.Json.IJsonObjectWithDefaultValues"

    override val projectionTypeKey: String = "Windows.Data.Json.IJsonObjectWithDefaultValues"

    override val iid: Guid = guidOf("d960d2a2-b7f0-4f00-8e44-d82cf415ea13")

    public fun from(inspectable: Inspectable): IJsonObjectWithDefaultValues =
        inspectable.projectInterface(this, ::IJsonObjectWithDefaultValuesProjection)

    public operator fun invoke(inspectable: Inspectable): IJsonObjectWithDefaultValues =
        from(inspectable)
  }
}

private class IJsonObjectWithDefaultValuesProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IJsonObjectWithDefaultValues {
  override val valueType: JsonValueType
    get() = JsonValueType.fromValue(PlatformComInterop.invokeUInt32Method(pointer,
        6).getOrThrow().toInt())

  override fun get_ValueType(): JsonValueType =
      JsonValueType.fromValue(PlatformComInterop.invokeUInt32Method(pointer,
      6).getOrThrow().toInt())

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
}
