package windows.`data`.json

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import kotlin.String

internal open class IJsonErrorStatics2(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Data.Json.IJsonErrorStatics2"

    override val projectionTypeKey: String = "Windows.Data.Json.IJsonErrorStatics2"

    override val iid: Guid = guidOf("404030da-87d0-436c-83ab-fc7b12c0cc26")

    public fun from(inspectable: Inspectable): IJsonErrorStatics2 =
        inspectable.projectInterface(this, ::IJsonErrorStatics2)
  }
}
