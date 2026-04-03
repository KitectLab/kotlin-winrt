package windows.foundation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking

class AsyncAwaitDeferredTest {
    @Test
    fun async_operation_as_deferred_returns_value() {
        runBlocking {
        val operation = AsyncInfo.fromResult(AsyncResultTypes.string, "ready")

        val deferred = operation.asDeferred()

        assertEquals("ready", deferred.await())
        }
    }

    @Test
    fun async_action_as_deferred_completes() {
        runBlocking {
            val deferred = AsyncInfo.completedAction().asDeferred()

            deferred.await()
        }
    }

    @Test
    fun async_operation_as_deferred_propagates_error() {
        runBlocking {
            val deferred = AsyncInfo.fromException(AsyncResultTypes.string, IllegalStateException("boom")).asDeferred()

            assertFailsWith<IllegalStateException> {
                deferred.await()
            }
        }
    }

    @Test
    fun async_operation_as_deferred_propagates_cancellation() {
        runBlocking {
            val deferred = AsyncInfo.canceledOperation(AsyncResultTypes.string).asDeferred()

            assertFailsWith<CancellationException> {
                deferred.await()
            }
        }
    }
}
