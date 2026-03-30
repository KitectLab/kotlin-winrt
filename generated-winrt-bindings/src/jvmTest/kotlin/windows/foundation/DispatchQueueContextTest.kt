package windows.foundation

import dev.winrt.core.UInt32
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DispatchQueueContextTest {
    @Test
    fun dispatch_queue_exposes_coroutine_context_wrapper() {
        val dispatched = mutableListOf<String>()
        val queue = DispatchQueue { block ->
            dispatched += "dispatch"
            block()
            true
        }

        val dispatcher = queue.asCoroutineDispatcher()
        val context = queue.asCoroutineContext()

        assertTrue(dispatcher is DispatchQueueCoroutineContext)
        assertTrue(context is DispatchQueueCoroutineContext)
        assertSame(queue, dispatcher.queue)
        assertSame(queue, context.queue)
    }

    @Test
    fun dispatch_queue_dispatcher_rejects_failed_enqueue() {
        val queue = DispatchQueue { false }
        val dispatcher = queue.asCoroutineDispatcher()

        assertFailsWith<IllegalStateException> {
            dispatcher.dispatch(EmptyCoroutineContext, Runnable { })
        }
    }

    @Test
    fun async_operation_uses_dispatch_queue_context() = runBlocking {
        val dispatches = mutableListOf<String>()
        val queue = DispatchQueue { block ->
            dispatches += "dispatch"
            block()
            true
        }
        val scope = CoroutineScope(Job())

        try {
            val operation = scope.asyncOperation(AsyncResultTypes.uint32, queue) {
                delay(1)
                UInt32(9u)
            }

            val result = operation.await()

            assertEquals(UInt32(9u), result)
            assertTrue(dispatches.isNotEmpty())
        } finally {
            scope.coroutineContext[Job]?.cancel()
        }
    }
}
