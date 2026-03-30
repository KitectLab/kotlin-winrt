package windows.foundation

import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

public fun interface DispatchQueue {
  public fun dispatch(block: () -> Unit): Boolean
}

public class DispatchQueueCoroutineContext internal constructor(
  public val queue: DispatchQueue,
) : CoroutineDispatcher() {
  override fun dispatch(
    context: CoroutineContext,
    block: Runnable,
  ) {
    check(queue.dispatch(block::run)) {
      "DispatchQueue rejected coroutine dispatch"
    }
  }
}

public fun DispatchQueue.asCoroutineContext(): CoroutineContext =
    DispatchQueueCoroutineContext(this)

public fun DispatchQueue.asCoroutineDispatcher(): CoroutineDispatcher =
    DispatchQueueCoroutineContext(this)
