package windows.foundation

import dev.winrt.core.UInt32
import dev.winrt.core.WinRtDelegateValueKind
import dev.winrt.kom.ComPtr
import dev.winrt.kom.HResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IAsyncActionWithProgressAwaitTest {
    @Test
    fun await_reports_progress_and_completes_without_polling() = runBlocking {
        val action = TestAsyncActionWithProgress(statusState = AsyncStatus.Started)
        val observedProgress = mutableListOf<UInt>()

        launch {
            delay(10)
            action.emitProgress(10u)
            action.emitProgress(20u)
            action.complete()
        }

        withTimeout(1_000) {
            action.await { observedProgress += it }
        }

        assertEquals(listOf(10u, 20u), observedProgress)
        assertTrue(action.resultsCalled)
        assertTrue(action.statusReadCount <= 2)
    }

    private class TestAsyncActionWithProgress(
        var statusState: AsyncStatus,
        private val error: HResult = HResult(0),
    ) : IAsyncActionWithProgress<UInt>(
        pointer = ComPtr.NULL,
        progressSignature = "u4",
        progressArgumentKind = WinRtDelegateValueKind.UINT32,
        decodeProgress = { it as UInt },
    ) {
        var resultsCalled = false
        var statusReadCount = 0
        private var progressHandler: AsyncActionProgressHandler<UInt>? = null
        private var completedHandler: AsyncActionWithProgressCompletedHandler<UInt>? = null

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

        override fun put_Progress(handler: AsyncActionProgressHandler<UInt>) {
            progressHandler = handler
        }

        override fun put_Completed(handler: AsyncActionWithProgressCompletedHandler<UInt>) {
            completedHandler = handler
        }

        override fun getResults() {
            resultsCalled = true
        }

        fun emitProgress(value: UInt) {
            progressHandler?.invoke(pointer, value)
        }

        fun complete() {
            statusState = AsyncStatus.Completed
            completedHandler?.invoke(pointer, AsyncStatus.Completed)
        }
    }
}
