package dev.winrt.core

import dev.winrt.kom.ComPtr

internal expect object WinRtProjectedObjectAuthoringBridge {
    fun createPointerOrNull(
        value: Any,
        projectionTypeKey: String,
        signature: String,
    ): ComPtr?
}
