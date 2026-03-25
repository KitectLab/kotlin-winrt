package dev.winrt.kom

@JvmInline
value class HResult(val value: Int) {
    val isSuccess: Boolean
        get() = value >= 0

    fun requireSuccess(operation: String) {
        if (!isSuccess) {
            throw KomException("$operation failed with HRESULT=0x${value.toUInt().toString(16)}")
        }
    }

    companion object {
        val OK = HResult(0)
    }
}
