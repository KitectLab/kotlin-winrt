package dev.winrt.core

import dev.winrt.kom.ComStructFieldKind
import dev.winrt.kom.ComStructLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ComStructCodecTest {
    @Test
    fun round_trips_float_struct_fields() {
        val layout = ComStructLayout.of(
            ComStructFieldKind.FLOAT32,
            ComStructFieldKind.FLOAT32,
        )
        val encoded = ComStructWriter(layout)
            .apply {
                writeFloat(1.25f)
                writeFloat(-2.5f)
            }
            .build()
        val reader = ComStructReader(encoded)

        assertEquals(1.25f, reader.readFloat())
        assertEquals(-2.5f, reader.readFloat())
    }

    @Test
    fun round_trips_nested_struct_bytes() {
        val innerLayout = ComStructLayout.of(
            ComStructFieldKind.FLOAT32,
            ComStructFieldKind.FLOAT32,
        )
        val outerLayout = ComStructLayout.of(
            ComStructFieldKind.UINT8,
            ComStructFieldKind.FLOAT32,
            ComStructFieldKind.FLOAT32,
            ComStructFieldKind.UINT8,
        )
        val innerValue = ComStructWriter(innerLayout)
            .apply {
                writeFloat(3.5f)
                writeFloat(7.25f)
            }
            .build()
        val encoded = ComStructWriter(outerLayout)
            .apply {
                writeUByte(1u)
                writeStruct(innerValue)
                writeUByte(2u)
            }
            .build()
        val reader = ComStructReader(encoded)

        assertEquals(1u, reader.readUByte())
        val nested = ComStructReader(reader.readStruct(innerLayout))
        assertEquals(3.5f, nested.readFloat())
        assertEquals(7.25f, nested.readFloat())
        assertEquals(2u, reader.readUByte())
    }

    @Test
    fun round_trips_guid_values() {
        val layout = ComStructLayout.of(ComStructFieldKind.GUID)
        val value = Uuid.parse("00112233-4455-6677-8899-aabbccddeeff")
        val encoded = ComStructWriter(layout)
            .apply { writeGuid(value) }
            .build()
        val reader = ComStructReader(encoded)

        assertEquals(value, reader.readGuid())
    }
}
