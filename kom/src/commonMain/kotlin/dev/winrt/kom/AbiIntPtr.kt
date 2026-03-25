package dev.winrt.kom

@JvmInline
value class AbiIntPtr(val rawValue: Long) {
    companion object {
        val NULL = AbiIntPtr(0)
    }

    val isNull: Boolean
        get() = rawValue == 0L
}
