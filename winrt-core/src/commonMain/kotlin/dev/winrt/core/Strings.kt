package dev.winrt.core

import dev.winrt.kom.HString
import dev.winrt.kom.PlatformHStringBridge

object WinRtStrings {
    fun fromKotlin(value: String): HString = PlatformHStringBridge.create(value)
    fun toKotlin(value: HString): String = PlatformHStringBridge.toKotlinString(value)
    fun release(value: HString) {
        PlatformHStringBridge.release(value)
    }
}
