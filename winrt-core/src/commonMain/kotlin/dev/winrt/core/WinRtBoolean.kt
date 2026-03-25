package dev.winrt.core

import kotlin.jvm.JvmInline

@JvmInline
value class WinRtBoolean(val value: Boolean) {
    companion object {
        val TRUE = WinRtBoolean(true)
        val FALSE = WinRtBoolean(false)
    }
}
