package windows.`data`.json

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

internal open class IJsonValueStatics(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public fun parse(input: String): JsonValue =
      JsonValue(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 6, input).getOrThrow())

  public fun createBooleanValue(input: WinRtBoolean): JsonValue =
      JsonValue(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 8, if (input.value) 1u
      else 0u).getOrThrow())

  public fun createStringValue(input: String): JsonValue =
      JsonValue(PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 10, input).getOrThrow())

  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Data.Json.IJsonValueStatics"

    override val projectionTypeKey: String = "Windows.Data.Json.IJsonValueStatics"

    override val iid: Guid = guidOf("5f6b544a-2f53-48e1-91a3-f78b50a6345c")

    public fun from(inspectable: Inspectable): IJsonValueStatics =
        inspectable.projectInterface(this, ::IJsonValueStatics)

    public operator fun invoke(inspectable: Inspectable): IJsonValueStatics = from(inspectable)
  }
}
