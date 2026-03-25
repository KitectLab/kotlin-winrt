package dev.winrt.kom

internal fun hexByte(value: Byte): String {
    val digits = "0123456789abcdef"
    val intValue = value.toInt() and 0xff
    return buildString(2) {
        append(digits[intValue ushr 4])
        append(digits[intValue and 0x0f])
    }
}

internal fun hexInt(value: Int, width: Int): String {
    val digits = "0123456789abcdef"
    return buildString(width) {
        for (index in width - 1 downTo 0) {
            val shift = index * 4
            append(digits[(value ushr shift) and 0x0f])
        }
    }
}

internal fun hexShort(value: Short): String = hexInt(value.toInt() and 0xffff, 4)
