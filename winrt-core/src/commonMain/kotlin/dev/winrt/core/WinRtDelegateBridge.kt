package dev.winrt.core

import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid

expect interface WinRtDelegateHandle : AutoCloseable {
    val pointer: ComPtr
}

expect object WinRtDelegateBridge {
    fun createNoArgUnitDelegate(iid: Guid, invoke: () -> Unit): WinRtDelegateHandle

    fun createObjectArgUnitDelegate(iid: Guid, invoke: (ComPtr) -> Unit): WinRtDelegateHandle
}
