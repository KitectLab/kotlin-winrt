package windows.foundation

import dev.winrt.core.UInt32
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AsyncInfoTest {
    @Test
    fun completed_action_is_completed() {
        val action = AsyncInfo.completedAction()

        assertEquals(AsyncStatus.Completed, action.status)
        action.getResults()
        action.close()
    }

    @Test
    fun canceled_action_throws_cancellation_exception() {
        val action = AsyncInfo.canceledAction()

        assertEquals(AsyncStatus.Canceled, action.status)
        assertFailsWith<kotlinx.coroutines.CancellationException> {
            action.getResults()
        }
        action.close()
    }

    @Test
    fun from_exception_action_propagates_error() {
        val failure = IllegalStateException("boom")
        val action = AsyncInfo.fromException(failure)

        assertEquals(AsyncStatus.Error, action.status)
        assertFailsWith<IllegalStateException> {
            action.getResults()
        }
        action.close()
    }

    @Test
    fun completed_action_with_progress_is_completed() {
        val action = AsyncInfo.completedActionWithProgress(AsyncProgressTypes.string)

        assertEquals(AsyncStatus.Completed, action.status)
        action.getResults()
        action.close()
    }

    @Test
    fun canceled_action_with_progress_throws_cancellation_exception() {
        val action = AsyncInfo.canceledActionWithProgress(AsyncProgressTypes.string)

        assertEquals(AsyncStatus.Canceled, action.status)
        assertFailsWith<kotlinx.coroutines.CancellationException> {
            action.getResults()
        }
        action.close()
    }

    @Test
    fun from_exception_action_with_progress_propagates_error() {
        val failure = IllegalStateException("boom")
        val action = AsyncInfo.fromExceptionWithProgress(AsyncProgressTypes.string, failure)

        assertEquals(AsyncStatus.Error, action.status)
        assertFailsWith<IllegalStateException> {
            action.getResults()
        }
        action.close()
    }

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
