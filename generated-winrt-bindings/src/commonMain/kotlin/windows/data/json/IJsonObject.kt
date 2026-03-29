package windows.`data`.json

import dev.winrt.core.Float64
import dev.winrt.core.Inspectable
import dev.winrt.core.PointerOwner
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtStrings
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String

public interface IJsonObject : PointerOwner {
  public fun getNamedValue(name: String): IJsonValue =
      IJsonValue.from(Inspectable(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 6,
      name).getOrThrow()))

  public fun getNamedString(name: String): String {
    val value = PlatformComInterop.invokeHStringMethodWithStringArg(pointer, 10, name).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun getNamedObject(name: String): JsonObject = JsonObject(
      PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 8, name).getOrThrow())

  public fun getNamedArray(name: String): JsonArray = JsonArray(
      PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 9, name).getOrThrow())

  public fun getNamedNumber(name: String): Float64 =
      Float64(PlatformComInterop.invokeFloat64MethodWithStringArg(pointer, 11, name).getOrThrow())

  public fun getNamedBoolean(name: String): WinRtBoolean =
      WinRtBoolean(PlatformComInterop.invokeBooleanMethodWithStringArg(pointer, 12,
      name).getOrThrow())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Data.Json.IJsonObject"

    override val iid: Guid = guidOf("064e24dd-29c2-4f83-9ac1-9ee11578beb3")

    public fun from(inspectable: Inspectable): IJsonObject = inspectable.projectInterface(this,
        ::IJsonObjectProjection)
  }
}

private class IJsonObjectProjection(
  pointer: ComPtr,
) : dev.winrt.core.WinRtInterfaceProjection(pointer), IJsonObject
