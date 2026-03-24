package dev.winrt.kom

@JvmInline
value class AbiIntPtr(val rawValue: Long) {
    companion object {
        val NULL = AbiIntPtr(0)
    }

    val isNull: Boolean
        get() = rawValue == 0L
}

@JvmInline
value class ComPtr(val value: AbiIntPtr) {
    companion object {
        val NULL = ComPtr(AbiIntPtr.NULL)
    }

    val isNull: Boolean
        get() = value.isNull
}

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

data class Guid(
    val data1: Int,
    val data2: Short,
    val data3: Short,
    val data4: ByteArray,
) {
    init {
        require(data4.size == 8) { "GUID data4 must contain 8 bytes" }
    }

    override fun toString(): String {
        val suffix = data4.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return "%08x-%04x-%04x-%s-%s".format(
            data1,
            data2.toInt() and 0xffff,
            data3.toInt() and 0xffff,
            suffix.take(4),
            suffix.drop(4),
        )
    }
}

class KomException(message: String) : RuntimeException(message)

object KnownHResults {
    val E_POINTER = HResult(0x80004003.toInt())
    val E_NOINTERFACE = HResult(0x80004002.toInt())
    val RPC_E_CHANGED_MODE = HResult(0x80010106.toInt())
    val S_FALSE = HResult(1)
}
