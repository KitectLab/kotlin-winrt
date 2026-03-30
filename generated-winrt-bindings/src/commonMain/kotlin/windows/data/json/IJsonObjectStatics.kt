package windows.`data`.json

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.String

internal open class IJsonObjectStatics(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public fun parse(input: String): JsonObject =
      JsonObject(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 6, input).getOrThrow())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Data.Json.IJsonObjectStatics"

    override val projectionTypeKey: String = "Windows.Data.Json.IJsonObjectStatics"

    override val iid: Guid = guidOf("2289f159-54de-45d8-abcc-22603fa066a0")

    public fun from(inspectable: Inspectable): IJsonObjectStatics =
        inspectable.projectInterface(this, ::IJsonObjectStatics)
  }
}
