package windows.foundation

import kotlin.Int

public enum class AsyncStatus(
  public val value: Int,
) {
  Canceled(2),
  Completed(1),
  Error(3),
  Started(0),
  ;

  public companion object {
    public fun fromValue(value: Int): AsyncStatus = entries.first { entry ->
      entry.value == value
    }
  }
}
