package windows.foundation

import kotlin.Int

public enum class AsyncStatus(
  public val `value`: Int,
) {
  Started(0),
  Completed(1),
  Canceled(2),
  Error(3),
  ;
}
