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
        return when (parameterKinds.singleOrNull()) {
            null -> JvmWinRtUnitResultDelegates.createNoArg(iid) { invoke(emptyArray()) }
            WinRtDelegateValueKind.OBJECT -> JvmWinRtUnitResultDelegates.createObjectArg(iid) { arg -> invoke(arrayOf(arg)) }
            WinRtDelegateValueKind.INT32 -> JvmWinRtUnitResultDelegates.createInt32Arg(iid, decode = { it }, invoke = { value -> invoke(arrayOf(value)) })
            WinRtDelegateValueKind.UINT32 -> JvmWinRtUnitResultDelegates.createInt32Arg(iid, decode = Int::toUInt, invoke = { value -> invoke(arrayOf(value)) })
            WinRtDelegateValueKind.BOOLEAN -> JvmWinRtUnitResultDelegates.createInt32Arg(iid, decode = { it != 0 }, invoke = { value -> invoke(arrayOf(value)) })
            WinRtDelegateValueKind.INT64 -> JvmWinRtUnitResultDelegates.createInt64Arg(iid, decode = { it }, invoke = { value -> invoke(arrayOf(value)) })
            WinRtDelegateValueKind.UINT64 -> JvmWinRtUnitResultDelegates.createInt64Arg(iid, decode = Long::toULong, invoke = { value -> invoke(arrayOf(value)) })
            WinRtDelegateValueKind.FLOAT32 -> JvmWinRtUnitResultDelegates.createFloat32Arg(iid, decode = { it }, invoke = { value -> invoke(arrayOf(value)) })
            WinRtDelegateValueKind.FLOAT64 -> JvmWinRtUnitResultDelegates.createFloat64Arg(iid, decode = { it }, invoke = { value -> invoke(arrayOf(value)) })
            WinRtDelegateValueKind.STRING -> JvmWinRtUnitResultDelegates.createStringArg(iid) { value -> invoke(arrayOf(value)) }
        }
    }

    actual fun createBooleanDelegate(
        iid: Guid,
        parameterKinds: List<WinRtDelegateValueKind>,
        invoke: (Array<Any?>) -> Boolean,
    ): WinRtDelegateHandle {
        return when (parameterKinds.singleOrNull()) {
            null -> JvmWinRtBooleanResultDelegates.createNoArg(iid) { invoke(emptyArray()) }
            WinRtDelegateValueKind.OBJECT -> JvmWinRtBooleanResultDelegates.createObjectArg(iid) { arg -> invoke(arrayOf(arg)) }
            WinRtDelegateValueKind.INT32 -> JvmWinRtBooleanResultDelegates.createInt32Arg(iid, decode = { it }, invoke = { value -> invoke(arrayOf(value)) })
            WinRtDelegateValueKind.UINT32 -> JvmWinRtBooleanResultDelegates.createInt32Arg(iid, decode = Int::toUInt, invoke = { value -> invoke(arrayOf(value)) })
            WinRtDelegateValueKind.BOOLEAN -> JvmWinRtBooleanResultDelegates.createInt32Arg(iid, decode = { it != 0 }, invoke = { value -> invoke(arrayOf(value)) })
            WinRtDelegateValueKind.INT64 -> JvmWinRtBooleanResultDelegates.createInt64Arg(iid, decode = { it }, invoke = { value -> invoke(arrayOf(value)) })
            WinRtDelegateValueKind.UINT64 -> JvmWinRtBooleanResultDelegates.createInt64Arg(iid, decode = Long::toULong, invoke = { value -> invoke(arrayOf(value)) })
            WinRtDelegateValueKind.FLOAT32 -> JvmWinRtBooleanResultDelegates.createFloat32Arg(iid, decode = { it }, invoke = { value -> invoke(arrayOf(value)) })
            WinRtDelegateValueKind.FLOAT64 -> JvmWinRtBooleanResultDelegates.createFloat64Arg(iid, decode = { it }, invoke = { value -> invoke(arrayOf(value)) })
            WinRtDelegateValueKind.STRING -> JvmWinRtBooleanResultDelegates.createStringArg(iid) { value -> invoke(arrayOf(value)) }
        }
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
