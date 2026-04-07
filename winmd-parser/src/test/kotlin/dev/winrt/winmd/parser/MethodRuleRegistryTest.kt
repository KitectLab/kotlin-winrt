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
        assertEquals(
            SharedMethodRuleFamily.DATE_TIME,
            MethodRuleRegistry.sharedMethodRuleFamily(MethodSignatureKey(MethodReturnKind.DATE_TIME, MethodSignatureShape.STRING)),
        )
        assertEquals(
            SharedMethodRuleFamily.TIME_SPAN,
            MethodRuleRegistry.sharedMethodRuleFamily(MethodSignatureKey(MethodReturnKind.TIME_SPAN, MethodSignatureShape.OBJECT)),
        )
        assertEquals(
            SharedMethodRuleFamily.EVENT_REGISTRATION_TOKEN,
            MethodRuleRegistry.sharedMethodRuleFamily(MethodSignatureKey(MethodReturnKind.EVENT_REGISTRATION_TOKEN, MethodSignatureShape.STRING)),
        )
    }

    @Test
    fun rejects_unsupported_shared_method_rule_families() {
        assertNull(
            MethodRuleRegistry.sharedMethodRuleFamily(MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.STRING_STRING)),
        )
    }

    @Test
    fun classifies_shared_method_plan_kinds() {
        assertEquals(
            SharedMethodPlanKind.UNARY,
            MethodRuleRegistry.sharedMethodPlan(MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.EMPTY))?.kind,
        )
        assertEquals(
            SharedMethodPlanKind.TWO_ARGUMENT_RETURN,
            MethodRuleRegistry.sharedMethodPlan(MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.OBJECT_STRING))?.kind,
        )
        assertEquals(
            SharedMethodPlanKind.TWO_ARGUMENT_UNIT,
            MethodRuleRegistry.sharedMethodPlan(MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.OBJECT_INT32))?.kind,
        )
    }
}
