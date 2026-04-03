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

    @Test
    fun com_method_result_require_helpers_return_the_concrete_values() {
        val hString = HString.NULL
        val objectValue = ComPtr.NULL
        val guid = Guid(
            data1 = 0x12345678.toInt(),
            data2 = 0x1234,
            data3 = 0x5678,
            data4 = byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x7f),
        )

        assertEquals(hString, ComMethodResult.HStringValue(hString).requireHString())
        assertEquals(objectValue, ComMethodResult.ObjectValue(objectValue).requireObject())
        assertEquals(42, ComMethodResult.Int32Value(42).requireInt32())
        assertEquals(42u, ComMethodResult.UInt32Value(42u).requireUInt32())
        assertEquals(42L, ComMethodResult.Int64Value(42L).requireInt64())
        assertEquals(42uL, ComMethodResult.UInt64Value(42uL).requireUInt64())
        assertTrue(ComMethodResult.BooleanValue(true).requireBoolean())
        assertEquals(1.5f, ComMethodResult.Float32Value(1.5f).requireFloat32())
        assertEquals(1.5, ComMethodResult.Float64Value(1.5).requireFloat64())
        assertEquals(guid, ComMethodResult.GuidValue(guid).requireGuid())
    }

    @Test
    fun com_method_result_require_helpers_fail_fast_for_wrong_variant() {
        assertFailsWith<ClassCastException> {
            ComMethodResult.Int32Value(42).requireObject()
        }
        assertFailsWith<ClassCastException> {
            ComMethodResult.BooleanValue(true).requireGuid()
        }
    }

    @Test
    fun hstring_null_close_is_a_noop() {
        HString.NULL.close()
        assertTrue(HString.NULL.isNull)
    }
}
