package windows.data.json

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
import kotlin.String

internal open class IJsonObjectStatics(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public fun parse(input: String): JsonObject =
      JsonObject(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 6, input).getOrThrow())

  public fun tryParse(input: String, result: JsonObject): WinRtBoolean =
      WinRtBoolean(PlatformComInterop.invokeMethodWithStringAndObjectArgs(pointer, 7,
      ComMethodResultKind.BOOLEAN, input, projectedObjectArgumentPointer(result,
      "Windows.Data.Json.JsonObject",
      "rc(Windows.Data.Json.JsonObject;{064e24dd-29c2-4f83-9ac1-9ee11578beb3})")).getOrThrow().requireBoolean())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Data.Json.IJsonObjectStatics"

    override val projectionTypeKey: String = "Windows.Data.Json.IJsonObjectStatics"

    override val iid: Guid = guidOf("2289f159-54de-45d8-abcc-22603fa066a0")

    public fun from(inspectable: Inspectable): IJsonObjectStatics =
        inspectable.projectInterface(this, ::IJsonObjectStatics)

    public operator fun invoke(inspectable: Inspectable): IJsonObjectStatics = from(inspectable)
  }
}
