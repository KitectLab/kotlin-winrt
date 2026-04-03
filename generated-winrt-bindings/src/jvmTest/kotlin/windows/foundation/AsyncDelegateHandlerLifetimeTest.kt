package windows.foundation

import dev.winrt.core.UInt32
import dev.winrt.core.WinRtTypeSignature
import dev.winrt.kom.ComPtr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AsyncDelegateHandlerLifetimeTest {
    @Test
    fun action_completed_handler_drops_local_callback_when_closed() {
        var invokeCount = 0
        val handler = AsyncActionCompletedHandler.create { _, _ ->
            invokeCount += 1
        }

        handler.close()

        val error = runCatching {
            handler.invoke(IAsyncAction(ComPtr.NULL), AsyncStatus.Completed)
        }.exceptionOrNull()

        assertEquals(0, invokeCount)
        assertTrue(error is IllegalStateException)
    }

    @Test
    fun operation_progress_handler_drops_local_callback_when_closed() {
        val observed = mutableListOf<UInt32>()
        val handler = AsyncOperationProgressHandler.create<UInt32, UInt32>(
            resultSignature = "u4",
            progressSignature = "u4",
            progressArgumentKind = dev.winrt.core.WinRtDelegateValueKind.UINT32,
            decodeProgress = { UInt32(it as UInt) },
        ) { _, progress ->
            observed += progress
        }

        handler.close()

        val error = runCatching {
            handler.invoke(ComPtr.NULL, UInt32(7u))
        }.exceptionOrNull()

        assertTrue(observed.isEmpty())
        assertTrue(error is IllegalStateException)
    }
}
