package dev.winrt.kom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

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

    @Test
    fun native_boolean_converts_to_kotlin_boolean() {
        assertTrue(NativeBoolean.TRUE.toBoolean())
        assertFalse(NativeBoolean.FALSE.toBoolean())
    }

    @Test
    fun hresult_require_success_reports_hex_value() {
        val error = assertFailsWith<KomException> {
            HResult(-1).requireSuccess("Invoke")
        }

        assertTrue(error.message!!.contains("Invoke failed with HRESULT=0xffffffff"))
    }

    @Test
    fun com_ptr_null_is_reported_as_null() {
        assertTrue(ComPtr.NULL.isNull)
    }
}
