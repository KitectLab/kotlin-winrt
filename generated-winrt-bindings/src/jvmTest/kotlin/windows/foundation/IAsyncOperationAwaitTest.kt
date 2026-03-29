package windows.foundation

import dev.winrt.core.UInt32
import dev.winrt.core.WinRtTypeSignature
import dev.winrt.kom.ComPtr
import dev.winrt.kom.HResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IAsyncOperationAwaitTest {
    @Test
    fun await_returns_result() = runBlocking {
        val operation = TestAsyncOperation(statusState = AsyncStatus.Completed, result = "done")

        val result = operation.await()

        assertEquals("done", result)
    }

    @Test
    fun await_resumes_from_completed_handler_without_polling() = runBlocking {
        val operation = TestAsyncOperation(statusState = AsyncStatus.Started, result = "done")

        launch {
            delay(10)
            operation.complete()
        }

        val result = withTimeout(1_000) {
            operation.await()
        }

        assertEquals("done", result)
        assertTrue(operation.statusReadCount <= 2)
    }

    @Test(expected = CancellationException::class)
    fun await_throws_on_canceled_status() {
        runBlocking {
            val operation = TestAsyncOperation(statusState = AsyncStatus.Canceled, result = "unused")
            operation.await()
        }
    }

    @Test
    fun await_throws_error_with_hresult() = runBlocking {
        val operation = TestAsyncOperation(statusState = AsyncStatus.Error, result = "unused", error = HResult(55))

        val error = runCatching { operation.await() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("WinRT async operation failed with HRESULT 55", error?.message)
    }

    private class TestAsyncOperation(
        var statusState: AsyncStatus,
        private val result: String,
        private val error: HResult = HResult(0),
    ) : IAsyncOperation<String>(ComPtr.NULL, WinRtTypeSignature.string()) {
        var statusReadCount: Int = 0
        private var completedHandler: AsyncOperationCompletedHandler<String>? = null

        override val id: UInt32
            get() = UInt32(1u)

        override val status: AsyncStatus
            get() {
                statusReadCount += 1
                return statusState
            }

        override val errorCode: HResult
            get() = error

        override fun cancel() = Unit

        override fun close() = Unit

        override fun put_Completed(handler: AsyncOperationCompletedHandler<String>) {
            completedHandler = handler
        }

        override fun getResults(): String = result

        fun complete() {
            statusState = AsyncStatus.Completed
            completedHandler?.invoke(pointer, AsyncStatus.Completed)
        }
    }
}
