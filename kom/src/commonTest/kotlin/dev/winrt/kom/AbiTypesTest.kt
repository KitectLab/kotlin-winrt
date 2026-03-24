package dev.winrt.kom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AbiTypesTest {
    @Test
    fun hresult_success_is_detected() {
        assertTrue(HResult.OK.isSuccess)
        assertFalse(HResult(-1).isSuccess)
    }

    @Test
    fun guid_formats_as_expected() {
        val guid = Guid(
            data1 = 0x12345678.toInt(),
            data2 = 0x1234,
            data3 = 0x5678,
            data4 = byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x7f),
        )

        assertEquals("12345678-1234-5678-1122-33445566777f", guid.toString())
    }
}
