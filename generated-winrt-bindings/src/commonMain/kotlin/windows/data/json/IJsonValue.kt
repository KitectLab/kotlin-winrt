package windows.`data`.json

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtStrings
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String

public open class IJsonValue(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public val valueType: JsonValueType
    get() = get_ValueType()

  public fun get_ValueType(): JsonValueType =
      JsonValueType.fromValue(PlatformComInterop.invokeUInt32Method(pointer, 6).getOrThrow().toInt())

  public fun stringify(): String {
    val value = PlatformComInterop.invokeHStringMethod(pointer, 7).getOrThrow()
    return try {
      WinRtStrings.toKotlin(value)
    } finally {
      WinRtStrings.release(value)
    }
  }

  public fun getObject(): JsonObject = JsonObject(PlatformComInterop.invokeObjectMethod(pointer,
      12).getOrThrow())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Data.Json.IJsonValue"

    override val iid: Guid = guidOf("a3219ecb-f0b3-4dcd-beee-19d48cd3ed1e")

    public fun from(inspectable: Inspectable): IJsonValue = inspectable.projectInterface(this,
        ::IJsonValue)
  }
}
