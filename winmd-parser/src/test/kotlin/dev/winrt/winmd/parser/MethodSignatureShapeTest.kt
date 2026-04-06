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
        assertEquals(MethodParameterCategory.INT32, methodParameterCategory("Windows.Foundation.HResult", ::supportsObjectType))
        assertEquals(MethodParameterCategory.BOOLEAN, methodParameterCategory("Boolean", ::supportsObjectType))
        assertEquals(MethodParameterCategory.INT64, methodParameterCategory("Int64", ::supportsObjectType))
        assertEquals(MethodParameterCategory.UINT32, methodParameterCategory("UInt32", ::supportsObjectType))
        assertEquals(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, methodParameterCategory("EventRegistrationToken", ::supportsObjectType))
        assertEquals(MethodParameterCategory.EVENT_REGISTRATION_TOKEN, methodParameterCategory("Windows.Foundation.EventRegistrationToken", ::supportsObjectType))
        assertEquals(MethodParameterCategory.OBJECT, methodParameterCategory("Windows.Foundation.Uri", ::supportsObjectType))
        assertEquals(MethodParameterCategory.OBJECT, methodParameterCategory("Object", ::supportsObjectType))
    }

    @Test
    fun classifies_qualified_event_registration_token_return_kind() {
        assertEquals(
            MethodSignatureKey(MethodReturnKind.EVENT_REGISTRATION_TOKEN, MethodSignatureShape.OBJECT),
            methodSignatureKey(
                "Windows.Foundation.EventRegistrationToken",
                listOf("Windows.Foundation.IInspectable"),
                ::supportsObjectType,
            ),
        )
    }

    @Test
    fun excludes_qualified_winrt_value_types_from_object_support() {
        assertFalse(supportsProjectedObjectTypeName("Windows.Foundation.EventRegistrationToken"))
        assertFalse(supportsProjectedObjectTypeName("Windows.Foundation.Int32"))
        assertFalse(supportsProjectedObjectTypeName("Windows.Foundation.WinRtBoolean"))
        assertTrue(supportsProjectedObjectTypeName("Windows.Foundation.Uri"))
    }

    @Test
    fun classifies_hresult_return_kind_as_int32() {
        assertEquals(
            MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.EMPTY),
            methodSignatureKey(
                "Windows.Foundation.HResult",
                emptyList(),
                ::supportsObjectType,
            ),
        )
        assertTrue(isHResultType("Windows.Foundation.HResult"))
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
    fun classifies_supported_two_argument_parameter_categories() {
        assertTrue(listOf(MethodParameterCategory.STRING, MethodParameterCategory.STRING).isSupportedTwoArgumentUnitCategories())
        assertTrue(listOf(MethodParameterCategory.OBJECT, MethodParameterCategory.INT32).isSupportedTwoArgumentUnitCategories())
        assertTrue(listOf(MethodParameterCategory.INT32, MethodParameterCategory.INT64).isSupportedTwoArgumentUnitCategories())
        assertTrue(listOf(MethodParameterCategory.STRING, MethodParameterCategory.INT32).isSupportedTwoArgumentUnifiedReturnCategories())
        assertTrue(listOf(MethodParameterCategory.INT64, MethodParameterCategory.OBJECT).isSupportedTwoArgumentUnifiedReturnCategories())
        assertFalse(listOf(MethodParameterCategory.STRING, MethodParameterCategory.STRING).isSupportedTwoArgumentUnifiedReturnCategories())
    }

    @Test
    fun folds_parameter_categories_into_abi_tokens() {
        assertEquals(
            MethodParameterAbiToken.STRING,
            MethodParameterCategory.STRING.toAbiToken(),
        )
        assertEquals(
            MethodParameterAbiToken.BOOLEAN,
            MethodParameterCategory.BOOLEAN.toAbiToken(),
        )
        assertEquals(
            MethodParameterAbiToken.INT64,
            MethodParameterCategory.EVENT_REGISTRATION_TOKEN.toAbiToken(),
        )
        assertEquals(
            MethodParameterAbiToken.OBJECT,
            MethodParameterCategory.OBJECT.toAbiToken(),
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
        return supportsProjectedObjectTypeName(type)
    }
}
