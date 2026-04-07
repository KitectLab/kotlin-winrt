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
            MethodSignatureKey(
                MethodReturnKind.EVENT_REGISTRATION_TOKEN,
                methodSignatureShapeOf(MethodParameterCategory.OBJECT),
            ),
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
            methodSignatureShapeOf(MethodParameterCategory.OBJECT, MethodParameterCategory.STRING),
            methodSignatureShape(listOf("Windows.Foundation.Uri", "String"), ::supportsObjectType),
        )
        assertEquals(
            methodSignatureShapeOf(MethodParameterCategory.STRING, MethodParameterCategory.OBJECT),
            methodSignatureShape(listOf("String", "Windows.Foundation.Uri"), ::supportsObjectType),
        )
        assertEquals(
            methodSignatureShapeOf(MethodParameterCategory.OBJECT, MethodParameterCategory.OBJECT),
            methodSignatureShape(listOf("Windows.Foundation.Uri", "Windows.System.User"), ::supportsObjectType),
        )
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
            methodSignatureShapeOf(MethodParameterCategory.STRING, MethodParameterCategory.INT32).toParameterCategories(),
        )
        assertEquals(
            listOf(MethodParameterCategory.OBJECT, MethodParameterCategory.OBJECT),
            methodSignatureShapeOf(MethodParameterCategory.OBJECT, MethodParameterCategory.OBJECT).toParameterCategories(),
        )
        assertEquals(
            listOf(MethodParameterCategory.OBJECT),
            methodSignatureShapeOf(MethodParameterCategory.OBJECT).toParameterCategories(),
        )
    }

    private fun supportsObjectType(type: String): Boolean {
        return supportsProjectedObjectTypeName(type)
    }
}
