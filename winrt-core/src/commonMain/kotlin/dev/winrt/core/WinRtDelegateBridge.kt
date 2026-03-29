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
}
