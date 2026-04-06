package microsoft.ui.xaml

import kotlin.UInt

public enum class ApplicationHighContrastAdjustment(
  public val value: UInt,
) {
  None(0u),
  Auto(0xffffffffu),
  ;

  public companion object {
    public fun fromValue(value: UInt): ApplicationHighContrastAdjustment = entries.first {
        entry ->
      entry.value == value
    }
  }
}
