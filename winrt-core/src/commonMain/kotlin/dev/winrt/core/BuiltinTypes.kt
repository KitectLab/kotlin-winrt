package dev.winrt.core

@JvmInline
value class WinRtBoolean(val value: Boolean) {
    companion object {
        val TRUE = WinRtBoolean(true)
        val FALSE = WinRtBoolean(false)
    }
}

@JvmInline
value class Int32(val value: Int)

@JvmInline
value class UInt32(val value: UInt)

@JvmInline
value class Int64(val value: Long)

@JvmInline
value class UInt64(val value: ULong)

@JvmInline
value class Float32(val value: Float)

@JvmInline
value class Float64(val value: Double)
