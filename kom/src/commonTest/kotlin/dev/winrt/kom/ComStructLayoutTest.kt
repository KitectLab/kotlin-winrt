package dev.winrt.kom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ComStructLayoutTest {
    @Test
    fun computes_foundation_geometry_offsets_without_padding() {
        val pointLayout = ComStructLayout.of(
            ComStructFieldKind.FLOAT32,
            ComStructFieldKind.FLOAT32,
        )
        val rectLayout = ComStructLayout.of(
            ComStructFieldKind.FLOAT32,
            ComStructFieldKind.FLOAT32,
            ComStructFieldKind.FLOAT32,
            ComStructFieldKind.FLOAT32,
        )

        assertEquals(listOf(0, 4), pointLayout.fieldOffsets)
        assertEquals(8, pointLayout.byteSize)
        assertEquals(listOf(0, 4, 8, 12), rectLayout.fieldOffsets)
        assertEquals(16, rectLayout.byteSize)
    }

    @Test
    fun aligns_mixed_size_fields_to_their_natural_boundaries() {
        val layout = ComStructLayout.of(
            ComStructFieldKind.UINT8,
            ComStructFieldKind.FLOAT64,
            ComStructFieldKind.UINT8,
        )

        assertEquals(listOf(0, 8, 16), layout.fieldOffsets)
        assertEquals(24, layout.byteSize)
    }

    @Test
    fun validates_struct_value_size_against_layout() {
        val layout = ComStructLayout.of(
            ComStructFieldKind.FLOAT32,
            ComStructFieldKind.FLOAT32,
        )

        assertFailsWith<IllegalArgumentException> {
            ComStructValue(layout, ByteArray(4))
        }
    }
}
