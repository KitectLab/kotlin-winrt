package dev.winrt.core

import dev.winrt.kom.Guid
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

actual object ParameterizedInterfaceId {
    private val pinterfaceNamespace = guidOf("d57af411-737b-c042-abae-878b1e16adee")

    actual fun createFromSignature(signature: String): Guid {
        val namespaceBytes = guidToRfc4122Bytes(pinterfaceNamespace)
        val payload = namespaceBytes + signature.toByteArray(StandardCharsets.UTF_8)
        val hash = MessageDigest.getInstance("SHA-1").digest(payload).copyOf(16)
        hash[6] = ((hash[6].toInt() and 0x0F) or (5 shl 4)).toByte()
        hash[8] = ((hash[8].toInt() and 0x3F) or 0x80).toByte()
        return rfc4122BytesToGuid(hash)
    }

    private fun guidToRfc4122Bytes(guid: Guid): ByteArray {
        val buffer = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(guid.data1)
        buffer.putShort(guid.data2)
        buffer.putShort(guid.data3)
        buffer.put(guid.data4)
        return buffer.array()
    }

    private fun rfc4122BytesToGuid(bytes: ByteArray): Guid {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        return Guid(
            data1 = buffer.int,
            data2 = buffer.short,
            data3 = buffer.short,
            data4 = ByteArray(8).also(buffer::get),
        )
    }
}
