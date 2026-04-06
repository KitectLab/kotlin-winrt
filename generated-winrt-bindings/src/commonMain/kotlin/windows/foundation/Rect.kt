package windows.foundation

import dev.winrt.core.ComStructReader
import dev.winrt.core.ComStructWriter
import dev.winrt.kom.ComStructFieldKind
import dev.winrt.kom.ComStructLayout
import dev.winrt.kom.ComStructValue
import kotlin.Float

public data class Rect(
  public val x: Float,
  public val y: Float,
  public val width: Float,
  public val height: Float,
) {
  internal fun toAbi(): ComStructValue {
    val writer = ComStructWriter(ABI_LAYOUT)
    writer.writeFloat(x)
    writer.writeFloat(y)
    writer.writeFloat(width)
    writer.writeFloat(height)
    return writer.build()
  }

  internal companion object {
    internal val ABI_LAYOUT: ComStructLayout = ComStructLayout(
      buildList {
        add(ComStructFieldKind.FLOAT32)
        add(ComStructFieldKind.FLOAT32)
        add(ComStructFieldKind.FLOAT32)
        add(ComStructFieldKind.FLOAT32)
      }
    )

    internal fun fromAbi(value: ComStructValue): Rect {
      val reader = ComStructReader(value)
      return Rect(reader.readFloat(), reader.readFloat(), reader.readFloat(), reader.readFloat())
    }
  }
}
