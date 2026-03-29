package windows.foundation

import dev.winrt.core.Int32
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

class IAsyncActionAwaitTest {
    @Test
    fun await_completes_and_reads_results() = runBlocking {
        val action = TestAsyncAction(statusState = AsyncStatus.Completed)

        action.await()

        assertTrue(action.resultsCalled)
    }

    @Test
    fun await_polls_until_completed() = runBlocking {
        val action = TestAsyncAction(statusState = AsyncStatus.Started)

        launch {
            delay(10)
            action.statusState = AsyncStatus.Completed
        }

        action.await()

        assertTrue(action.resultsCalled)
    }

    @Test(expected = CancellationException::class)
    fun await_throws_on_canceled_status() = runBlocking {
        val action = TestAsyncAction(statusState = AsyncStatus.Canceled)
        action.await()
    }

    @Test
    fun await_throws_error_with_hresult() = runBlocking {
        val action = TestAsyncAction(statusState = AsyncStatus.Error, error = HResult(42))

        val error = runCatching { action.await() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("WinRT async action failed with HRESULT 42", error?.message)
    }

    private class TestAsyncAction(
        var statusState: AsyncStatus,
        private val error: HResult = HResult(0),
    ) : IAsyncAction(ComPtr.NULL) {
        var resultsCalled: Boolean = false
        var cancelCalled: Boolean = false

        override val id: UInt32
            get() = UInt32(1u)

        override val status: AsyncStatus
            get() = statusState

        override val errorCode: HResult
            get() = error

        override fun get_Id(): UInt32 = id

        override fun get_Status(): AsyncStatus = status

        override fun get_ErrorCode(): HResult = errorCode

        override fun cancel() {
            cancelCalled = true
        }

        override fun close() = Unit

        override fun put_Completed(handler: AsyncActionCompletedHandler) = Unit

        override fun get_Completed(): AsyncActionCompletedHandler = AsyncActionCompletedHandler(ComPtr.NULL)

        override fun getResults() {
            resultsCalled = true
        }
    }
}
