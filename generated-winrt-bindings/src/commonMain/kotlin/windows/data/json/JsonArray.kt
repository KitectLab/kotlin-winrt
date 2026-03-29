package windows.`data`.json

import dev.winrt.core.Float64
import dev.winrt.core.Inspectable
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.core.WinRtStrings
import dev.winrt.kom.ComPtr
import dev.winrt.kom.PlatformComInterop
import kotlin.String

public open class JsonArray(
  pointer: ComPtr,
) : Inspectable(pointer) {

  public fun getObjectAt(index: UInt32): JsonObject =
      JsonObject(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 6,
      index.value).getOrThrow())

  public fun getArrayAt(index: UInt32): JsonArray =
      JsonArray(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 7,
      index.value).getOrThrow())

  public fun getStringAt(index: UInt32): String {
    val value = PlatformComInterop.invokeHStringMethodWithUInt32Arg(pointer, 8,
        index.value).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun getNumberAt(index: UInt32): Float64 =
      Float64(PlatformComInterop.invokeFloat64MethodWithUInt32Arg(pointer, 9,
      index.value).getOrThrow())

  public fun getBooleanAt(index: UInt32): WinRtBoolean =
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
