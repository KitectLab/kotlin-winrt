package microsoft.ui.xaml

import kotlin.Int

public enum class ApplicationRequiresPointerMode(
  public val value: Int,
) {
  Auto(0),
  WhenRequested(1),
  ;

  public companion object {
    public fun fromValue(value: Int): ApplicationRequiresPointerMode = entries.first { entry ->
      entry.value == value
    }
  }
}
