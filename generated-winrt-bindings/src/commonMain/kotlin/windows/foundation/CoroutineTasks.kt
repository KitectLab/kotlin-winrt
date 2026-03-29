package windows.foundation

import dev.winrt.core.WinRtDelegateValueKind
import dev.winrt.core.UInt32
import dev.winrt.kom.ComPtr
import dev.winrt.kom.HResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

private val eFail = HResult(0x80004005.toInt())

public fun CoroutineScope.asyncAction(
  block: suspend CoroutineScope.() -> Unit,
): IAsyncAction {
  val job = launch(block = block)
  return LocalAsyncAction(job)
}

public fun <TResult> CoroutineScope.asyncOperation(
  resultSignature: String,
  block: suspend CoroutineScope.() -> TResult,
): IAsyncOperation<TResult> {
  val deferred = async(block = block)
  return LocalAsyncOperation(
      deferred = deferred,
      resultSignature = resultSignature,
  )
}

public fun <TProgress> CoroutineScope.asyncActionWithProgress(
  progressSignature: String,
  progressArgumentKind: WinRtDelegateValueKind,
  block: suspend CoroutineScope.(reportProgress: (TProgress) -> Unit) -> Unit,
): IAsyncActionWithProgress<TProgress> {
  var progressEmitter: ((TProgress) -> Unit)? = null
  val job = launch(start = CoroutineStart.LAZY) {
    block { progress ->
      progressEmitter?.invoke(progress)
    }
  }
  val action = LocalAsyncActionWithProgress<TProgress>(
      job = job,
      progressSignature = progressSignature,
      progressArgumentKind = progressArgumentKind,
  )
  progressEmitter = { progress -> action.emitProgress(progress) }
  job.start()
  return action
}

public fun <TResult, TProgress> CoroutineScope.asyncOperationWithProgress(
  resultSignature: String,
  progressSignature: String,
  progressArgumentKind: WinRtDelegateValueKind,
  block: suspend CoroutineScope.(reportProgress: (TProgress) -> Unit) -> TResult,
): IAsyncOperationWithProgress<TResult, TProgress> {
  var progressEmitter: ((TProgress) -> Unit)? = null
  val deferred = async(start = CoroutineStart.LAZY) {
    block { progress ->
      progressEmitter?.invoke(progress)
    }
  }
  val operation = LocalAsyncOperationWithProgress<TResult, TProgress>(
      deferred = deferred,
      resultSignature = resultSignature,
      progressSignature = progressSignature,
      progressArgumentKind = progressArgumentKind,
  )
  progressEmitter = { progress -> operation.emitProgress(progress) }
  deferred.start()
  return operation
}

