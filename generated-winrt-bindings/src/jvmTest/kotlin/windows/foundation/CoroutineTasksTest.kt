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
        val operation = scope.asyncOperation(AsyncResultTypes.string) {
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
        val operation = scope.asyncOperation(AsyncResultTypes.string) { "ready" }

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
            progressType = AsyncProgressTypes.uint32,
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
            resultType = AsyncResultTypes.string,
            progressType = AsyncProgressTypes.uint32,
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

    @Test
    fun progress_type_overload_supports_string_progress() = runBlocking {
        val progressValues = mutableListOf<String>()

        val action = scope.asyncActionWithProgress(
            progressType = AsyncProgressTypes.string,
        ) { reportProgress ->
            reportProgress("a")
            delay(10)
            reportProgress("b")
        }

        action.await { progressValues += it }

        assertEquals(listOf("a", "b"), progressValues)
        assertEquals(AsyncStatus.Completed, action.status)
    }

    @Test
    fun canceling_async_action_with_progress_cancels_coroutine() = runBlocking {
        val action = scope.asyncActionWithProgress(
            progressType = AsyncProgressTypes.uint32,
        ) { reportProgress ->
            reportProgress(UInt32(1u))
            delay(10_000)
        }

        action.cancel()

        try {
            action.await { }
            fail("Expected await() to throw CancellationException")
        } catch (_: CancellationException) {
        }
        assertEquals(AsyncStatus.Canceled, action.status)
    }

    @Test
    fun completed_handler_runs_even_when_set_after_progress_operation_finishes() = runBlocking {
        val operation = scope.asyncOperationWithProgress(
            resultType = AsyncResultTypes.string,
            progressType = AsyncProgressTypes.uint32,
        ) { reportProgress ->
            reportProgress(UInt32(1u))
            "ready"
        }

        operation.await { }

        var callbackStatus: AsyncStatus? = null
        operation.completed = AsyncOperationWithProgressCompletedHandler.create(
            resultSignature = WinRtTypeSignature.string(),
            progressSignature = "u4",
        ) { _, status ->
            callbackStatus = status
        }

        assertEquals(AsyncStatus.Completed, callbackStatus)
    }

    @Test
    fun result_type_overload_supports_uint32_results() = runBlocking {
        val operation = scope.asyncOperation(AsyncResultTypes.uint32) {
            delay(10)
            UInt32(7u)
        }

        val result = operation.await()

        assertEquals(UInt32(7u), result)
        assertEquals(AsyncStatus.Completed, operation.status)
    }

    @Test
    fun typed_result_and_progress_overloads_round_trip_together() = runBlocking {
        val progressValues = mutableListOf<String>()

        val operation = scope.asyncOperationWithProgress(
            resultType = AsyncResultTypes.uint32,
            progressType = AsyncProgressTypes.string,
        ) { reportProgress ->
            reportProgress("warmup")
            delay(10)
            reportProgress("done")
            UInt32(9u)
        }

        val result = operation.await { progressValues += it }

        assertEquals(UInt32(9u), result)
        assertEquals(listOf("warmup", "done"), progressValues)
        assertEquals(AsyncStatus.Completed, operation.status)
    }
}
