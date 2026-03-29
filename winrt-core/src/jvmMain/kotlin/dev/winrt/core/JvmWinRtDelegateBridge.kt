package dev.winrt.core

import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.HResult

actual interface WinRtDelegateHandle : AutoCloseable {
    actual val pointer: ComPtr
}

actual object WinRtDelegateBridge {
    actual fun createUnitDelegate(
        iid: Guid,
        parameterKinds: List<WinRtDelegateValueKind>,
        invoke: (Array<Any?>) -> Unit,
    ): WinRtDelegateHandle {
        return JvmWinRtUnitResultDelegates.createDelegate(iid, parameterKinds, invoke)
    }

    actual fun createBooleanDelegate(
        iid: Guid,
        parameterKinds: List<WinRtDelegateValueKind>,
        invoke: (Array<Any?>) -> Boolean,
    ): WinRtDelegateHandle {
        return JvmWinRtBooleanResultDelegates.createDelegate(iid, parameterKinds, invoke)
    }

    actual fun createNoArgUnitDelegate(iid: Guid, invoke: () -> Unit): WinRtDelegateHandle {
        return createUnitDelegate(iid, emptyList()) { invoke() }
    }

    actual fun createObjectArgUnitDelegate(iid: Guid, invoke: (ComPtr) -> Unit): WinRtDelegateHandle {
        return createUnitDelegate(iid, listOf(WinRtDelegateValueKind.OBJECT)) { args -> invoke(args.single() as ComPtr) }
    }

    actual fun createNoArgBooleanDelegate(iid: Guid, invoke: () -> Boolean): WinRtDelegateHandle {
        return createBooleanDelegate(iid, emptyList()) { invoke() }
    }

    actual fun createObjectArgBooleanDelegate(iid: Guid, invoke: (ComPtr) -> Boolean): WinRtDelegateHandle {
        return createBooleanDelegate(iid, listOf(WinRtDelegateValueKind.OBJECT)) { args -> invoke(args.single() as ComPtr) }
    }

    actual fun createInt32ArgUnitDelegate(iid: Guid, invoke: (Int) -> Unit): WinRtDelegateHandle {
        return createUnitDelegate(iid, listOf(WinRtDelegateValueKind.INT32)) { args -> invoke(args.single() as Int) }
    }

    actual fun createInt32ArgBooleanDelegate(iid: Guid, invoke: (Int) -> Boolean): WinRtDelegateHandle {
        return createBooleanDelegate(iid, listOf(WinRtDelegateValueKind.INT32)) { args -> invoke(args.single() as Int) }
    }

    actual fun createStringArgUnitDelegate(iid: Guid, invoke: (String) -> Unit): WinRtDelegateHandle {
        return createUnitDelegate(iid, listOf(WinRtDelegateValueKind.STRING)) { args -> invoke(args.single() as String) }
    }

    actual fun createStringArgBooleanDelegate(iid: Guid, invoke: (String) -> Boolean): WinRtDelegateHandle {
        return createBooleanDelegate(iid, listOf(WinRtDelegateValueKind.STRING)) { args -> invoke(args.single() as String) }
    }

    actual fun createUInt32ArgUnitDelegate(iid: Guid, invoke: (UInt) -> Unit): WinRtDelegateHandle {
        return createUnitDelegate(iid, listOf(WinRtDelegateValueKind.UINT32)) { args -> invoke(args.single() as UInt) }
    }

    actual fun createUInt32ArgBooleanDelegate(iid: Guid, invoke: (UInt) -> Boolean): WinRtDelegateHandle {
        return createBooleanDelegate(iid, listOf(WinRtDelegateValueKind.UINT32)) { args -> invoke(args.single() as UInt) }
    }

    actual fun createBooleanArgUnitDelegate(iid: Guid, invoke: (Boolean) -> Unit): WinRtDelegateHandle {
        return createUnitDelegate(iid, listOf(WinRtDelegateValueKind.BOOLEAN)) { args -> invoke(args.single() as Boolean) }
    }

    actual fun createInt64ArgUnitDelegate(iid: Guid, invoke: (Long) -> Unit): WinRtDelegateHandle {
        return createUnitDelegate(iid, listOf(WinRtDelegateValueKind.INT64)) { args -> invoke(args.single() as Long) }
    }

    actual fun createInt64ArgBooleanDelegate(iid: Guid, invoke: (Long) -> Boolean): WinRtDelegateHandle {
        return createBooleanDelegate(iid, listOf(WinRtDelegateValueKind.INT64)) { args -> invoke(args.single() as Long) }
    }

    actual fun createUInt64ArgUnitDelegate(iid: Guid, invoke: (ULong) -> Unit): WinRtDelegateHandle {
        return createUnitDelegate(iid, listOf(WinRtDelegateValueKind.UINT64)) { args -> invoke(args.single() as ULong) }
    }

    actual fun createFloat32ArgUnitDelegate(iid: Guid, invoke: (Float) -> Unit): WinRtDelegateHandle {
        return createUnitDelegate(iid, listOf(WinRtDelegateValueKind.FLOAT32)) { args -> invoke(args.single() as Float) }
    }

    actual fun createFloat64ArgUnitDelegate(iid: Guid, invoke: (Double) -> Unit): WinRtDelegateHandle {
        return createUnitDelegate(iid, listOf(WinRtDelegateValueKind.FLOAT64)) { args -> invoke(args.single() as Double) }
    }
}
