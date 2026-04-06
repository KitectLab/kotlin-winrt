package microsoft.ui.xaml

import dev.winrt.core.ComStructReader
import dev.winrt.core.ComStructWriter
import dev.winrt.kom.ComStructFieldKind
import dev.winrt.kom.ComStructLayout
import dev.winrt.kom.ComStructValue
import kotlin.Double

public data class CornerRadius(
  public val topLeft: Double,
  public val topRight: Double,
  public val bottomRight: Double,
  public val bottomLeft: Double,
) {
  internal fun toAbi(): ComStructValue {
    val writer = ComStructWriter(ABI_LAYOUT)
    writer.writeDouble(topLeft)
    writer.writeDouble(topRight)
    writer.writeDouble(bottomRight)
    writer.writeDouble(bottomLeft)
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

    internal fun fromAbi(value: ComStructValue): CornerRadius {
      val reader = ComStructReader(value)
      return CornerRadius(reader.readDouble(), reader.readDouble(), reader.readDouble(),
          reader.readDouble())
    }
  }
}
