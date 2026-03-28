package dev.winrt.core

import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.HResult

actual interface WinRtDelegateHandle : AutoCloseable {
    actual val pointer: ComPtr
}

actual object WinRtDelegateBridge {
    actual fun createNoArgUnitDelegate(iid: Guid, invoke: () -> Unit): WinRtDelegateHandle {
        val delegate = JvmWinRtNoArgDelegate.create(iid) {
            invoke()
            HResult(0)
        }
        return object : WinRtDelegateHandle {
            override val pointer: ComPtr = delegate.pointer

            override fun close() {
                delegate.close()
            }
        }
    }

    actual fun createObjectArgUnitDelegate(iid: Guid, invoke: (ComPtr) -> Unit): WinRtDelegateHandle {
        val delegate = JvmWinRtObjectArgDelegate.create(iid) { value ->
            invoke(value)
            HResult(0)
        }
        return object : WinRtDelegateHandle {
            override val pointer: ComPtr = delegate.pointer

            override fun close() {
                delegate.close()
            }
        }
    }
}
