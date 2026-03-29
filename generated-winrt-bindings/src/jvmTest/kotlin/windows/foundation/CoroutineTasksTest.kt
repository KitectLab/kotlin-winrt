package windows.foundation

import dev.winrt.core.UInt32
import dev.winrt.core.WinRtDelegateValueKind
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

    @Test
    fun async_action_with_progress_round_trips_through_await() = runBlocking {
        val progressValues = mutableListOf<UInt32>()

        val action = scope.asyncActionWithProgress(
            progressSignature = "u4",
            progressArgumentKind = WinRtDelegateValueKind.UINT32,
        ) { reportProgress ->
            reportProgress(UInt32(1u))
            delay(10)
            reportProgress(UInt32(2u))
        }

        action.await { progressValues += it }

        assertEquals(listOf(UInt32(1u), UInt32(2u)), progressValues)
        assertEquals(AsyncStatus.Completed, action.status)
    }

    @Test
    fun async_operation_with_progress_round_trips_through_await() = runBlocking {
        val progressValues = mutableListOf<UInt32>()

        val operation = scope.asyncOperationWithProgress(
            resultSignature = WinRtTypeSignature.string(),
            progressSignature = "u4",
            progressArgumentKind = WinRtDelegateValueKind.UINT32,
        ) { reportProgress ->
            reportProgress(UInt32(3u))
            delay(10)
            reportProgress(UInt32(4u))
            "done"
        }

        val result = operation.await { progressValues += it }

        assertEquals("done", result)
        assertEquals(listOf(UInt32(3u), UInt32(4u)), progressValues)
        assertEquals(AsyncStatus.Completed, operation.status)
    }
}
