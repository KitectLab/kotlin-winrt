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

    private val sharedMethodRules: Map<MethodSignatureKey, SharedMethodRuleFamily> = buildMap {
        register(SharedMethodRuleFamily.STRING, MethodReturnKind.STRING, emptyShapes + unaryStringLikeShapes)
        register(SharedMethodRuleFamily.FLOAT32, MethodReturnKind.FLOAT32, emptyShapes + listOf(MethodSignatureShape.STRING, MethodSignatureShape.UINT32, MethodSignatureShape.BOOLEAN))
        register(SharedMethodRuleFamily.FLOAT64, MethodReturnKind.FLOAT64, emptyShapes + listOf(MethodSignatureShape.STRING, MethodSignatureShape.UINT32, MethodSignatureShape.BOOLEAN))
        register(SharedMethodRuleFamily.BOOLEAN, MethodReturnKind.BOOLEAN, emptyShapes + unaryStringLikeShapes + unaryObjectCapableShapes)
        register(SharedMethodRuleFamily.UNIT, MethodReturnKind.INT32, listOf(MethodSignatureShape.STRING, MethodSignatureShape.INT32, MethodSignatureShape.UINT32, MethodSignatureShape.BOOLEAN, MethodSignatureShape.OBJECT))
        register(SharedMethodRuleFamily.UNIT, MethodReturnKind.UINT32, emptyShapes + listOf(MethodSignatureShape.STRING, MethodSignatureShape.INT32, MethodSignatureShape.UINT32, MethodSignatureShape.BOOLEAN, MethodSignatureShape.OBJECT))
        register(SharedMethodRuleFamily.EVENT_REGISTRATION_TOKEN, MethodReturnKind.EVENT_REGISTRATION_TOKEN, emptyShapes)
        register(SharedMethodRuleFamily.OBJECT, MethodReturnKind.OBJECT, emptyShapes + listOf(MethodSignatureShape.STRING, MethodSignatureShape.UINT32, MethodSignatureShape.BOOLEAN, MethodSignatureShape.OBJECT))
        register(SharedMethodRuleFamily.UNIT, MethodReturnKind.UNIT, emptyShapes + unaryStringLikeShapes + unaryUnitOnlyShapes)
        register(SharedMethodRuleFamily.OBJECT, MethodReturnKind.INT64, listOf(MethodSignatureShape.STRING, MethodSignatureShape.INT32, MethodSignatureShape.UINT32, MethodSignatureShape.BOOLEAN, MethodSignatureShape.OBJECT))
        register(SharedMethodRuleFamily.OBJECT, MethodReturnKind.UINT64, listOf(MethodSignatureShape.STRING, MethodSignatureShape.INT32, MethodSignatureShape.UINT32, MethodSignatureShape.BOOLEAN, MethodSignatureShape.OBJECT))
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
        val parameterPair = signatureKey.shape.toTwoArgumentParameterPair() ?: return null
        return when (signatureKey.returnKind) {
            MethodReturnKind.UNIT -> if (parameterPair.isSupportedTwoArgumentUnitPair()) SharedMethodRuleFamily.UNIT else null
            MethodReturnKind.STRING -> if (parameterPair.isSupportedTwoArgumentUnifiedReturnPair()) SharedMethodRuleFamily.STRING else null
            MethodReturnKind.FLOAT32 -> if (parameterPair.isSupportedTwoArgumentUnifiedReturnPair()) SharedMethodRuleFamily.FLOAT32 else null
            MethodReturnKind.FLOAT64 -> if (parameterPair.isSupportedTwoArgumentUnifiedReturnPair()) SharedMethodRuleFamily.FLOAT64 else null
            MethodReturnKind.BOOLEAN -> if (parameterPair.isSupportedTwoArgumentUnifiedReturnPair()) SharedMethodRuleFamily.BOOLEAN else null
            MethodReturnKind.INT32,
            MethodReturnKind.UINT32,
            MethodReturnKind.INT64,
            MethodReturnKind.UINT64,
            MethodReturnKind.GUID,
            MethodReturnKind.OBJECT -> if (parameterPair.isSupportedTwoArgumentUnifiedReturnPair()) SharedMethodRuleFamily.OBJECT else null
            MethodReturnKind.EVENT_REGISTRATION_TOKEN -> null
        }
    }
}
