package dev.winrt.winmd.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MethodSignatureShapeTest {
    @Test
    fun classifies_parameter_categories() {
        assertEquals(MethodParameterCategory.STRING, methodParameterCategory("String", ::supportsObjectType))
        assertEquals(MethodParameterCategory.INT32, methodParameterCategory("Int32", ::supportsObjectType))
        assertEquals(MethodParameterCategory.BOOLEAN, methodParameterCategory("Boolean", ::supportsObjectType))
        assertEquals(MethodParameterCategory.INT64, methodParameterCategory("Int64", ::supportsObjectType))
        assertEquals(MethodParameterCategory.UINT32, methodParameterCategory("UInt32", ::supportsObjectType))
        assertEquals(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, methodParameterCategory("EventRegistrationToken", ::supportsObjectType))
        assertEquals(MethodParameterCategory.OBJECT, methodParameterCategory("Windows.Foundation.Uri", ::supportsObjectType))
        assertEquals(MethodParameterCategory.OBJECT, methodParameterCategory("Object", ::supportsObjectType))
    }

    @Test
    fun derives_supported_two_argument_object_shapes_from_categories() {
        assertEquals(
            MethodSignatureShape.OBJECT_STRING,
            methodSignatureShape(listOf("Windows.Foundation.Uri", "String"), ::supportsObjectType),
        )
        assertEquals(
            MethodSignatureShape.STRING_OBJECT,
            methodSignatureShape(listOf("String", "Windows.Foundation.Uri"), ::supportsObjectType),
        )
        assertEquals(
            MethodSignatureShape.TWO_OBJECT,
            methodSignatureShape(listOf("Windows.Foundation.Uri", "Windows.System.User"), ::supportsObjectType),
        )
    }

    @Test
    fun identifies_two_argument_unified_return_keys() {
        assertTrue(MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.OBJECT_STRING).isTwoArgumentUnifiedReturnShape())
        assertTrue(MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.TWO_OBJECT).isTwoArgumentUnifiedReturnShape())
        assertTrue(MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.TWO_OBJECT).isTwoArgumentUnifiedReturnShape())
        assertTrue(MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.OBJECT_INT32).isTwoArgumentUnifiedReturnShape())
        assertTrue(MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.STRING_INT32).isTwoArgumentUnifiedReturnShape())
        assertFalse(MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.STRING).isTwoArgumentUnifiedReturnShape())
    }

    @Test
    fun exposes_two_argument_parameter_categories_for_supported_shapes() {
        assertEquals(
            MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.STRING),
            MethodSignatureShape.STRING_STRING.toTwoArgumentParameterPair(),
        )
        assertEquals(
            MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.INT32),
            MethodSignatureShape.STRING_INT32.toTwoArgumentParameterPair(),
        )
        assertEquals(
            MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.STRING),
            MethodSignatureShape.INT32_STRING.toTwoArgumentParameterPair(),
        )
        assertEquals(
            MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.INT64),
            MethodSignatureShape.INT32_INT64.toTwoArgumentParameterPair(),
        )
        assertEquals(
            MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.BOOLEAN),
            MethodSignatureShape.EVENT_REGISTRATION_TOKEN_BOOLEAN.toTwoArgumentParameterPair(),
        )
        assertEquals(
            MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.INT32),
            MethodSignatureShape.OBJECT_INT32.toTwoArgumentParameterPair(),
        )
        assertEquals(
            MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.OBJECT),
            MethodSignatureShape.INT64_OBJECT.toTwoArgumentParameterPair(),
        )
        assertEquals(
            MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.STRING),
            MethodSignatureShape.OBJECT_STRING.toTwoArgumentParameterPair(),
        )
        assertEquals(
            MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.OBJECT),
            MethodSignatureShape.STRING_OBJECT.toTwoArgumentParameterPair(),
        )
        assertEquals(
            MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.OBJECT),
            MethodSignatureShape.TWO_OBJECT.toTwoArgumentParameterPair(),
        )
        assertEquals(null, MethodSignatureShape.STRING.toTwoArgumentParameterPair())
    }

    @Test
    fun classifies_supported_two_argument_parameter_pairs() {
        assertTrue(MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.STRING).isSupportedTwoArgumentUnitPair())
        assertTrue(MethodParameterPair(MethodParameterCategory.OBJECT, MethodParameterCategory.INT32).isSupportedTwoArgumentUnitPair())
        assertTrue(MethodParameterPair(MethodParameterCategory.INT32, MethodParameterCategory.INT64).isSupportedTwoArgumentUnitPair())
        assertTrue(MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.INT32).isSupportedTwoArgumentUnifiedReturnPair())
        assertTrue(MethodParameterPair(MethodParameterCategory.INT64, MethodParameterCategory.OBJECT).isSupportedTwoArgumentUnifiedReturnPair())
        assertFalse(MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.STRING).isSupportedTwoArgumentUnifiedReturnPair())
    }

    @Test
    fun folds_parameter_pairs_into_abi_families() {
        assertEquals(
            MethodParameterFamilyPair(MethodParameterAbiFamily.STRING, MethodParameterAbiFamily.INT32_LIKE),
            MethodParameterPair(MethodParameterCategory.STRING, MethodParameterCategory.BOOLEAN).toAbiFamilyPair(),
        )
        assertEquals(
            MethodParameterFamilyPair(MethodParameterAbiFamily.INT64_LIKE, MethodParameterAbiFamily.OBJECT),
            MethodParameterPair(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, MethodParameterCategory.OBJECT).toAbiFamilyPair(),
        )
    }

    @Test
    fun exposes_parameter_categories_for_supported_shapes() {
        assertEquals(
            listOf(MethodParameterCategory.STRING, MethodParameterCategory.INT32),
            MethodSignatureShape.STRING_INT32.toParameterCategories(),
        )
        assertEquals(
            listOf(MethodParameterCategory.OBJECT, MethodParameterCategory.OBJECT),
            MethodSignatureShape.TWO_OBJECT.toParameterCategories(),
        )
        assertEquals(
            listOf(MethodParameterCategory.OBJECT),
            MethodSignatureShape.OBJECT.toParameterCategories(),
        )
    }

    private fun supportsObjectType(type: String): Boolean {
        return (type == "Object" || type.contains('.')) &&
            !type.contains('`') &&
            !type.contains('<') &&
            !type.endsWith("[]")
    }
}
