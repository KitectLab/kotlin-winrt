package dev.winrt.core

import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid

expect interface WinRtDelegateHandle : AutoCloseable {
    val pointer: ComPtr
}

enum class WinRtDelegateValueKind {
    OBJECT,
    INT32,
    UINT32,
    BOOLEAN,
    INT64,
    UINT64,
    FLOAT32,
    FLOAT64,
    STRING,
}

expect object WinRtDelegateBridge {
    fun createUnitDelegate(
        iid: Guid,
        parameterKinds: List<WinRtDelegateValueKind>,
        invoke: (Array<Any?>) -> Unit,
    ): WinRtDelegateHandle

    fun createBooleanDelegate(
        iid: Guid,
        parameterKinds: List<WinRtDelegateValueKind>,
        invoke: (Array<Any?>) -> Boolean,
    ): WinRtDelegateHandle

    fun createNoArgUnitDelegate(iid: Guid, invoke: () -> Unit): WinRtDelegateHandle

    fun createObjectArgUnitDelegate(iid: Guid, invoke: (ComPtr) -> Unit): WinRtDelegateHandle

    fun createNoArgBooleanDelegate(iid: Guid, invoke: () -> Boolean): WinRtDelegateHandle

    fun createObjectArgBooleanDelegate(iid: Guid, invoke: (ComPtr) -> Boolean): WinRtDelegateHandle

    fun createInt32ArgUnitDelegate(iid: Guid, invoke: (Int) -> Unit): WinRtDelegateHandle

    fun createInt32ArgBooleanDelegate(iid: Guid, invoke: (Int) -> Boolean): WinRtDelegateHandle

    fun createStringArgUnitDelegate(iid: Guid, invoke: (String) -> Unit): WinRtDelegateHandle

    fun createStringArgBooleanDelegate(iid: Guid, invoke: (String) -> Boolean): WinRtDelegateHandle

    fun createUInt32ArgUnitDelegate(iid: Guid, invoke: (UInt) -> Unit): WinRtDelegateHandle

    fun createUInt32ArgBooleanDelegate(iid: Guid, invoke: (UInt) -> Boolean): WinRtDelegateHandle

    fun createBooleanArgUnitDelegate(iid: Guid, invoke: (Boolean) -> Unit): WinRtDelegateHandle

    fun createInt64ArgUnitDelegate(iid: Guid, invoke: (Long) -> Unit): WinRtDelegateHandle

    fun createInt64ArgBooleanDelegate(iid: Guid, invoke: (Long) -> Boolean): WinRtDelegateHandle

    fun createUInt64ArgUnitDelegate(iid: Guid, invoke: (ULong) -> Unit): WinRtDelegateHandle

    fun createFloat32ArgUnitDelegate(iid: Guid, invoke: (Float) -> Unit): WinRtDelegateHandle

    fun createFloat64ArgUnitDelegate(iid: Guid, invoke: (Double) -> Unit): WinRtDelegateHandle
}
