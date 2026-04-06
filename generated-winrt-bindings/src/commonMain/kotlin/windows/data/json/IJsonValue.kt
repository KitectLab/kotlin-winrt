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

public interface IJsonValue {
  public val valueType: JsonValueType

  public fun get_ValueType(): JsonValueType

  public fun stringify(): String

  public fun getString(): String

  public fun getNumber(): Float64

  public fun getBoolean(): WinRtBoolean

  public fun getObject(): JsonObject

  public fun getArray(): JsonArray

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Data.Json.IJsonValue"

    override val projectionTypeKey: String = "Windows.Data.Json.IJsonValue"

    override val iid: Guid = guidOf("a3219ecb-f0b3-4dcd-beee-19d48cd3ed1e")

    public fun from(inspectable: Inspectable): IJsonValue = inspectable.projectInterface(this,
        ::IJsonValueProjection)

    public operator fun invoke(inspectable: Inspectable): IJsonValue = from(inspectable)
  }
}

private class IJsonValueProjection(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    IJsonValue {
  override val valueType: JsonValueType
    get() = JsonValueType.fromValue(PlatformComInterop.invokeInt32Method(pointer, 6).getOrThrow())

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
