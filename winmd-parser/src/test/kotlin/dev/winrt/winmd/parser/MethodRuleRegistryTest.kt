package dev.winrt.winmd.parser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MethodRuleRegistryTest {
    @Test
    fun accepts_supported_shared_method_shapes() {
        assertTrue(MethodRuleRegistry.supportsSharedMethod(MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.EMPTY)))
        assertTrue(
            MethodRuleRegistry.supportsSharedMethod(
                MethodSignatureKey(
                    MethodReturnKind.OBJECT,
                    methodSignatureShapeOf(MethodParameterCategory.OBJECT, MethodParameterCategory.STRING),
                ),
            ),
        )
        assertTrue(
            MethodRuleRegistry.supportsSharedMethod(
                MethodSignatureKey(
                    MethodReturnKind.UNIT,
                    methodSignatureShapeOf(MethodParameterCategory.OBJECT, MethodParameterCategory.INT32),
                ),
            ),
        )
        assertTrue(
            MethodRuleRegistry.supportsSharedMethod(
                MethodSignatureKey(MethodReturnKind.GUID, methodSignatureShapeOf(MethodParameterCategory.STRING)),
            ),
        )
        assertTrue(
            MethodRuleRegistry.supportsSharedMethod(
                MethodSignatureKey(MethodReturnKind.DATE_TIME, methodSignatureShapeOf(MethodParameterCategory.STRING)),
            ),
        )
        assertTrue(
            MethodRuleRegistry.supportsSharedMethod(
                MethodSignatureKey(MethodReturnKind.TIME_SPAN, methodSignatureShapeOf(MethodParameterCategory.OBJECT)),
            ),
        )
        assertTrue(
            MethodRuleRegistry.supportsSharedMethod(
                MethodSignatureKey(
                    MethodReturnKind.EVENT_REGISTRATION_TOKEN,
                    methodSignatureShapeOf(MethodParameterCategory.STRING),
                ),
            ),
        )
    }

    @Test
    fun rejects_unsupported_shared_method_shapes() {
        assertFalse(
            MethodRuleRegistry.supportsSharedMethod(
                MethodSignatureKey(
                    MethodReturnKind.STRING,
                    methodSignatureShapeOf(MethodParameterCategory.STRING, MethodParameterCategory.STRING),
                ),
            ),
        )
    }
}
