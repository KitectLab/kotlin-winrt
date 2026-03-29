package windows.foundation

import dev.winrt.core.UInt32
import dev.winrt.kom.ComPtr
import dev.winrt.kom.HResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    fun await_polls_until_completed() = runBlocking {
        val operation = TestAsyncOperation(statusState = AsyncStatus.Started, result = "done")

        launch {
            delay(10)
            operation.statusState = AsyncStatus.Completed
        }

        val result = operation.await()

        assertEquals("done", result)
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
    ) : IAsyncOperation<String>(ComPtr.NULL) {
        override val id: UInt32
            get() = UInt32(1u)

        override val status: AsyncStatus
            get() = statusState

        override val errorCode: HResult
            get() = error

        override fun cancel() = Unit

        override fun close() = Unit

        override fun getResults(): String = result
    }
}
