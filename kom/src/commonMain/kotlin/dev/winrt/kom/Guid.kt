package dev.winrt.kom

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
        val suffix = data4.joinToString("") { hexByte(it) }
        return buildString(36) {
            append(hexInt(data1, 8))
            append('-')
            append(hexShort(data2))
            append('-')
            append(hexShort(data3))
            append('-')
            append(suffix.take(4))
            append('-')
            append(suffix.drop(4))
        }
    }
}
