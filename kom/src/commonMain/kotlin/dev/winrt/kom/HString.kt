package dev.winrt.kom

import kotlin.jvm.JvmInline

@JvmInline
value class HString(val raw: Long) : AutoCloseable {
    companion object {
        val NULL = HString(0)

        fun fromKotlin(value: String): HString = PlatformHStringBridge.create(value)
    }

    val isNull: Boolean
        get() = raw == 0L

    fun toKotlinString(): String = PlatformHStringBridge.toKotlinString(this)

    override fun close() {
        PlatformHStringBridge.release(this)
    }
}

interface HStringBridge {
    fun create(value: String): HString
    fun toKotlinString(value: HString): String
    fun release(value: HString)
}

expect object PlatformHStringBridge : HStringBridge
