package windows.data.json

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String

internal open class IJsonValueStatics2(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public fun createNullValue(): JsonValue = JsonValue(PlatformComInterop.invokeObjectMethod(pointer,
      6).getOrThrow())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Data.Json.IJsonValueStatics2"

    override val projectionTypeKey: String = "Windows.Data.Json.IJsonValueStatics2"

    override val iid: Guid = guidOf("1d9ecbe4-3fe8-4335-8392-93d8e36865f0")

    public fun from(inspectable: Inspectable): IJsonValueStatics2 =
        inspectable.projectInterface(this, ::IJsonValueStatics2)

    public operator fun invoke(inspectable: Inspectable): IJsonValueStatics2 = from(inspectable)
  }
}
