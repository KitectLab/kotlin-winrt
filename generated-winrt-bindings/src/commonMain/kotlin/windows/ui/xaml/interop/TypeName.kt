package windows.ui.xaml.interop

import dev.winrt.core.ComStructReader
import dev.winrt.core.ComStructWriter
import dev.winrt.kom.ComStructFieldKind
import dev.winrt.kom.ComStructLayout
import dev.winrt.kom.ComStructValue
import kotlin.String

public data class TypeName(
  public val name: String,
  public val kind: TypeKind,
) {
  internal fun toAbi(): ComStructValue {
    val writer = ComStructWriter(ABI_LAYOUT)
    writer.writeHString(name)
    writer.writeInt(kind.value)
    return writer.build()
  }

  internal companion object {
    internal val ABI_LAYOUT: ComStructLayout = ComStructLayout(
      buildList {
        add(ComStructFieldKind.HSTRING)
        add(ComStructFieldKind.INT32)
      }
    )

    internal fun fromAbi(value: ComStructValue): TypeName {
      val reader = ComStructReader(value)
      return TypeName(reader.readHString(), TypeKind.fromValue(reader.readInt()))
    }
  }
}
