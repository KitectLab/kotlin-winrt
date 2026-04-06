package microsoft.ui.xaml.controls

import kotlin.Int

public enum class BackgroundSizing(
  public val value: Int,
) {
  InnerBorderEdge(0),
  OuterBorderEdge(1),
  ;

  public companion object {
    public fun fromValue(value: Int): BackgroundSizing = entries.first { entry ->
      entry.value == value
    }
  }
}
