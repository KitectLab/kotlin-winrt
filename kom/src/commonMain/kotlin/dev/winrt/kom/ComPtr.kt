package dev.winrt.kom

@JvmInline
value class ComPtr(val value: AbiIntPtr) {
    companion object {
        val NULL = ComPtr(AbiIntPtr.NULL)
    }

    val isNull: Boolean
        get() = value.isNull
}
