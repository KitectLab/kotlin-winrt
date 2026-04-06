package microsoft.ui.xaml.media

import kotlin.Int

public enum class BrushMappingMode(
  public val value: Int,
) {
  Absolute(0),
  RelativeToBoundingBox(1),
  ;

  public companion object {
    public fun fromValue(value: Int): BrushMappingMode = entries.first { entry ->
      entry.value == value
    }
  }
}
