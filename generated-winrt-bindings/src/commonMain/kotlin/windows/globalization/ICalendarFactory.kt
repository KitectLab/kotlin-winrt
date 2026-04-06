package windows.globalization

import dev.winrt.core.Inspectable
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import kotlin.String

public open class ICalendarFactory(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer) {
  public companion object : WinRtInterfaceMetadata {
    override val qualifiedName: String = "Windows.Globalization.ICalendarFactory"

    override val iid: Guid = guidOf("83f58412-e56b-4c75-a66e-0f63d57758a6")

    public fun from(inspectable: Inspectable): ICalendarFactory = inspectable.projectInterface(this,
        ::ICalendarFactory)
  }
}
