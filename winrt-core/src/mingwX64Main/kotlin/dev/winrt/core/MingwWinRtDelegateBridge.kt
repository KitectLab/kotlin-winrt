package dev.winrt.core

import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid

actual interface WinRtDelegateHandle : AutoCloseable {
    actual val pointer: ComPtr
}

actual object WinRtDelegateBridge {
    actual fun createNoArgUnitDelegate(iid: Guid, invoke: () -> Unit): WinRtDelegateHandle {
        error("WinRtDelegateBridge is not implemented for mingwX64 yet")
    }

    actual fun createObjectArgUnitDelegate(iid: Guid, invoke: (ComPtr) -> Unit): WinRtDelegateHandle {
        error("WinRtDelegateBridge is not implemented for mingwX64 yet")
    }

    actual fun createNoArgBooleanDelegate(iid: Guid, invoke: () -> Boolean): WinRtDelegateHandle {
        error("WinRtDelegateBridge is not implemented for mingwX64 yet")
    }

    actual fun createObjectArgBooleanDelegate(iid: Guid, invoke: (ComPtr) -> Boolean): WinRtDelegateHandle {
        error("WinRtDelegateBridge is not implemented for mingwX64 yet")
    }

    actual fun createInt32ArgUnitDelegate(iid: Guid, invoke: (Int) -> Unit): WinRtDelegateHandle {
        error("WinRtDelegateBridge is not implemented for mingwX64 yet")
    }

    actual fun createStringArgUnitDelegate(iid: Guid, invoke: (String) -> Unit): WinRtDelegateHandle {
        error("WinRtDelegateBridge is not implemented for mingwX64 yet")
    }
}