private class LocalAsyncAction(
  private val job: Job,
) : IAsyncAction(ComPtr.NULL) {
  @Volatile
  private var statusState: AsyncStatus = AsyncStatus.Started

  @Volatile
  private var errorState: HResult = HResult.OK

  @Volatile
  private var completedHandler: AsyncActionCompletedHandler? = null

  override val id: UInt32
    get() = UInt32(0u)

  override val status: AsyncStatus
    get() = statusState

  override val errorCode: HResult
    get() = errorState

  init {
    job.invokeOnCompletion { cause ->
      when (cause) {
        null -> statusState = AsyncStatus.Completed
        is CancellationException -> statusState = AsyncStatus.Canceled
        else -> {
          statusState = AsyncStatus.Error
          errorState = eFail
        }
      }
      completedHandler?.invoke(this, statusState)
    }
  }

  override fun put_Completed(handler: AsyncActionCompletedHandler) {
    completedHandler = handler
    if (statusState != AsyncStatus.Started) {
      handler.invoke(this, statusState)
    }
  }

  override fun get_Completed(): AsyncActionCompletedHandler =
      completedHandler ?: AsyncActionCompletedHandler(ComPtr.NULL)

  override fun getResults() {
    when (statusState) {
      AsyncStatus.Completed -> return
      AsyncStatus.Canceled -> throw CancellationException("WinRT async action was canceled")
      AsyncStatus.Error -> throw IllegalStateException(
          "WinRT async action failed with HRESULT ${errorState.value}",
      )
      AsyncStatus.Started -> error("Async action is still running")
    }
  }

  override fun cancel() {
    job.cancel()
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class LocalAsyncOperation<TResult>(
  private val deferred: Deferred<TResult>,
  resultSignature: String,
) : IAsyncOperation<TResult>(ComPtr.NULL, resultSignature) {
  @Volatile
  private var statusState: AsyncStatus = AsyncStatus.Started

  @Volatile
  private var errorState: HResult = HResult.OK

  @Volatile
  private var completedHandler: AsyncOperationCompletedHandler<TResult>? = null

  @Volatile
  private var completedResult: Result<TResult>? = null

  override val id: UInt32
    get() = UInt32(0u)

  override val status: AsyncStatus
    get() = statusState

  override val errorCode: HResult
    get() = errorState

  init {
    deferred.invokeOnCompletion { cause ->
      completedResult = when (cause) {
        null -> runCatching { deferred.getCompleted() }
        else -> Result.failure(cause)
      }
      when (cause) {
        null -> statusState = AsyncStatus.Completed
        is CancellationException -> statusState = AsyncStatus.Canceled
        else -> {
          statusState = AsyncStatus.Error
          errorState = eFail
        }
      }
      completedHandler?.invoke(pointer, statusState)
    }
  }

  override fun put_Completed(handler: AsyncOperationCompletedHandler<TResult>) {
    completedHandler = handler
    if (statusState != AsyncStatus.Started) {
      handler.invoke(pointer, statusState)
    }
  }

  override fun get_Completed(): AsyncOperationCompletedHandler<TResult> =
      completedHandler ?: AsyncOperationCompletedHandler(ComPtr.NULL)

  override fun getResults(): TResult {
    return when (statusState) {
      AsyncStatus.Completed -> completedResult?.getOrThrow()
          ?: error("Async operation completed without a result")
      AsyncStatus.Canceled -> throw CancellationException("WinRT async operation was canceled")
      AsyncStatus.Error -> throw IllegalStateException(
          "WinRT async operation failed with HRESULT ${errorState.value}",
      )
      AsyncStatus.Started -> error("Async operation is still running")
    }
  }

  override fun cancel() {
    deferred.cancel()
  }
}

private class LocalAsyncActionWithProgress<TProgress>(
  private val job: Job,
  progressSignature: String,
  progressArgumentKind: WinRtDelegateValueKind,
) : IAsyncActionWithProgress<TProgress>(
    pointer = ComPtr.NULL,
    progressSignature = progressSignature,
    progressArgumentKind = progressArgumentKind,
    decodeProgress = { decodeProgressValue(it) },
) {
  @Volatile
  private var statusState: AsyncStatus = AsyncStatus.Started

  @Volatile
  private var errorState: HResult = HResult.OK

  @Volatile
  private var progressHandler: AsyncActionProgressHandler<TProgress>? = null

  private val pendingProgress = mutableListOf<TProgress>()

  @Volatile
  private var completedHandler: AsyncActionWithProgressCompletedHandler<TProgress>? = null

  override val id: UInt32
    get() = UInt32(0u)

  override val status: AsyncStatus
    get() = statusState

  override val errorCode: HResult
    get() = errorState

  init {
    job.invokeOnCompletion { cause ->
      when (cause) {
        null -> statusState = AsyncStatus.Completed
        is CancellationException -> statusState = AsyncStatus.Canceled
        else -> {
          statusState = AsyncStatus.Error
          errorState = eFail
        }
      }
      completedHandler?.invoke(pointer, statusState)
    }
  }

  fun emitProgress(progress: TProgress) {
    val handler = progressHandler
    if (handler != null) {
      handler.invoke(pointer, progress)
      return
    }
    synchronized(pendingProgress) {
      progressHandler?.invoke(pointer, progress) ?: pendingProgress.add(progress)
    }
  }

  override fun put_Progress(handler: AsyncActionProgressHandler<TProgress>) {
    progressHandler = handler
    val buffered = synchronized(pendingProgress) {
      pendingProgress.toList().also { pendingProgress.clear() }
    }
    buffered.forEach { progress -> handler.invoke(pointer, progress) }
  }

  override fun get_Progress(): AsyncActionProgressHandler<TProgress> =
      progressHandler ?: AsyncActionProgressHandler(ComPtr.NULL)

  override fun put_Completed(handler: AsyncActionWithProgressCompletedHandler<TProgress>) {
    completedHandler = handler
    if (statusState != AsyncStatus.Started) {
      handler.invoke(pointer, statusState)
    }
  }

  override fun get_Completed(): AsyncActionWithProgressCompletedHandler<TProgress> =
      completedHandler ?: AsyncActionWithProgressCompletedHandler(ComPtr.NULL)

  override fun getResults() {
    when (statusState) {
      AsyncStatus.Completed -> return
      AsyncStatus.Canceled -> throw CancellationException("WinRT async action with progress was canceled")
      AsyncStatus.Error -> throw IllegalStateException(
          "WinRT async action with progress failed with HRESULT ${errorState.value}",
      )
      AsyncStatus.Started -> error("Async action with progress is still running")
    }
  }

  override fun cancel() {
    job.cancel()
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class LocalAsyncOperationWithProgress<TResult, TProgress>(
  private val deferred: Deferred<TResult>,
  resultSignature: String,
  progressSignature: String,
  progressArgumentKind: WinRtDelegateValueKind,
) : IAsyncOperationWithProgress<TResult, TProgress>(
    pointer = ComPtr.NULL,
    resultSignature = resultSignature,
    progressSignature = progressSignature,
    progressArgumentKind = progressArgumentKind,
    decodeProgress = { decodeProgressValue(it) },
) {
  @Volatile
  private var statusState: AsyncStatus = AsyncStatus.Started

  @Volatile
  private var errorState: HResult = HResult.OK

  @Volatile
  private var progressHandler: AsyncOperationProgressHandler<TResult, TProgress>? = null

  private val pendingProgress = mutableListOf<TProgress>()

  @Volatile
  private var completedHandler: AsyncOperationWithProgressCompletedHandler<TResult, TProgress>? = null

  @Volatile
  private var completedResult: Result<TResult>? = null

  override val id: UInt32
    get() = UInt32(0u)

  override val status: AsyncStatus
    get() = statusState

  override val errorCode: HResult
    get() = errorState

  init {
    deferred.invokeOnCompletion { cause ->
      completedResult = when (cause) {
        null -> runCatching { deferred.getCompleted() }
        else -> Result.failure(cause)
      }
      when (cause) {
        null -> statusState = AsyncStatus.Completed
        is CancellationException -> statusState = AsyncStatus.Canceled
        else -> {
          statusState = AsyncStatus.Error
          errorState = eFail
        }
      }
      completedHandler?.invoke(pointer, statusState)
    }
  }

  fun emitProgress(progress: TProgress) {
    val handler = progressHandler
    if (handler != null) {
      handler.invoke(pointer, progress)
      return
    }
    synchronized(pendingProgress) {
      progressHandler?.invoke(pointer, progress) ?: pendingProgress.add(progress)
    }
  }

  override fun put_Progress(handler: AsyncOperationProgressHandler<TResult, TProgress>) {
    progressHandler = handler
    val buffered = synchronized(pendingProgress) {
      pendingProgress.toList().also { pendingProgress.clear() }
    }
    buffered.forEach { progress -> handler.invoke(pointer, progress) }
  }

  override fun get_Progress(): AsyncOperationProgressHandler<TResult, TProgress> =
      progressHandler ?: AsyncOperationProgressHandler(ComPtr.NULL)

  override fun put_Completed(handler: AsyncOperationWithProgressCompletedHandler<TResult, TProgress>) {
    completedHandler = handler
    if (statusState != AsyncStatus.Started) {
      handler.invoke(pointer, statusState)
    }
  }

  override fun get_Completed(): AsyncOperationWithProgressCompletedHandler<TResult, TProgress> =
      completedHandler ?: AsyncOperationWithProgressCompletedHandler(ComPtr.NULL)

  override fun getResults(): TResult {
    return when (statusState) {
      AsyncStatus.Completed -> completedResult?.getOrThrow()
          ?: error("Async operation with progress completed without a result")
      AsyncStatus.Canceled -> throw CancellationException("WinRT async operation with progress was canceled")
      AsyncStatus.Error -> throw IllegalStateException(
          "WinRT async operation with progress failed with HRESULT ${errorState.value}",
      )
      AsyncStatus.Started -> error("Async operation with progress is still running")
    }
  }

  override fun cancel() {
    deferred.cancel()
  }
}

@Suppress("UNCHECKED_CAST")
private fun <TProgress> decodeProgressValue(value: Any?): TProgress = value as TProgress
