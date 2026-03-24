package dev.winrt.kom

@JvmInline
value class HString(val value: String)

interface HStringBridge {
    fun create(value: String): HString
    fun toKotlinString(value: HString): String
}

expect object PlatformHStringBridge : HStringBridge
