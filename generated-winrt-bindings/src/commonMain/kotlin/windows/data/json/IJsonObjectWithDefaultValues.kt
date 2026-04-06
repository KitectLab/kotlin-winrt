package windows.data.json

import dev.winrt.core.Float64
import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.core.projectedObjectArgumentPointer
import dev.winrt.kom.ComMethodResultKind
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import dev.winrt.kom.requireBoolean
import dev.winrt.kom.requireObject
import kotlin.String

public interface IJsonObjectWithDefaultValues : IJsonObject {
  public fun getNamedValue(name: String, defaultValue: JsonValue): JsonValue

  public fun getNamedObject(name: String, defaultValue: JsonObject): JsonObject

  public fun getNamedArray(name: String, defaultValue: JsonArray): JsonArray

  public fun getNamedBoolean(name: String, defaultValue: WinRtBoolean): WinRtBoolean

  public fun getNamedValue(name: String): JsonValue

  public fun setNamedValue(name: String, value: IJsonValue)

  public fun getNamedObject(name: String): JsonObject

  public fun getNamedArray(name: String): JsonArray

  public fun getNamedString(name: String): String

  public fun getNamedNumber(name: String): Float64

  public fun getNamedBoolean(name: String): WinRtBoolean

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
    get() = JsonValueType.fromValue(PlatformComInterop.invokeInt32Method(pointer, 6).getOrThrow())

  override fun getNamedValue(name: String, defaultValue: JsonValue): JsonValue =
      JsonValue(PlatformComInterop.invokeMethodWithStringAndObjectArgs(pointer, 27,
      ComMethodResultKind.OBJECT, name, projectedObjectArgumentPointer(defaultValue,
      "Windows.Data.Json.JsonValue",
      "rc(Windows.Data.Json.JsonValue;{a3219ecb-f0b3-4dcd-beee-19d48cd3ed1e})")).getOrThrow().requireObject())

  override fun getNamedObject(name: String, defaultValue: JsonObject): JsonObject =
      JsonObject(PlatformComInterop.invokeMethodWithStringAndObjectArgs(pointer, 28,
      ComMethodResultKind.OBJECT, name, projectedObjectArgumentPointer(defaultValue,
      "Windows.Data.Json.JsonObject",
      "rc(Windows.Data.Json.JsonObject;{064e24dd-29c2-4f83-9ac1-9ee11578beb3})")).getOrThrow().requireObject())

  override fun getNamedArray(name: String, defaultValue: JsonArray): JsonArray =
      JsonArray(PlatformComInterop.invokeMethodWithStringAndObjectArgs(pointer, 30,
      ComMethodResultKind.OBJECT, name, projectedObjectArgumentPointer(defaultValue,
      "Windows.Data.Json.JsonArray",
      "rc(Windows.Data.Json.JsonArray;{08c1ddb6-0cbd-4a9a-b5d3-2f852dc37e81})")).getOrThrow().requireObject())

  override fun getNamedBoolean(name: String, defaultValue: WinRtBoolean): WinRtBoolean =
      WinRtBoolean(PlatformComInterop.invokeMethodWithStringAndBooleanArgs(pointer, 32,
      ComMethodResultKind.BOOLEAN, name, defaultValue.value).getOrThrow().requireBoolean())

  override fun getNamedValue(name: String): JsonValue =
      JsonValue(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 13, name).getOrThrow())

  override fun setNamedValue(name: String, value: IJsonValue) {
    PlatformComInterop.invokeUnitMethodWithStringAndObjectArgs(pointer, 14, name,
        projectedObjectArgumentPointer(value, "Windows.Data.Json.IJsonValue",
        "{a3219ecb-f0b3-4dcd-beee-19d48cd3ed1e}")).getOrThrow()
  }

  override fun getNamedObject(name: String): JsonObject =
      JsonObject(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 15, name).getOrThrow())

  override fun getNamedArray(name: String): JsonArray =
      JsonArray(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 16, name).getOrThrow())

  override fun getNamedString(name: String): String {
    val value = PlatformComInterop.invokeHStringMethodWithStringArg(pointer, 17, name).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun getNamedNumber(name: String): Float64 =
      Float64(PlatformComInterop.invokeFloat64MethodWithStringArg(pointer, 18, name).getOrThrow())

  override fun getNamedBoolean(name: String): WinRtBoolean =
      WinRtBoolean(PlatformComInterop.invokeBooleanMethodWithStringArg(pointer, 19,
      name).getOrThrow())

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

  override fun getArray(): JsonArray = JsonArray(PlatformComInterop.invokeObjectMethod(pointer,
      11).getOrThrow())

  override fun getObject(): JsonObject = JsonObject(PlatformComInterop.invokeObjectMethod(pointer,
      12).getOrThrow())

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
}
