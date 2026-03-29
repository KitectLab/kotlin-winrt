package windows.`data`.json

import dev.winrt.core.Float64
import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtBoolean
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import kotlin.String

public interface IJsonValue {
  public val valueType: JsonValueType

  public fun get_ValueType(): JsonValueType

  public fun stringify(): String

  public fun getString(): String

  public fun getNumber(): Float64

  public fun getBoolean(): WinRtBoolean

  public fun getArray(): JsonArray

  public fun getObject(): JsonObject

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Data.Json.IJsonValue"

    override val iid: Guid = guidOf("a3219ecb-f0b3-4dcd-beee-19d48cd3ed1e")

    public fun from(inspectable: Inspectable): IJsonValue = inspectable.projectInterface(this,
        ::IJsonValueProjection)
  }
}

private class IJsonValueProjection(
  pointer: ComPtr,
) : dev.winrt.core.WinRtInterfaceProjection(pointer), IJsonValue {
  override val valueType: JsonValueType
    get() = get_ValueType()

  override fun get_ValueType(): JsonValueType =
      JsonValueType.fromValue(dev.winrt.kom.PlatformComInterop.invokeUInt32Method(pointer, 6).getOrThrow().toInt())

  override fun stringify(): String {
    val value = dev.winrt.kom.PlatformComInterop.invokeHStringMethod(pointer, 7).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun getString(): String {
    val value = dev.winrt.kom.PlatformComInterop.invokeHStringMethod(pointer, 8).getOrThrow()
    return try {
      value.toKotlinString()
    } finally {
      value.close()
    }
  }

  override fun getNumber(): Float64 =
      Float64(dev.winrt.kom.PlatformComInterop.invokeFloat64Method(pointer, 9).getOrThrow())

  override fun getBoolean(): WinRtBoolean =
      WinRtBoolean(dev.winrt.kom.PlatformComInterop.invokeBooleanGetter(pointer, 10).getOrThrow())

  override fun getArray(): JsonArray = JsonArray(dev.winrt.kom.PlatformComInterop.invokeObjectMethod(pointer, 11)
      .getOrThrow())

  override fun getObject(): JsonObject = JsonObject(dev.winrt.kom.PlatformComInterop.invokeObjectMethod(pointer,
      12).getOrThrow())
}
