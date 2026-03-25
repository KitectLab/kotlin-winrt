package dev.winrt.core

@JvmInline
value class WinRtBoolean(val value: Boolean) {
    companion object {
        val TRUE = WinRtBoolean(true)
        val FALSE = WinRtBoolean(false)
    }
}
