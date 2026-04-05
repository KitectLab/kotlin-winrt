package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WinMdMethodOverloadKeyTest {
    @Test
    fun distinguishes_array_parameter_directions() {
        val passArray = WinMdMethod(
            name = "CopyTo",
            returnType = "Unit",
            parameters = listOf(WinMdParameter(name = "value", type = "Int32[]", isIn = true)),
        )
        val receiveArray = WinMdMethod(
            name = "CopyTo",
            returnType = "Unit",
            parameters = listOf(WinMdParameter(name = "value", type = "Int32[]", byRef = true, isOut = true)),
        )

        assertNotEquals(passArray.overloadKey("copyTo"), receiveArray.overloadKey("copyTo"))
    }

    @Test
    fun keeps_rendered_method_name_in_the_key() {
        val toStringMethod = WinMdMethod(name = "ToString", returnType = "String")

        assertEquals("toString()", toStringMethod.overloadKey("toString"))
    }
}
