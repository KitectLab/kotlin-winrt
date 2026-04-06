package microsoft.ui.xaml

import dev.winrt.core.ComStructReader
import dev.winrt.core.ComStructWriter
import dev.winrt.kom.ComStructFieldKind
import dev.winrt.kom.ComStructLayout
import dev.winrt.kom.ComStructValue
import kotlin.Double

public data class Thickness(
  public val left: Double,
  public val top: Double,
  public val right: Double,
  public val bottom: Double,
) {
  internal fun toAbi(): ComStructValue {
    val writer = ComStructWriter(ABI_LAYOUT)
    writer.writeDouble(left)
    writer.writeDouble(top)
    writer.writeDouble(right)
    writer.writeDouble(bottom)
    return writer.build()
  }

  internal companion object {
    internal val ABI_LAYOUT: ComStructLayout = ComStructLayout(
      buildList {
        add(ComStructFieldKind.FLOAT64)
        add(ComStructFieldKind.FLOAT64)
        add(ComStructFieldKind.FLOAT64)
        add(ComStructFieldKind.FLOAT64)
      }
    )

    internal fun fromAbi(value: ComStructValue): Thickness {
      val reader = ComStructReader(value)
      return Thickness(reader.readDouble(), reader.readDouble(), reader.readDouble(),
          reader.readDouble())
    }
  }
}
