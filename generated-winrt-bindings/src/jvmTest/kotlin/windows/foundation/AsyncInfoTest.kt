package windows.foundation

import dev.winrt.core.UInt32
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AsyncInfoTest {
    @Test
    fun from_result_returns_completed_async_operation() {
        val operation = AsyncInfo.fromResult(AsyncResultTypes.string, "ready")

        assertEquals(AsyncStatus.Completed, operation.status)
        assertEquals("ready", operation.getResults())
    }

    @Test
    fun from_result_with_progress_returns_completed_async_operation() {
        val operation = AsyncInfo.fromResultWithProgress(
            AsyncResultTypes.uint32,
            AsyncProgressTypes.string,
            UInt32(42u),
        )

        assertEquals(AsyncStatus.Completed, operation.status)
        assertEquals(UInt32(42u), operation.getResults())
    }

    @Test
    fun canceled_operation_throws_cancellation_exception() {
        val operation = AsyncInfo.canceledOperation(AsyncResultTypes.int32)

        assertEquals(AsyncStatus.Canceled, operation.status)
        assertFailsWith<kotlinx.coroutines.CancellationException> {
            operation.getResults()
        }
    }

    @Test
    fun from_exception_propagates_error() {
        val failure = IllegalStateException("boom")
        val operation = AsyncInfo.fromException(AsyncResultTypes.string, failure)

        assertEquals(AsyncStatus.Error, operation.status)
        assertFailsWith<IllegalStateException> {
            operation.getResults()
        }
    }
}
