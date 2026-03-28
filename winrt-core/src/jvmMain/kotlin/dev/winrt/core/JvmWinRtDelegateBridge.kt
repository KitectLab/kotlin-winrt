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

    actual fun createNoArgBooleanDelegate(iid: Guid, invoke: () -> Boolean): WinRtDelegateHandle {
        val delegate = JvmWinRtNoArgBooleanDelegate.create(iid, invoke)
        return object : WinRtDelegateHandle {
            override val pointer: ComPtr = delegate.pointer

            override fun close() {
                delegate.close()
            }
        }
    }

    actual fun createObjectArgBooleanDelegate(iid: Guid, invoke: (ComPtr) -> Boolean): WinRtDelegateHandle {
        val delegate = JvmWinRtObjectArgBooleanDelegate.create(iid, invoke)
        return object : WinRtDelegateHandle {
            override val pointer: ComPtr = delegate.pointer

            override fun close() {
                delegate.close()
            }
        }
    }

    actual fun createInt32ArgUnitDelegate(iid: Guid, invoke: (Int) -> Unit): WinRtDelegateHandle {
        val delegate = JvmWinRtInt32ArgDelegate.create(iid) { value ->
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

    actual fun createInt32ArgBooleanDelegate(iid: Guid, invoke: (Int) -> Boolean): WinRtDelegateHandle {
        val delegate = JvmWinRtInt32ArgBooleanDelegate.create(iid, invoke)
        return object : WinRtDelegateHandle {
            override val pointer: ComPtr = delegate.pointer

            override fun close() {
                delegate.close()
            }
        }
    }

    actual fun createStringArgUnitDelegate(iid: Guid, invoke: (String) -> Unit): WinRtDelegateHandle {
        val delegate = JvmWinRtStringArgDelegate.create(iid) { value ->
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

    actual fun createUInt32ArgUnitDelegate(iid: Guid, invoke: (UInt) -> Unit): WinRtDelegateHandle {
        val delegate = JvmWinRtUInt32ArgDelegate.create(iid) { value ->
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

    actual fun createBooleanArgUnitDelegate(iid: Guid, invoke: (Boolean) -> Unit): WinRtDelegateHandle {
        val delegate = JvmWinRtBooleanArgDelegate.create(iid) { value ->
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

    actual fun createInt64ArgUnitDelegate(iid: Guid, invoke: (Long) -> Unit): WinRtDelegateHandle {
        val delegate = JvmWinRtInt64ArgDelegate.create(iid) { value ->
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

    actual fun createUInt64ArgUnitDelegate(iid: Guid, invoke: (ULong) -> Unit): WinRtDelegateHandle {
        val delegate = JvmWinRtUInt64ArgDelegate.create(iid) { value ->
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

    actual fun createFloat32ArgUnitDelegate(iid: Guid, invoke: (Float) -> Unit): WinRtDelegateHandle {
        val delegate = JvmWinRtFloat32ArgDelegate.create(iid) { value ->
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

    actual fun createFloat64ArgUnitDelegate(iid: Guid, invoke: (Double) -> Unit): WinRtDelegateHandle {
        val delegate = JvmWinRtFloat64ArgDelegate.create(iid) { value ->
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
