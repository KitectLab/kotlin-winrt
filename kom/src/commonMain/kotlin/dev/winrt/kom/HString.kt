package dev.winrt.kom

import kotlin.jvm.JvmInline

@JvmInline
value class HString(val raw: Long) {
    companion object {
        val NULL = HString(0)
    }

    val isNull: Boolean
        get() = raw == 0L
}

interface HStringBridge {
    fun create(value: String): HString
    fun toKotlinString(value: HString): String
    fun release(value: HString)
}

expect object PlatformHStringBridge : HStringBridge
