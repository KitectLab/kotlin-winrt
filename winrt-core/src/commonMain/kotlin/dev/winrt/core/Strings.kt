package dev.winrt.core

import dev.winrt.kom.HString

object WinRtStrings {
    fun fromKotlin(value: String): HString = HString.fromKotlin(value)
    fun toKotlin(value: HString): String = value.toKotlinString()
    fun release(value: HString) {
        value.close()
    }
}
