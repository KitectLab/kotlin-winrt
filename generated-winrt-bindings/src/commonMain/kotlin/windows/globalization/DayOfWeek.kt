package windows.globalization

import kotlin.Int

public enum class DayOfWeek(
  public val value: Int,
) {
  Sunday(0),
  Monday(1),
  Tuesday(2),
  Wednesday(3),
  Thursday(4),
  Friday(5),
  Saturday(6),
  ;

  public companion object {
    public fun fromValue(value: Int): DayOfWeek = entries.first { entry ->
      entry.value == value
    }
  }
}
