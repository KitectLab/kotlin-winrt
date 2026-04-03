package dev.winrt.winmd.parser

internal enum class SharedMethodRuleFamily {
    STRING,
    FLOAT32,
    FLOAT64,
    DATE_TIME,
    TIME_SPAN,
    BOOLEAN,
    EVENT_REGISTRATION_TOKEN,
    GUID,
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

    private val sharedMethodRules: Map<MethodSignatureKey, SharedMethodRuleFamily> = buildMap {
        register(SharedMethodRuleFamily.STRING, MethodReturnKind.STRING, emptyShapes + unaryStringLikeShapes + listOf(MethodSignatureShape.INT64, MethodSignatureShape.OBJECT))
        register(SharedMethodRuleFamily.FLOAT32, MethodReturnKind.FLOAT32, emptyShapes + unaryStringLikeShapes + unaryUnitOnlyShapes)
        register(SharedMethodRuleFamily.FLOAT64, MethodReturnKind.FLOAT64, emptyShapes + unaryStringLikeShapes + unaryUnitOnlyShapes)
        register(SharedMethodRuleFamily.DATE_TIME, MethodReturnKind.DATE_TIME, emptyShapes + unaryStringLikeShapes + unaryUnitOnlyShapes)
        register(SharedMethodRuleFamily.TIME_SPAN, MethodReturnKind.TIME_SPAN, emptyShapes + unaryStringLikeShapes + unaryUnitOnlyShapes)
        register(SharedMethodRuleFamily.BOOLEAN, MethodReturnKind.BOOLEAN, emptyShapes + unaryStringLikeShapes + unaryUnitOnlyShapes)
        register(SharedMethodRuleFamily.UNIT, MethodReturnKind.INT32, listOf(MethodSignatureShape.STRING, MethodSignatureShape.INT32, MethodSignatureShape.UINT32, MethodSignatureShape.BOOLEAN, MethodSignatureShape.OBJECT))
        register(SharedMethodRuleFamily.UNIT, MethodReturnKind.UINT32, emptyShapes + listOf(MethodSignatureShape.STRING, MethodSignatureShape.INT32, MethodSignatureShape.UINT32, MethodSignatureShape.BOOLEAN, MethodSignatureShape.OBJECT))
        register(SharedMethodRuleFamily.EVENT_REGISTRATION_TOKEN, MethodReturnKind.EVENT_REGISTRATION_TOKEN, emptyShapes)
        register(SharedMethodRuleFamily.OBJECT, MethodReturnKind.OBJECT, emptyShapes + unaryStringLikeShapes + unaryUnitOnlyShapes)
        register(SharedMethodRuleFamily.UNIT, MethodReturnKind.UNIT, emptyShapes + unaryStringLikeShapes + unaryUnitOnlyShapes)
        register(SharedMethodRuleFamily.OBJECT, MethodReturnKind.INT64, unaryStringLikeShapes + unaryUnitOnlyShapes)
        register(SharedMethodRuleFamily.OBJECT, MethodReturnKind.UINT64, unaryStringLikeShapes + unaryUnitOnlyShapes)
        register(SharedMethodRuleFamily.GUID, MethodReturnKind.GUID, emptyShapes + unaryStringLikeShapes + unaryUnitOnlyShapes)
        register(SharedMethodRuleFamily.DATE_TIME, MethodReturnKind.DATE_TIME, listOf(MethodSignatureShape.STRING, MethodSignatureShape.INT32, MethodSignatureShape.UINT32, MethodSignatureShape.BOOLEAN, MethodSignatureShape.INT64, MethodSignatureShape.OBJECT))
        register(SharedMethodRuleFamily.TIME_SPAN, MethodReturnKind.TIME_SPAN, listOf(MethodSignatureShape.STRING, MethodSignatureShape.INT32, MethodSignatureShape.UINT32, MethodSignatureShape.BOOLEAN, MethodSignatureShape.INT64, MethodSignatureShape.OBJECT))
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
        sharedMethodRules[signatureKey] ?: sharedTwoArgumentMethodRuleFamily(signatureKey)

    private fun sharedTwoArgumentMethodRuleFamily(signatureKey: MethodSignatureKey): SharedMethodRuleFamily? {
        val parameterCategories = signatureKey.shape.toParameterCategories() ?: return null
        return signatureKey.returnKind.twoArgumentSharedRuleFamily(parameterCategories)
    }
}
