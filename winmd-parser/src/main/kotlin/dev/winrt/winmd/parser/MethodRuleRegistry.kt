package dev.winrt.winmd.parser

internal enum class SharedMethodRuleFamily {
    STRING,
    FLOAT32,
    FLOAT64,
    BOOLEAN,
    EVENT_REGISTRATION_TOKEN,
    OBJECT,
    UNIT,
}

internal object MethodRuleRegistry {
    private val emptyShapes = listOf(MethodSignatureShape.EMPTY)
    private val unaryStringLikeShapes = listOf(
        MethodSignatureShape.STRING,
        MethodSignatureShape.INT32,
        MethodSignatureShape.UINT32,
        MethodSignatureShape.BOOLEAN,
    )
    private val unaryUnitOnlyShapes = listOf(
        MethodSignatureShape.INT64,
        MethodSignatureShape.EVENT_REGISTRATION_TOKEN,
        MethodSignatureShape.OBJECT,
    )
    private val unaryObjectCapableShapes = listOf(
        MethodSignatureShape.OBJECT,
    )
    private val twoArgumentObjectShapes = listOf(
        MethodSignatureShape.OBJECT_INT32,
        MethodSignatureShape.OBJECT_UINT32,
        MethodSignatureShape.OBJECT_BOOLEAN,
        MethodSignatureShape.OBJECT_INT64,
        MethodSignatureShape.OBJECT_EVENT_REGISTRATION_TOKEN,
        MethodSignatureShape.INT32_OBJECT,
        MethodSignatureShape.UINT32_OBJECT,
        MethodSignatureShape.BOOLEAN_OBJECT,
        MethodSignatureShape.INT64_OBJECT,
        MethodSignatureShape.EVENT_REGISTRATION_TOKEN_OBJECT,
        MethodSignatureShape.OBJECT_STRING,
        MethodSignatureShape.STRING_OBJECT,
        MethodSignatureShape.TWO_OBJECT,
    )
    private val twoArgumentUnitScalarShapes = listOf(
        MethodSignatureShape.STRING_INT32,
        MethodSignatureShape.INT32_STRING,
        MethodSignatureShape.STRING_UINT32,
        MethodSignatureShape.UINT32_STRING,
        MethodSignatureShape.STRING_BOOLEAN,
        MethodSignatureShape.BOOLEAN_STRING,
        MethodSignatureShape.STRING_INT64,
        MethodSignatureShape.INT64_STRING,
        MethodSignatureShape.STRING_EVENT_REGISTRATION_TOKEN,
        MethodSignatureShape.EVENT_REGISTRATION_TOKEN_STRING,
        MethodSignatureShape.STRING_STRING,
        MethodSignatureShape.INT32_INT32,
        MethodSignatureShape.INT32_UINT32,
        MethodSignatureShape.INT32_BOOLEAN,
        MethodSignatureShape.INT32_INT64,
        MethodSignatureShape.INT32_EVENT_REGISTRATION_TOKEN,
        MethodSignatureShape.UINT32_INT32,
        MethodSignatureShape.UINT32_UINT32,
        MethodSignatureShape.UINT32_BOOLEAN,
        MethodSignatureShape.UINT32_INT64,
        MethodSignatureShape.UINT32_EVENT_REGISTRATION_TOKEN,
        MethodSignatureShape.BOOLEAN_INT32,
        MethodSignatureShape.BOOLEAN_UINT32,
        MethodSignatureShape.BOOLEAN_BOOLEAN,
        MethodSignatureShape.BOOLEAN_INT64,
        MethodSignatureShape.BOOLEAN_EVENT_REGISTRATION_TOKEN,
        MethodSignatureShape.INT64_INT32,
        MethodSignatureShape.INT64_UINT32,
        MethodSignatureShape.INT64_BOOLEAN,
        MethodSignatureShape.INT64_INT64,
        MethodSignatureShape.INT64_EVENT_REGISTRATION_TOKEN,
        MethodSignatureShape.EVENT_REGISTRATION_TOKEN_INT32,
        MethodSignatureShape.EVENT_REGISTRATION_TOKEN_UINT32,
        MethodSignatureShape.EVENT_REGISTRATION_TOKEN_BOOLEAN,
        MethodSignatureShape.EVENT_REGISTRATION_TOKEN_INT64,
        MethodSignatureShape.EVENT_REGISTRATION_TOKEN_EVENT_REGISTRATION_TOKEN,
    )

