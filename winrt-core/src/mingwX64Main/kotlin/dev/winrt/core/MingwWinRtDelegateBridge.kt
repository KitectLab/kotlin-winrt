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
        error("WinRtDelegateBridge is not implemented for mingwX64 yet")
    }

    actual fun createBooleanDelegate(
        iid: Guid,
        parameterKinds: List<WinRtDelegateValueKind>,
        invoke: (Array<Any?>) -> Boolean,
    ): WinRtDelegateHandle {
        error("WinRtDelegateBridge is not implemented for mingwX64 yet")
    }
}
