package microsoft.ui.xaml

import kotlin.Int

public enum class ApplicationHighContrastAdjustment(
  public val `value`: Int,
) {
  ;
  public companion object {
    public fun fromValue(`value`: Int): ApplicationHighContrastAdjustment = entries.first { it.value == value }
  }
}
