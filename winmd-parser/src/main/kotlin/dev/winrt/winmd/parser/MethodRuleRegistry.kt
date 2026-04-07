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
    private fun unaryMethodShapes(returnKind: MethodReturnKind): Set<MethodSignatureShape> =
        if (returnKind == MethodReturnKind.UINT64) uint64UnaryShapes else fullUnaryShapes

    fun supportsSharedMethod(signatureKey: MethodSignatureKey): Boolean {
        if (signatureKey.shape in unaryMethodShapes(signatureKey.returnKind)) {
            return true
        }
        val parameterCategories = signatureKey.shape.toParameterCategories()
        return signatureKey.returnKind.supportsTwoArgumentSharedMethod(parameterCategories)
    }
}