    private val sharedMethodRules: Map<MethodSignatureKey, SharedMethodRuleFamily> = buildMap {
        register(SharedMethodRuleFamily.STRING, MethodReturnKind.STRING, emptyShapes + unaryStringLikeShapes + twoArgumentObjectShapes)
        register(SharedMethodRuleFamily.FLOAT32, MethodReturnKind.FLOAT32, emptyShapes + listOf(MethodSignatureShape.STRING, MethodSignatureShape.UINT32, MethodSignatureShape.BOOLEAN) + twoArgumentObjectShapes)
        register(SharedMethodRuleFamily.FLOAT64, MethodReturnKind.FLOAT64, emptyShapes + listOf(MethodSignatureShape.STRING, MethodSignatureShape.UINT32, MethodSignatureShape.BOOLEAN) + twoArgumentObjectShapes)
        register(SharedMethodRuleFamily.BOOLEAN, MethodReturnKind.BOOLEAN, emptyShapes + unaryStringLikeShapes + unaryObjectCapableShapes + twoArgumentObjectShapes)
        register(SharedMethodRuleFamily.UNIT, MethodReturnKind.INT32, listOf(MethodSignatureShape.STRING, MethodSignatureShape.INT32, MethodSignatureShape.UINT32, MethodSignatureShape.BOOLEAN, MethodSignatureShape.OBJECT) + twoArgumentObjectShapes)
        register(SharedMethodRuleFamily.UNIT, MethodReturnKind.UINT32, emptyShapes + listOf(MethodSignatureShape.STRING, MethodSignatureShape.INT32, MethodSignatureShape.UINT32, MethodSignatureShape.BOOLEAN, MethodSignatureShape.OBJECT) + twoArgumentObjectShapes)
        register(SharedMethodRuleFamily.EVENT_REGISTRATION_TOKEN, MethodReturnKind.EVENT_REGISTRATION_TOKEN, emptyShapes)
        register(SharedMethodRuleFamily.OBJECT, MethodReturnKind.OBJECT, emptyShapes + listOf(MethodSignatureShape.STRING, MethodSignatureShape.UINT32, MethodSignatureShape.BOOLEAN, MethodSignatureShape.OBJECT) + twoArgumentObjectShapes)
        register(SharedMethodRuleFamily.UNIT, MethodReturnKind.UNIT, emptyShapes + unaryStringLikeShapes + unaryUnitOnlyShapes + twoArgumentUnitScalarShapes + twoArgumentObjectShapes)
        register(SharedMethodRuleFamily.OBJECT, MethodReturnKind.INT64, listOf(MethodSignatureShape.STRING, MethodSignatureShape.INT32, MethodSignatureShape.UINT32, MethodSignatureShape.BOOLEAN, MethodSignatureShape.OBJECT) + twoArgumentObjectShapes)
        register(SharedMethodRuleFamily.OBJECT, MethodReturnKind.UINT64, listOf(MethodSignatureShape.STRING, MethodSignatureShape.INT32, MethodSignatureShape.UINT32, MethodSignatureShape.BOOLEAN, MethodSignatureShape.OBJECT) + twoArgumentObjectShapes)
        register(SharedMethodRuleFamily.UNIT, MethodReturnKind.GUID, twoArgumentObjectShapes)
    }

    private fun MutableMap<MethodSignatureKey, SharedMethodRuleFamily>.register(
        family: SharedMethodRuleFamily,
        returnKind: MethodReturnKind,
        shapes: List<MethodSignatureShape>,
    ) {
        for (shape in shapes) {
            put(MethodSignatureKey(returnKind, shape), family)
        }
    }

    fun sharedMethodRuleFamily(signatureKey: MethodSignatureKey): SharedMethodRuleFamily? =
        sharedMethodRules[signatureKey]
}
