package windows.foundation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

public suspend fun IAsyncAction.await() {
  when (val currentStatus = status) {
    AsyncStatus.Started -> Unit
    else -> {
      ensureActionCompleted(currentStatus)
      return
    }
  }

  suspendCancellableCoroutine { continuation ->
    lateinit var handler: AsyncActionCompletedHandler
    handler = AsyncActionCompletedHandler.create { _, asyncStatus ->
      val result = runCatching { ensureActionCompleted(asyncStatus) }
      handler.close()
      result.onSuccess {
        if (continuation.isActive) {
          continuation.resume(Unit)
        }
      }.onFailure { error ->
        if (continuation.isActive) {
          continuation.resumeWithException(error)
        }
      }
    }

    continuation.invokeOnCancellation {
      handler.close()
      runCatching { cancel() }
    }

    try {
      completed = handler
      val currentStatus = status
      if (currentStatus != AsyncStatus.Started) {
        val result = runCatching { ensureActionCompleted(currentStatus) }
        handler.close()
        result.onSuccess {
          if (continuation.isActive) {
            continuation.resume(Unit)
          }
        }.onFailure { error ->
          if (continuation.isActive) {
            continuation.resumeWithException(error)
          }
        }
      }
    } catch (error: Throwable) {
      handler.close()
      if (continuation.isActive) {
        continuation.resumeWithException(error)
      }
    }
  }
}

public suspend fun <TResult> IAsyncOperation<TResult>.await(): TResult {
  when (val currentStatus = status) {
    AsyncStatus.Started -> Unit
    else -> return completeOperation(currentStatus)
  }

  return suspendCancellableCoroutine { continuation ->
    lateinit var handler: AsyncOperationCompletedHandler<TResult>
    handler = AsyncOperationCompletedHandler.create(resultSignature) { _, asyncStatus ->
      val result = runCatching { completeOperation(asyncStatus) }
      handler.close()
      result.onSuccess { value ->
        if (continuation.isActive) {
          continuation.resume(value)
        }
      }.onFailure { error ->
        if (continuation.isActive) {
          continuation.resumeWithException(error)
        }
      }
    }

    continuation.invokeOnCancellation {
      handler.close()
      runCatching { cancel() }
    }

    try {
      completed = handler
      val currentStatus = status
      if (currentStatus != AsyncStatus.Started) {
        val result = runCatching { completeOperation(currentStatus) }
        handler.close()
        result.onSuccess { value ->
          if (continuation.isActive) {
            continuation.resume(value)
          }
        }.onFailure { error ->
          if (continuation.isActive) {
            continuation.resumeWithException(error)
          }
        }
      }
    } catch (error: Throwable) {
      handler.close()
      if (continuation.isActive) {
        continuation.resumeWithException(error)
      }
    }
  }
}

private fun IAsyncAction.ensureActionCompleted(asyncStatus: AsyncStatus) {
  when (asyncStatus) {
    AsyncStatus.Started -> error("Async action is still running")
    AsyncStatus.Completed -> getResults()
    AsyncStatus.Canceled -> throw CancellationException("WinRT async action was canceled")
    AsyncStatus.Error -> throw IllegalStateException(
        "WinRT async action failed with HRESULT ${errorCode.value}",
    )
  }
}

private fun <TResult> IAsyncOperation<TResult>.completeOperation(asyncStatus: AsyncStatus): TResult {
  return when (asyncStatus) {
    AsyncStatus.Started -> error("Async operation is still running")
    AsyncStatus.Completed -> getResults()
    AsyncStatus.Canceled -> throw CancellationException("WinRT async operation was canceled")
    AsyncStatus.Error -> throw IllegalStateException(
        "WinRT async operation failed with HRESULT ${errorCode.value}",
    )
  }
}
