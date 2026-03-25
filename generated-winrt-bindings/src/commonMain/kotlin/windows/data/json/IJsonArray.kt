package windows.`data`.json

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid

public open class IJsonArray(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Data.Json.IJsonArray"

    override val iid: Guid = guidOf("08c1ddb6-0cbd-4a9a-b5d3-2f852dc37e81")

    public fun from(inspectable: Inspectable): IJsonArray = inspectable.projectInterface(this,
        ::IJsonArray)
  }
}
