package windows.data.json

import kotlin.Int

public enum class JsonValueType(
  public val value: Int,
) {
  Null(0),
  Boolean(1),
  Number(2),
  String(3),
  Array(4),
  Object(5),
  ;

  public companion object {
    public fun fromValue(value: Int): JsonValueType = entries.first { entry ->
      entry.value == value
    }
  }
}
