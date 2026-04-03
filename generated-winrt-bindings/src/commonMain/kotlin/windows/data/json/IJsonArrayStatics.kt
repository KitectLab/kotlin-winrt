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

internal open class IJsonArrayStatics(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public fun parse(input: String): JsonArray =
      JsonArray(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 6, input).getOrThrow())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Data.Json.IJsonArrayStatics"

    override val projectionTypeKey: String = "Windows.Data.Json.IJsonArrayStatics"

    override val iid: Guid = guidOf("db1434a9-e164-499f-93e2-8a8f49bb90ba")

    public fun from(inspectable: Inspectable): IJsonArrayStatics =
        inspectable.projectInterface(this, ::IJsonArrayStatics)

    public operator fun invoke(inspectable: Inspectable): IJsonArrayStatics = from(inspectable)
  }
}
