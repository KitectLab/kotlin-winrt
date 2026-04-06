package windows.foundation

import dev.winrt.core.ComStructReader
import dev.winrt.core.ComStructWriter
import dev.winrt.kom.ComStructFieldKind
import dev.winrt.kom.ComStructLayout
import dev.winrt.kom.ComStructValue
import kotlin.Long

public data class EventRegistrationToken(
  public val value: Long,
) {
  internal fun toAbi(): ComStructValue {
    val writer = ComStructWriter(ABI_LAYOUT)
    writer.writeLong(value)
    return writer.build()
  }

  internal companion object {
    internal val ABI_LAYOUT: ComStructLayout = ComStructLayout(
      buildList {
        add(ComStructFieldKind.INT64)
      }
    )

    internal fun fromAbi(value: ComStructValue): EventRegistrationToken {
      val reader = ComStructReader(value)
      return EventRegistrationToken(reader.readLong())
    }
  }
}
