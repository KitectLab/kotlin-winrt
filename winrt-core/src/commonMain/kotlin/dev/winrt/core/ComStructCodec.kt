package dev.winrt.core

import dev.winrt.kom.ComStructFieldKind
import dev.winrt.kom.ComStructLayout
import dev.winrt.kom.ComStructValue
import dev.winrt.kom.Guid
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ComStructWriter(
    private val layout: ComStructLayout,
) {
    private val bytes = ByteArray(layout.byteSize)
    private var fieldIndex = 0

    fun writeBoolean(value: Boolean) {
        bytes[consume(ComStructFieldKind.BOOLEAN)] = if (value) 1 else 0
    }

    fun writeByte(value: Byte) {
        bytes[consume(ComStructFieldKind.INT8)] = value
    }

    fun writeUByte(value: UByte) {
        bytes[consume(ComStructFieldKind.UINT8)] = value.toByte()
    }

    fun writeShort(value: Short) {
        writeInt16(consume(ComStructFieldKind.INT16), value.toInt())
    }

    fun writeUShort(value: UShort) {
        writeInt16(consume(ComStructFieldKind.UINT16), value.toInt())
    }

    fun writeChar(value: Char) {
        writeInt16(consume(ComStructFieldKind.CHAR16), value.code)
    }

    fun writeInt(value: Int) {
        writeInt32(consume(ComStructFieldKind.INT32), value)
    }

    fun writeUInt(value: UInt) {
        writeInt32(consume(ComStructFieldKind.UINT32), value.toInt())
    }

    fun writeLong(value: Long) {
        writeInt64(consume(ComStructFieldKind.INT64), value)
    }

    fun writeULong(value: ULong) {
        writeInt64(consume(ComStructFieldKind.UINT64), value.toLong())
    }

    fun writeFloat(value: Float) {
        writeInt32(consume(ComStructFieldKind.FLOAT32), value.toRawBits())
    }

    fun writeDouble(value: Double) {
        writeInt64(consume(ComStructFieldKind.FLOAT64), value.toRawBits())
    }

    fun writeGuid(value: Uuid) {
        val offset = consume(ComStructFieldKind.GUID)
        val guid = guidOf(value.toString())
        writeInt32(offset, guid.data1)
        writeInt16(offset + 4, guid.data2.toInt())
        writeInt16(offset + 6, guid.data3.toInt())
        guid.data4.copyInto(bytes, destinationOffset = offset + 8)
    }

    fun writeStruct(value: ComStructValue) {
        val offset = consumeStruct(value.layout)
        value.bytes.copyInto(bytes, destinationOffset = offset)
    }

    fun build(): ComStructValue {
        require(fieldIndex == layout.fields.size) {
            "Expected ${layout.fields.size} fields, encoded $fieldIndex"
        }
        return ComStructValue(layout, bytes.copyOf())
    }

    private fun consume(expected: ComStructFieldKind): Int {
        require(fieldIndex < layout.fields.size) { "Struct layout exhausted while writing $expected" }
        val actual = layout.fields[fieldIndex]
        require(actual == expected) { "Expected $expected at field $fieldIndex but found $actual" }
        return layout.fieldOffsets[fieldIndex++]
    }

    private fun consumeStruct(structLayout: ComStructLayout): Int {
        require(fieldIndex < layout.fields.size) { "Struct layout exhausted while writing nested struct" }
        val nextIndex = fieldIndex + structLayout.fields.size
        require(nextIndex <= layout.fields.size) {
            "Nested struct with ${structLayout.fields.size} fields exceeds remaining layout"
        }
        val expected = layout.fields.subList(fieldIndex, nextIndex)
        require(expected == structLayout.fields) {
            "Nested struct layout $structLayout does not match remaining fields $expected"
        }
        val offset = layout.fieldOffsets[fieldIndex]
        fieldIndex = nextIndex
        return offset
    }

    private fun writeInt16(offset: Int, value: Int) {
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value ushr 8).toByte()
    }

    private fun writeInt32(offset: Int, value: Int) {
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value ushr 8).toByte()
        bytes[offset + 2] = (value ushr 16).toByte()
        bytes[offset + 3] = (value ushr 24).toByte()
    }

    private fun writeInt64(offset: Int, value: Long) {
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value ushr 8).toByte()
        bytes[offset + 2] = (value ushr 16).toByte()
        bytes[offset + 3] = (value ushr 24).toByte()
        bytes[offset + 4] = (value ushr 32).toByte()
        bytes[offset + 5] = (value ushr 40).toByte()
        bytes[offset + 6] = (value ushr 48).toByte()
        bytes[offset + 7] = (value ushr 56).toByte()
    }
}

