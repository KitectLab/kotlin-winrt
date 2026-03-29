package dev.winrt.core

import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid

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
}
