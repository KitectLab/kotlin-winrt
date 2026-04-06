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
    private val fullUnaryShapes = setOf(
        MethodSignatureShape.EMPTY,
        MethodSignatureShape.STRING,
        MethodSignatureShape.INT32,
        MethodSignatureShape.UINT32,
        MethodSignatureShape.BOOLEAN,
        MethodSignatureShape.INT64,
        MethodSignatureShape.EVENT_REGISTRATION_TOKEN,
        MethodSignatureShape.OBJECT,
    )
    private val uint64UnaryShapes = fullUnaryShapes - setOf(
        MethodSignatureShape.INT64,
        MethodSignatureShape.EVENT_REGISTRATION_TOKEN,
    )
    private val unaryMethodRuleFamilies = mapOf(
        MethodReturnKind.STRING to SharedMethodRuleFamily.STRING,
        MethodReturnKind.FLOAT32 to SharedMethodRuleFamily.FLOAT32,
        MethodReturnKind.FLOAT64 to SharedMethodRuleFamily.FLOAT64,
        MethodReturnKind.DATE_TIME to SharedMethodRuleFamily.DATE_TIME,
        MethodReturnKind.TIME_SPAN to SharedMethodRuleFamily.TIME_SPAN,
        MethodReturnKind.BOOLEAN to SharedMethodRuleFamily.BOOLEAN,
        MethodReturnKind.INT32 to SharedMethodRuleFamily.UNIT,
        MethodReturnKind.UINT32 to SharedMethodRuleFamily.UNIT,
        MethodReturnKind.INT64 to SharedMethodRuleFamily.OBJECT,
        MethodReturnKind.UINT64 to SharedMethodRuleFamily.OBJECT,
        MethodReturnKind.EVENT_REGISTRATION_TOKEN to SharedMethodRuleFamily.EVENT_REGISTRATION_TOKEN,
        MethodReturnKind.GUID to SharedMethodRuleFamily.GUID,
        MethodReturnKind.OBJECT to SharedMethodRuleFamily.OBJECT,
        MethodReturnKind.UNIT to SharedMethodRuleFamily.UNIT,
    )
    private val unaryMethodShapes = mapOf(
        MethodReturnKind.STRING to fullUnaryShapes,
        MethodReturnKind.FLOAT32 to fullUnaryShapes,
        MethodReturnKind.FLOAT64 to fullUnaryShapes,
        MethodReturnKind.DATE_TIME to fullUnaryShapes,
        MethodReturnKind.TIME_SPAN to fullUnaryShapes,
        MethodReturnKind.BOOLEAN to fullUnaryShapes,
        MethodReturnKind.INT32 to fullUnaryShapes,
        MethodReturnKind.UINT32 to fullUnaryShapes,
        MethodReturnKind.INT64 to fullUnaryShapes,
        MethodReturnKind.UINT64 to uint64UnaryShapes,
        MethodReturnKind.EVENT_REGISTRATION_TOKEN to fullUnaryShapes,
        MethodReturnKind.GUID to fullUnaryShapes,
        MethodReturnKind.OBJECT to fullUnaryShapes,
        MethodReturnKind.UNIT to fullUnaryShapes,
    )

    fun sharedMethodRuleFamily(signatureKey: MethodSignatureKey): SharedMethodRuleFamily? =
        unaryMethodRuleFamilies[signatureKey.returnKind]
            ?.takeIf { signatureKey.shape in unaryMethodShapes.getValue(signatureKey.returnKind) }
            ?: sharedTwoArgumentMethodRuleFamily(signatureKey)

    private fun sharedTwoArgumentMethodRuleFamily(signatureKey: MethodSignatureKey): SharedMethodRuleFamily? {
        val parameterCategories = signatureKey.shape.toParameterCategories() ?: return null
        return signatureKey.returnKind.twoArgumentSharedRuleFamily(parameterCategories)
    }
}
