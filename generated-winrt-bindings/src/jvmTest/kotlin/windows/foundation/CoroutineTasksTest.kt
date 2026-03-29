package windows.foundation

import dev.winrt.core.WinRtTypeSignature
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class CoroutineTasksTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @AfterTest
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun async_action_round_trips_through_await() = runBlocking {
        var ran = false

        val action = scope.asyncAction {
            delay(10)
            ran = true
        }

        action.await()

        assertTrue(ran)
        assertEquals(AsyncStatus.Completed, action.status)
    }

    @Test
    fun async_operation_round_trips_through_await() = runBlocking {
        val operation = scope.asyncOperation(WinRtTypeSignature.string()) {
            delay(10)
            "done"
        }

        val result = operation.await()

        assertEquals("done", result)
        assertEquals(AsyncStatus.Completed, operation.status)
    }

    @Test
    fun canceling_async_action_cancels_coroutine() = runBlocking {
        val action = scope.asyncAction {
            delay(10_000)
        }

        action.cancel()

        try {
            action.await()
            fail("Expected await() to throw CancellationException")
        } catch (_: CancellationException) {
        }
        assertEquals(AsyncStatus.Canceled, action.status)
    }

    @Test
    fun completed_handler_runs_even_when_set_after_operation_finishes() = runBlocking {
        val operation = scope.asyncOperation(WinRtTypeSignature.string()) { "ready" }

        operation.await()

        var callbackStatus: AsyncStatus? = null
        operation.completed = AsyncOperationCompletedHandler.create(WinRtTypeSignature.string()) { _, status ->
            callbackStatus = status
        }

        assertEquals(AsyncStatus.Completed, callbackStatus)
    }
}
