package microsoft.ui.xaml

import kotlin.Int

public enum class ApplicationTheme(
  public val `value`: Int,
) {
  ;
  public companion object {
    public fun fromValue(`value`: Int): ApplicationTheme = entries.first { it.value == value }
  }
}
