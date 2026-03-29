package dev.winrt.winmd.parser

internal enum class SharedMethodRuleFamily {
    STRING,
    FLOAT64,
    BOOLEAN,
    OBJECT,
    UNIT,
}

internal object MethodRuleRegistry {
    private val sharedMethodRules: Map<MethodSignatureKey, SharedMethodRuleFamily> = buildMap {
        put(MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.EMPTY), SharedMethodRuleFamily.STRING)
        put(MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.STRING), SharedMethodRuleFamily.STRING)
        put(MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.INT32), SharedMethodRuleFamily.STRING)
        put(MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.UINT32), SharedMethodRuleFamily.STRING)

        put(MethodSignatureKey(MethodReturnKind.FLOAT64, MethodSignatureShape.EMPTY), SharedMethodRuleFamily.FLOAT64)
        put(MethodSignatureKey(MethodReturnKind.FLOAT64, MethodSignatureShape.STRING), SharedMethodRuleFamily.FLOAT64)
        put(MethodSignatureKey(MethodReturnKind.FLOAT64, MethodSignatureShape.UINT32), SharedMethodRuleFamily.FLOAT64)

        put(MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.EMPTY), SharedMethodRuleFamily.BOOLEAN)
        put(MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.STRING), SharedMethodRuleFamily.BOOLEAN)
        put(MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.UINT32), SharedMethodRuleFamily.BOOLEAN)
        put(MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.OBJECT), SharedMethodRuleFamily.BOOLEAN)

        put(MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.EMPTY), SharedMethodRuleFamily.OBJECT)
        put(MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.STRING), SharedMethodRuleFamily.OBJECT)
        put(MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.UINT32), SharedMethodRuleFamily.OBJECT)

        put(MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.EMPTY), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.STRING), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.INT32), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.INT64), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.OBJECT), SharedMethodRuleFamily.UNIT)
    }

    fun sharedMethodRuleFamily(signatureKey: MethodSignatureKey): SharedMethodRuleFamily? =
        sharedMethodRules[signatureKey]
}
