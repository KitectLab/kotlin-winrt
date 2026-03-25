package dev.winrt.kom

@JvmInline
value class NativeBoolean(val rawValue: Int) {
    fun toBoolean(): Boolean = rawValue != 0

    companion object {
        val TRUE = NativeBoolean(1)
        val FALSE = NativeBoolean(0)
    }
}
