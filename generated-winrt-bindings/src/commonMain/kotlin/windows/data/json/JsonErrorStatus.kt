package windows.data.json

import kotlin.Int

public enum class JsonErrorStatus(
  public val value: Int,
) {
  Unknown(0),
  InvalidJsonString(1),
  InvalidJsonNumber(2),
  JsonValueNotFound(3),
  ImplementationLimit(4),
  ;

  public companion object {
    public fun fromValue(value: Int): JsonErrorStatus = entries.first { entry ->
      entry.value == value
    }
  }
}
