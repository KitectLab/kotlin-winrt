package windows.`data`.json

import kotlin.Int

public enum class JsonErrorStatus(
  public val `value`: Int,
) {
  ;
  public companion object {
    public fun fromValue(`value`: Int): JsonErrorStatus = entries.first { it.value == value }
  }
}
