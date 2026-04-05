package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WinRtArrayParameterCategoryTest {
    @Test
    fun classifies_input_arrays_as_pass_array() {
        val parameter = WinMdParameter(name = "value", type = "Int32[]", isIn = true)

        assertEquals(WinRtArrayParameterCategory.PASS_ARRAY, parameter.arrayParameterCategory())
    }

    @Test
    fun classifies_out_arrays_as_fill_array_when_not_by_ref() {
        val parameter = WinMdParameter(name = "value", type = "Int32[]", isOut = true)

        assertEquals(WinRtArrayParameterCategory.FILL_ARRAY, parameter.arrayParameterCategory())
    }

    @Test
    fun classifies_by_ref_out_arrays_as_receive_array() {
        val parameter = WinMdParameter(name = "value", type = "Int32[]", byRef = true, isOut = true)

        assertEquals(WinRtArrayParameterCategory.RECEIVE_ARRAY, parameter.arrayParameterCategory())
    }

    @Test
    fun treats_array_returns_as_receive_array() {
        val method = WinMdMethod(name = "GetValues", returnType = "Int32[]")

        assertEquals(WinRtArrayParameterCategory.RECEIVE_ARRAY, method.arrayReturnCategory())
    }

    @Test
    fun keeps_bare_array_parameters_fail_closed_until_marshaling_support_exists() {
        val parameter = WinMdParameter(name = "value", type = "Int32[]")
        val method = WinMdMethod(name = "CreateValues", returnType = "Object", parameters = listOf(parameter))

        assertNull(parameter.arrayParameterCategory())
        assertTrue(method.requiresArrayMarshaling())
    }
}
