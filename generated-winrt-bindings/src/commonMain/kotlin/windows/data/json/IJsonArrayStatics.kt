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

internal open class IJsonArrayStatics(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public fun parse(input: String): JsonArray =
      JsonArray(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 6, input).getOrThrow())

  public fun tryParse(input: String, result: JsonArray): WinRtBoolean =
      WinRtBoolean(PlatformComInterop.invokeMethodWithStringAndObjectArgs(pointer, 7,
      ComMethodResultKind.BOOLEAN, input, projectedObjectArgumentPointer(result,
      "Windows.Data.Json.JsonArray",
      "rc(Windows.Data.Json.JsonArray;{08c1ddb6-0cbd-4a9a-b5d3-2f852dc37e81})")).getOrThrow().requireBoolean())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Data.Json.IJsonArrayStatics"

    override val projectionTypeKey: String = "Windows.Data.Json.IJsonArrayStatics"

    override val iid: Guid = guidOf("db1434a9-e164-499f-93e2-8a8f49bb90ba")

    public fun from(inspectable: Inspectable): IJsonArrayStatics =
        inspectable.projectInterface(this, ::IJsonArrayStatics)

    public operator fun invoke(inspectable: Inspectable): IJsonArrayStatics = from(inspectable)
  }
}
