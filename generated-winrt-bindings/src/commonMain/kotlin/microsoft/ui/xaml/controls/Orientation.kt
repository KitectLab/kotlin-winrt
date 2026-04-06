package microsoft.ui.xaml.controls

import kotlin.Int

public enum class Orientation(
  public val value: Int,
) {
  Vertical(0),
  Horizontal(1),
  ;

  public companion object {
    public fun fromValue(value: Int): Orientation = entries.first { entry ->
      entry.value == value
    }
  }
}
