package windows.foundation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
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

public suspend fun <TProgress> IAsyncActionWithProgress<TProgress>.await(
  onProgress: ((TProgress) -> Unit)? = null,
) {
  when (val currentStatus = status) {
    AsyncStatus.Started -> Unit
    else -> {
      ensureActionCompleted(currentStatus)
      return
    }
  }

  suspendCancellableCoroutine { continuation ->
    var progressHandler: AsyncActionProgressHandler<TProgress>? = null
    lateinit var completedHandler: AsyncActionWithProgressCompletedHandler<TProgress>
    completedHandler = AsyncActionWithProgressCompletedHandler.create(progressSignature) { _, asyncStatus ->
      progressHandler?.close()
      val result = runCatching { ensureActionCompleted(asyncStatus) }
      completedHandler.close()
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
    if (onProgress != null) {
      progressHandler = AsyncActionProgressHandler.create(
          progressSignature = progressSignature,
          progressArgumentKind = progressArgumentKind,
          decodeProgress = decodeProgress,
      ) { _, progressInfo ->
        onProgress(progressInfo)
      }
    }

    continuation.invokeOnCancellation {
      progressHandler?.close()
      completedHandler.close()
      runCatching { cancel() }
    }

    try {
      progressHandler?.let { progress = it }
      completed = completedHandler
      val currentStatus = status
      if (currentStatus != AsyncStatus.Started) {
        progressHandler?.close()
        val result = runCatching { ensureActionCompleted(currentStatus) }
        completedHandler.close()
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
      progressHandler?.close()
      completedHandler.close()
      if (continuation.isActive) {
        continuation.resumeWithException(error)
      }
    }
  }
}

public suspend fun <TResult, TProgress> IAsyncOperationWithProgress<TResult, TProgress>.await(
  onProgress: ((TProgress) -> Unit)? = null,
): TResult {
  when (val currentStatus = status) {
    AsyncStatus.Started -> Unit
    else -> return completeOperation(currentStatus)
  }

  return suspendCancellableCoroutine { continuation ->
    var progressHandler: AsyncOperationProgressHandler<TResult, TProgress>? = null
    lateinit var completedHandler: AsyncOperationWithProgressCompletedHandler<TResult, TProgress>
    completedHandler = AsyncOperationWithProgressCompletedHandler.create(
        resultSignature = resultSignature,
        progressSignature = progressSignature,
    ) { _, asyncStatus ->
      progressHandler?.close()
      val result = runCatching { completeOperation(asyncStatus) }
      completedHandler.close()
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
    if (onProgress != null) {
      progressHandler = AsyncOperationProgressHandler.create(
          resultSignature = resultSignature,
          progressSignature = progressSignature,
          progressArgumentKind = progressArgumentKind,
          decodeProgress = decodeProgress,
      ) { _, progressInfo ->
        onProgress(progressInfo)
      }
    }

    continuation.invokeOnCancellation {
      progressHandler?.close()
      completedHandler.close()
      runCatching { cancel() }
    }

    try {
      progressHandler?.let { progress = it }
      completed = completedHandler
      val currentStatus = status
      if (currentStatus != AsyncStatus.Started) {
        progressHandler?.close()
        val result = runCatching { completeOperation(currentStatus) }
        completedHandler.close()
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
      progressHandler?.close()
      completedHandler.close()
      if (continuation.isActive) {
        continuation.resumeWithException(error)
      }
    }
  }
}

public fun IAsyncAction.asDeferred(): CompletableDeferred<Unit> {
  val deferred = CompletableDeferred<Unit>()
  if (status != AsyncStatus.Started) {
    runCatching {
      ensureActionCompleted(status)
    }.onSuccess {
      deferred.complete(Unit)
    }.onFailure {
      deferred.completeExceptionally(it)
    }
    return deferred
  }

  lateinit var handler: AsyncActionCompletedHandler
  handler = AsyncActionCompletedHandler.create { _, asyncStatus ->
    val result = runCatching { ensureActionCompleted(asyncStatus) }
    handler.close()
    result.onSuccess {
      deferred.complete(Unit)
    }.onFailure { error ->
      deferred.completeExceptionally(error)
    }
  }

  deferred.invokeOnCompletion {
    if (deferred.isCancelled) {
      handler.close()
      runCatching { cancel() }
    }
  }

  try {
    completed = handler
    val currentStatus = status
    if (currentStatus != AsyncStatus.Started) {
      val result = runCatching { ensureActionCompleted(currentStatus) }
      handler.close()
      result.onSuccess {
        deferred.complete(Unit)
      }.onFailure { error ->
        deferred.completeExceptionally(error)
      }
    }
  } catch (error: Throwable) {
    handler.close()
    deferred.completeExceptionally(error)
  }

  return deferred
}

public fun <TResult> IAsyncOperation<TResult>.asDeferred(): CompletableDeferred<TResult> {
  val deferred = CompletableDeferred<TResult>()
  if (status != AsyncStatus.Started) {
    runCatching {
      completeOperation(status)
    }.onSuccess { value ->
      deferred.complete(value)
    }.onFailure { error ->
      deferred.completeExceptionally(error)
    }
    return deferred
  }

  lateinit var handler: AsyncOperationCompletedHandler<TResult>
  handler = AsyncOperationCompletedHandler.create(resultSignature) { _, asyncStatus ->
    val result = runCatching { completeOperation(asyncStatus) }
    handler.close()
    result.onSuccess { value ->
      deferred.complete(value)
    }.onFailure { error ->
      deferred.completeExceptionally(error)
    }
  }

  deferred.invokeOnCompletion {
    if (deferred.isCancelled) {
      handler.close()
      runCatching { cancel() }
    }
  }

  try {
    completed = handler
    val currentStatus = status
    if (currentStatus != AsyncStatus.Started) {
      val result = runCatching { completeOperation(currentStatus) }
      handler.close()
      result.onSuccess { value ->
        deferred.complete(value)
      }.onFailure { error ->
        deferred.completeExceptionally(error)
      }
    }
  } catch (error: Throwable) {
    handler.close()
    deferred.completeExceptionally(error)
  }

  return deferred
}

public fun <TProgress> IAsyncActionWithProgress<TProgress>.asDeferred(
  onProgress: ((TProgress) -> Unit)? = null,
): CompletableDeferred<Unit> {
  val deferred = CompletableDeferred<Unit>()
  if (status != AsyncStatus.Started) {
    runCatching {
      ensureActionCompleted(status)
    }.onSuccess {
      deferred.complete(Unit)
    }.onFailure { error ->
      deferred.completeExceptionally(error)
    }
    return deferred
  }

  var progressHandler: AsyncActionProgressHandler<TProgress>? = null
  lateinit var completedHandler: AsyncActionWithProgressCompletedHandler<TProgress>
  completedHandler = AsyncActionWithProgressCompletedHandler.create(progressSignature) { _, asyncStatus ->
    progressHandler?.close()
    val result = runCatching { ensureActionCompleted(asyncStatus) }
    completedHandler.close()
    result.onSuccess {
      deferred.complete(Unit)
    }.onFailure { error ->
      deferred.completeExceptionally(error)
    }
  }
  if (onProgress != null) {
    progressHandler = AsyncActionProgressHandler.create(
        progressSignature = progressSignature,
        progressArgumentKind = progressArgumentKind,
        decodeProgress = decodeProgress,
    ) { _, progressInfo ->
      onProgress(progressInfo)
    }
  }

  deferred.invokeOnCompletion {
    if (deferred.isCancelled) {
      progressHandler?.close()
      completedHandler.close()
      runCatching { cancel() }
    }
  }

  try {
    progressHandler?.let { progress = it }
    completed = completedHandler
    val currentStatus = status
    if (currentStatus != AsyncStatus.Started) {
      progressHandler?.close()
      val result = runCatching { ensureActionCompleted(currentStatus) }
      completedHandler.close()
      result.onSuccess {
        deferred.complete(Unit)
      }.onFailure { error ->
        deferred.completeExceptionally(error)
      }
    }
  } catch (error: Throwable) {
    progressHandler?.close()
    completedHandler.close()
    deferred.completeExceptionally(error)
  }

  return deferred
}

public fun <TResult, TProgress> IAsyncOperationWithProgress<TResult, TProgress>.asDeferred(
  onProgress: ((TProgress) -> Unit)? = null,
): CompletableDeferred<TResult> {
  val deferred = CompletableDeferred<TResult>()
  if (status != AsyncStatus.Started) {
    runCatching {
      completeOperation(status)
    }.onSuccess { value ->
      deferred.complete(value)
    }.onFailure { error ->
      deferred.completeExceptionally(error)
    }
    return deferred
  }

  var progressHandler: AsyncOperationProgressHandler<TResult, TProgress>? = null
  lateinit var completedHandler: AsyncOperationWithProgressCompletedHandler<TResult, TProgress>
  completedHandler = AsyncOperationWithProgressCompletedHandler.create(
      resultSignature = resultSignature,
      progressSignature = progressSignature,
  ) { _, asyncStatus ->
    progressHandler?.close()
    val result = runCatching { completeOperation(asyncStatus) }
    completedHandler.close()
    result.onSuccess { value ->
      deferred.complete(value)
    }.onFailure { error ->
      deferred.completeExceptionally(error)
    }
  }
  if (onProgress != null) {
    progressHandler = AsyncOperationProgressHandler.create(
        resultSignature = resultSignature,
        progressSignature = progressSignature,
        progressArgumentKind = progressArgumentKind,
        decodeProgress = decodeProgress,
    ) { _, progressInfo ->
      onProgress(progressInfo)
    }
  }

  deferred.invokeOnCompletion {
    if (deferred.isCancelled) {
      progressHandler?.close()
      completedHandler.close()
      runCatching { cancel() }
    }
  }

  try {
    progressHandler?.let { progress = it }
    completed = completedHandler
    val currentStatus = status
    if (currentStatus != AsyncStatus.Started) {
      progressHandler?.close()
      val result = runCatching { completeOperation(currentStatus) }
      completedHandler.close()
      result.onSuccess { value ->
        deferred.complete(value)
      }.onFailure { error ->
        deferred.completeExceptionally(error)
      }
    }
  } catch (error: Throwable) {
    progressHandler?.close()
    completedHandler.close()
    deferred.completeExceptionally(error)
  }

  return deferred
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

private fun <TProgress> IAsyncActionWithProgress<TProgress>.ensureActionCompleted(asyncStatus: AsyncStatus) {
  when (asyncStatus) {
    AsyncStatus.Started -> error("Async action with progress is still running")
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

private fun <TResult, TProgress> IAsyncOperationWithProgress<TResult, TProgress>.completeOperation(
  asyncStatus: AsyncStatus,
): TResult {
  return when (asyncStatus) {
    AsyncStatus.Started -> error("Async operation with progress is still running")
    AsyncStatus.Completed -> getResults()
    AsyncStatus.Canceled -> throw CancellationException("WinRT async operation was canceled")
    AsyncStatus.Error -> throw IllegalStateException(
        "WinRT async operation failed with HRESULT ${errorCode.value}",
    )
  }
}
