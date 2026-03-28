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

    actual fun createInt32ArgBooleanDelegate(iid: Guid, invoke: (Int) -> Boolean): WinRtDelegateHandle {
        error("WinRtDelegateBridge is not implemented for mingwX64 yet")
    }

    actual fun createStringArgUnitDelegate(iid: Guid, invoke: (String) -> Unit): WinRtDelegateHandle {
        error("WinRtDelegateBridge is not implemented for mingwX64 yet")
    }

    actual fun createStringArgBooleanDelegate(iid: Guid, invoke: (String) -> Boolean): WinRtDelegateHandle {
        error("WinRtDelegateBridge is not implemented for mingwX64 yet")
    }

    actual fun createUInt32ArgUnitDelegate(iid: Guid, invoke: (UInt) -> Unit): WinRtDelegateHandle {
        error("WinRtDelegateBridge is not implemented for mingwX64 yet")
    }

    actual fun createUInt32ArgBooleanDelegate(iid: Guid, invoke: (UInt) -> Boolean): WinRtDelegateHandle {
        error("WinRtDelegateBridge is not implemented for mingwX64 yet")
    }

    actual fun createBooleanArgUnitDelegate(iid: Guid, invoke: (Boolean) -> Unit): WinRtDelegateHandle {
        error("WinRtDelegateBridge is not implemented for mingwX64 yet")
    }

    actual fun createInt64ArgUnitDelegate(iid: Guid, invoke: (Long) -> Unit): WinRtDelegateHandle {
        error("WinRtDelegateBridge is not implemented for mingwX64 yet")
    }

    actual fun createInt64ArgBooleanDelegate(iid: Guid, invoke: (Long) -> Boolean): WinRtDelegateHandle {
        error("WinRtDelegateBridge is not implemented for mingwX64 yet")
    }

    actual fun createUInt64ArgUnitDelegate(iid: Guid, invoke: (ULong) -> Unit): WinRtDelegateHandle {
        error("WinRtDelegateBridge is not implemented for mingwX64 yet")
    }

    actual fun createFloat32ArgUnitDelegate(iid: Guid, invoke: (Float) -> Unit): WinRtDelegateHandle {
        error("WinRtDelegateBridge is not implemented for mingwX64 yet")
    }

    actual fun createFloat64ArgUnitDelegate(iid: Guid, invoke: (Double) -> Unit): WinRtDelegateHandle {
        error("WinRtDelegateBridge is not implemented for mingwX64 yet")
    }
}
