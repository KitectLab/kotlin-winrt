package windows.foundation

import dev.winrt.core.UInt32
import dev.winrt.kom.ComPtr
import dev.winrt.kom.HResult
import kotlinx.coroutines.CancellationException
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