@OptIn(ExperimentalUuidApi::class)
class ComStructReader(
    private val value: ComStructValue,
) {
    private var fieldIndex = 0

    fun readBoolean(): Boolean = value.bytes[consume(ComStructFieldKind.BOOLEAN)] != 0.toByte()

    fun readByte(): Byte = value.bytes[consume(ComStructFieldKind.INT8)]

    fun readUByte(): UByte = value.bytes[consume(ComStructFieldKind.UINT8)].toUByte()

    fun readShort(): Short = readInt16(consume(ComStructFieldKind.INT16)).toShort()

    fun readUShort(): UShort = readInt16(consume(ComStructFieldKind.UINT16)).toUShort()

    fun readChar(): Char = readInt16(consume(ComStructFieldKind.CHAR16)).toChar()

    fun readInt(): Int = readInt32(consume(ComStructFieldKind.INT32))

    fun readUInt(): UInt = readInt32(consume(ComStructFieldKind.UINT32)).toUInt()

    fun readLong(): Long = readInt64(consume(ComStructFieldKind.INT64))

    fun readULong(): ULong = readInt64(consume(ComStructFieldKind.UINT64)).toULong()

    fun readFloat(): Float = Float.fromBits(readInt32(consume(ComStructFieldKind.FLOAT32)))

    fun readDouble(): Double = Double.fromBits(readInt64(consume(ComStructFieldKind.FLOAT64)))

    fun readGuid(): Uuid {
        val offset = consume(ComStructFieldKind.GUID)
        return Uuid.parse(
            Guid(
                data1 = readInt32(offset),
                data2 = readInt16(offset + 4).toShort(),
                data3 = readInt16(offset + 6).toShort(),
                data4 = value.bytes.copyOfRange(offset + 8, offset + 16),
            ).toString(),
        )
    }

    fun readStruct(layout: ComStructLayout): ComStructValue {
        val offset = consumeStruct(layout)
        return ComStructValue(layout, value.bytes.copyOfRange(offset, offset + layout.byteSize))
    }

    private fun consume(expected: ComStructFieldKind): Int {
        require(fieldIndex < value.layout.fields.size) { "Struct layout exhausted while reading $expected" }
        val actual = value.layout.fields[fieldIndex]
        require(actual == expected) { "Expected $expected at field $fieldIndex but found $actual" }
        return value.layout.fieldOffsets[fieldIndex++]
    }

    private fun consumeStruct(structLayout: ComStructLayout): Int {
        require(fieldIndex < value.layout.fields.size) { "Struct layout exhausted while reading nested struct" }
        val nextIndex = fieldIndex + structLayout.fields.size
        require(nextIndex <= value.layout.fields.size) {
            "Nested struct with ${structLayout.fields.size} fields exceeds remaining layout"
        }
        val expected = value.layout.fields.subList(fieldIndex, nextIndex)
        require(expected == structLayout.fields) {
            "Nested struct layout $structLayout does not match remaining fields $expected"
        }
        val offset = value.layout.fieldOffsets[fieldIndex]
        fieldIndex = nextIndex
        return offset
    }

    private fun readInt16(offset: Int): Int {
        return (value.bytes[offset].toInt() and 0xFF) or
            ((value.bytes[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun readInt32(offset: Int): Int {
        return (value.bytes[offset].toInt() and 0xFF) or
            ((value.bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((value.bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((value.bytes[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun readInt64(offset: Int): Long {
        return (value.bytes[offset].toLong() and 0xFFL) or
            ((value.bytes[offset + 1].toLong() and 0xFFL) shl 8) or
            ((value.bytes[offset + 2].toLong() and 0xFFL) shl 16) or
            ((value.bytes[offset + 3].toLong() and 0xFFL) shl 24) or
            ((value.bytes[offset + 4].toLong() and 0xFFL) shl 32) or
            ((value.bytes[offset + 5].toLong() and 0xFFL) shl 40) or
            ((value.bytes[offset + 6].toLong() and 0xFFL) shl 48) or
            ((value.bytes[offset + 7].toLong() and 0xFFL) shl 56)
    }
}
