package microsoft.ui.xaml

import kotlin.Int

public enum class ApplicationRequiresPointerMode(
  public val `value`: Int,
) {
  ;
  public companion object {
    public fun fromValue(`value`: Int): ApplicationRequiresPointerMode = entries.first { it.value == value }
  }
}
