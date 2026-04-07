package dev.winrt.winmd.parser

internal object MethodRuleRegistry {
    private val fullUnaryShapes = setOf(
        MethodSignatureShape.EMPTY,
        methodSignatureShapeOf(MethodParameterCategory.STRING),
        methodSignatureShapeOf(MethodParameterCategory.INT32),
        methodSignatureShapeOf(MethodParameterCategory.UINT32),
        methodSignatureShapeOf(MethodParameterCategory.BOOLEAN),
        methodSignatureShapeOf(MethodParameterCategory.INT64),
        methodSignatureShapeOf(MethodParameterCategory.EVENT_REGISTRATION_TOKEN),
        methodSignatureShapeOf(MethodParameterCategory.OBJECT),
    )
    private val uint64UnaryShapes = fullUnaryShapes - setOf(
        methodSignatureShapeOf(MethodParameterCategory.INT64),
        methodSignatureShapeOf(MethodParameterCategory.EVENT_REGISTRATION_TOKEN),
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

    fun supportsSharedMethod(signatureKey: MethodSignatureKey): Boolean {
        if (unaryMethodShapes[signatureKey.returnKind]?.contains(signatureKey.shape) == true) {
            return true
        }
        val parameterCategories = signatureKey.shape.toParameterCategories()
        return signatureKey.returnKind.supportsTwoArgumentSharedMethod(parameterCategories)
    }
}
