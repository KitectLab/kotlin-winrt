package windows.globalization

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import kotlin.String

public open class Calendar(
  pointer: ComPtr,
) : ICalendar(pointer) {

  public companion object : WinRtRuntimeClassMetadata {
    override val qualifiedName: String = "Windows.Globalization.Calendar"

    override val classId: RuntimeClassId = RuntimeClassId("Windows.Globalization", "Calendar")

    override val defaultInterfaceName: String? = "Windows.Globalization.ICalendar"

    override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

    public fun activate(): Calendar = WinRtRuntime.activate(this, ::Calendar)
  }
}
