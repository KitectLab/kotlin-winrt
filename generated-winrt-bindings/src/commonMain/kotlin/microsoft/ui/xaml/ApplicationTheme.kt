package microsoft.ui.xaml

import kotlin.Int

public enum class ApplicationTheme(
  public val value: Int,
) {
  Light(0),
  Dark(1),
  ;

  public companion object {
    public fun fromValue(value: Int): ApplicationTheme = entries.first { entry ->
      entry.value == value
    }
  }
}
