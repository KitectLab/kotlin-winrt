package dev.winrt.kom

enum class ComStructFieldKind(
    val sizeBytes: Int,
    val alignmentBytes: Int = sizeBytes.coerceAtMost(8),
) {
    BOOLEAN(1),
    INT8(1),
    UINT8(1),
    INT16(2),
    UINT16(2),
    CHAR16(2),
    INT32(4),
    UINT32(4),
    INT64(8),
    UINT64(8),
    FLOAT32(4),
    FLOAT64(8),
    GUID(16, 4),
}

data class ComStructLayout(
    val fields: List<ComStructFieldKind>,
) {
    init {
        require(fields.isNotEmpty()) { "COM struct layouts must declare at least one field" }
    }

    val fieldOffsets: List<Int> by lazy {
        buildList {
            var offset = 0
            fields.forEach { field ->
                offset = align(offset, field.alignmentBytes)
                add(offset)
                offset += field.sizeBytes
            }
        }
    }

    val byteSize: Int by lazy {
        val maxAlignment = fields.maxOf(ComStructFieldKind::alignmentBytes)
        val endOffset = fieldOffsets.last() + fields.last().sizeBytes
        align(endOffset, maxAlignment)
    }

    companion object {
        fun of(vararg fields: ComStructFieldKind): ComStructLayout = ComStructLayout(fields.toList())
    }
}

data class ComStructValue(
    val layout: ComStructLayout,
    val bytes: ByteArray,
) {
    init {
        require(bytes.size == layout.byteSize) {
            "Expected ${layout.byteSize} struct bytes for $layout, got ${bytes.size}"
        }
    }
}

private fun align(value: Int, alignment: Int): Int {
    val remainder = value % alignment
    return if (remainder == 0) value else value + alignment - remainder
}
