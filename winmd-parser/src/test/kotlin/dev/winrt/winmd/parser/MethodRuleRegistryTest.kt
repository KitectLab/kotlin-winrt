package dev.winrt.winmd.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MethodRuleRegistryTest {
    @Test
    fun classifies_shared_method_rule_families_for_common_runtime_shapes() {
        assertEquals(
            SharedMethodRuleFamily.STRING,
            MethodRuleRegistry.sharedMethodRuleFamily(MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.EMPTY)),
        )
        assertEquals(
            SharedMethodRuleFamily.OBJECT,
            MethodRuleRegistry.sharedMethodRuleFamily(MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.OBJECT_STRING)),
        )
        assertEquals(
            SharedMethodRuleFamily.UNIT,
            MethodRuleRegistry.sharedMethodRuleFamily(MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.OBJECT_INT32)),
        )
        assertEquals(
            SharedMethodRuleFamily.GUID,
            MethodRuleRegistry.sharedMethodRuleFamily(MethodSignatureKey(MethodReturnKind.GUID, MethodSignatureShape.STRING)),
        )
    }

    @Test
    fun rejects_unsupported_shared_method_rule_families() {
        assertNull(
            MethodRuleRegistry.sharedMethodRuleFamily(MethodSignatureKey(MethodReturnKind.EVENT_REGISTRATION_TOKEN, MethodSignatureShape.STRING)),
        )
        assertNull(
            MethodRuleRegistry.sharedMethodRuleFamily(MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.STRING_STRING)),
        )
    }
}
