package dev.winrt.winmd.plugin

internal class BlobReader(private val bytes: ByteArray) {
    private var cursor: Int = 0

    fun readByte(): Int = bytes[cursor++].toInt() and 0xFF

    fun readCompressedUInt(): Int {
        val first = readByte()
        return when {
            first and 0x80 == 0 -> first
            first and 0xC0 == 0x80 -> ((first and 0x3F) shl 8) or readByte()
            else -> ((first and 0x1F) shl 24) or (readByte() shl 16) or (readByte() shl 8) or readByte()
        }
    }
}
