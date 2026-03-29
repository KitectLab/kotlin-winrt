package windows.foundation

import dev.winrt.core.UInt32
import dev.winrt.core.WinRtDelegateValueKind
import dev.winrt.core.WinRtTypeSignature
import dev.winrt.kom.ComPtr
import dev.winrt.kom.HResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IAsyncOperationWithProgressAwaitTest {
    @Test
    fun await_reports_progress_and_returns_result_without_polling() = runBlocking {
        val operation = TestAsyncOperationWithProgress(statusState = AsyncStatus.Started, result = "done")
        val observedProgress = mutableListOf<UInt>()

        launch {
            delay(10)
            operation.emitProgress(25u)
            operation.emitProgress(50u)
            operation.complete()
        }

        val result = withTimeout(1_000) {
            operation.await { observedProgress += it }
        }

        assertEquals("done", result)
        assertEquals(listOf(25u, 50u), observedProgress)
        assertTrue(operation.statusReadCount <= 2)
    }

    private class TestAsyncOperationWithProgress(
        var statusState: AsyncStatus,
        private val result: String,
        private val error: HResult = HResult(0),
    ) : IAsyncOperationWithProgress<String, UInt>(
        pointer = ComPtr.NULL,
        resultSignature = WinRtTypeSignature.string(),
        progressSignature = "u4",
        progressArgumentKind = WinRtDelegateValueKind.UINT32,
        decodeProgress = { it as UInt },
    ) {
        var statusReadCount = 0
        private var progressHandler: AsyncOperationProgressHandler<String, UInt>? = null
        private var completedHandler: AsyncOperationWithProgressCompletedHandler<String, UInt>? = null

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

        override fun put_Progress(handler: AsyncOperationProgressHandler<String, UInt>) {
            progressHandler = handler
        }

        override fun put_Completed(handler: AsyncOperationWithProgressCompletedHandler<String, UInt>) {
            completedHandler = handler
        }

        override fun getResults(): String = result

        fun emitProgress(value: UInt) {
            progressHandler?.invoke(pointer, value)
        }

        fun complete() {
            statusState = AsyncStatus.Completed
            completedHandler?.invoke(pointer, AsyncStatus.Completed)
        }
    }
}
