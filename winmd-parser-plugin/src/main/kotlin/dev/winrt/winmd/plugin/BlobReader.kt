package dev.winrt.winmd.plugin

internal class BlobReader(private val bytes: ByteArray) {
    private var cursor: Int = 0

    fun readByte(): Int = bytes[cursor++].toInt() and 0xFF

    fun readUInt16(): Int = readByte() or (readByte() shl 8)

    fun readInt32(): Int = readByte() or (readByte() shl 8) or (readByte() shl 16) or (readByte() shl 24)

    fun hasRemaining(): Boolean = cursor < bytes.size

    fun readCompressedUInt(): Int {
        val first = readByte()
        return when {
            first and 0x80 == 0 -> first
            first and 0xC0 == 0x80 -> ((first and 0x3F) shl 8) or readByte()
            else -> ((first and 0x1F) shl 24) or (readByte() shl 16) or (readByte() shl 8) or readByte()
        }
    }

    fun readSerializedString(): String? {
        if (!hasRemaining()) {
            return null
        }
        val marker = bytes[cursor].toInt() and 0xFF
        if (marker == 0xFF) {
            cursor++
            return null
        }
        val length = readCompressedUInt()
        val value = bytes.copyOfRange(cursor, cursor + length)
        cursor += length
        return value.toString(Charsets.UTF_8)
    }
}
