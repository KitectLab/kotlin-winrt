package dev.winrt.core

import dev.winrt.kom.ComPtr

internal actual object WinRtProjectedObjectAuthoringBridge {
    actual fun createPointerOrNull(
        value: Any,
        projectionTypeKey: String,
        signature: String,
    ): ComPtr? = null
}
