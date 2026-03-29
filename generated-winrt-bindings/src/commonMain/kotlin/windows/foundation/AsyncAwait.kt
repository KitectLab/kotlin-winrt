package windows.foundation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

public suspend fun IAsyncAction.await(pollIntervalMillis: Long = 1L) {
  try {
    while (true) {
      when (status) {
        AsyncStatus.Started -> delay(pollIntervalMillis)
        AsyncStatus.Completed -> {
          getResults()
          return
        }
        AsyncStatus.Canceled -> throw CancellationException("WinRT async action was canceled")
        AsyncStatus.Error -> throw IllegalStateException(
            "WinRT async action failed with HRESULT ${errorCode.value}",
        )
      }
    }
  } catch (e: CancellationException) {
    cancel()
    throw e
  }
}

public suspend fun <TResult> IAsyncOperation<TResult>.await(pollIntervalMillis: Long = 1L): TResult {
  try {
    while (true) {
      when (status) {
        AsyncStatus.Started -> delay(pollIntervalMillis)
        AsyncStatus.Completed -> return getResults()
        AsyncStatus.Canceled -> throw CancellationException("WinRT async operation was canceled")
        AsyncStatus.Error -> throw IllegalStateException(
            "WinRT async operation failed with HRESULT ${errorCode.value}",
        )
      }
    }
  } catch (e: CancellationException) {
    cancel()
    throw e
  }
}
