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
    private val sharedMethodRules: Map<MethodSignatureKey, SharedMethodRuleFamily> = buildMap {
        put(MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.EMPTY), SharedMethodRuleFamily.STRING)
        put(MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.STRING), SharedMethodRuleFamily.STRING)
        put(MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.INT32), SharedMethodRuleFamily.STRING)
        put(MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.UINT32), SharedMethodRuleFamily.STRING)
        put(MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.BOOLEAN), SharedMethodRuleFamily.STRING)

        put(MethodSignatureKey(MethodReturnKind.FLOAT32, MethodSignatureShape.EMPTY), SharedMethodRuleFamily.FLOAT32)
        put(MethodSignatureKey(MethodReturnKind.FLOAT32, MethodSignatureShape.STRING), SharedMethodRuleFamily.FLOAT32)
        put(MethodSignatureKey(MethodReturnKind.FLOAT32, MethodSignatureShape.UINT32), SharedMethodRuleFamily.FLOAT32)
        put(MethodSignatureKey(MethodReturnKind.FLOAT32, MethodSignatureShape.BOOLEAN), SharedMethodRuleFamily.FLOAT32)

        put(MethodSignatureKey(MethodReturnKind.FLOAT64, MethodSignatureShape.EMPTY), SharedMethodRuleFamily.FLOAT64)
        put(MethodSignatureKey(MethodReturnKind.FLOAT64, MethodSignatureShape.STRING), SharedMethodRuleFamily.FLOAT64)
        put(MethodSignatureKey(MethodReturnKind.FLOAT64, MethodSignatureShape.UINT32), SharedMethodRuleFamily.FLOAT64)
        put(MethodSignatureKey(MethodReturnKind.FLOAT64, MethodSignatureShape.BOOLEAN), SharedMethodRuleFamily.FLOAT64)

        put(MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.EMPTY), SharedMethodRuleFamily.BOOLEAN)
        put(MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.STRING), SharedMethodRuleFamily.BOOLEAN)
        put(MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.UINT32), SharedMethodRuleFamily.BOOLEAN)
        put(MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.BOOLEAN), SharedMethodRuleFamily.BOOLEAN)
        put(MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.OBJECT), SharedMethodRuleFamily.BOOLEAN)

        put(MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.STRING), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.INT32), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.UINT32), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.BOOLEAN), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.OBJECT), SharedMethodRuleFamily.UNIT)

        put(MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.EMPTY), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.STRING), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.INT32), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.UINT32), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.BOOLEAN), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.OBJECT), SharedMethodRuleFamily.UNIT)

        put(
            MethodSignatureKey(MethodReturnKind.EVENT_REGISTRATION_TOKEN, MethodSignatureShape.EMPTY),
            SharedMethodRuleFamily.EVENT_REGISTRATION_TOKEN,
        )

        put(MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.EMPTY), SharedMethodRuleFamily.OBJECT)
        put(MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.STRING), SharedMethodRuleFamily.OBJECT)
        put(MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.UINT32), SharedMethodRuleFamily.OBJECT)
        put(MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.BOOLEAN), SharedMethodRuleFamily.OBJECT)
        put(MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.OBJECT), SharedMethodRuleFamily.OBJECT)
        put(MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.OBJECT_STRING), SharedMethodRuleFamily.OBJECT)
        put(MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.STRING_OBJECT), SharedMethodRuleFamily.OBJECT)

        put(MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.EMPTY), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.STRING), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.INT32), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.UINT32), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.BOOLEAN), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.INT64), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.EVENT_REGISTRATION_TOKEN), SharedMethodRuleFamily.UNIT)
        put(MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.OBJECT), SharedMethodRuleFamily.UNIT)

        put(MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.STRING), SharedMethodRuleFamily.OBJECT)
        put(MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.INT32), SharedMethodRuleFamily.OBJECT)
        put(MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.UINT32), SharedMethodRuleFamily.OBJECT)
        put(MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.BOOLEAN), SharedMethodRuleFamily.OBJECT)
        put(MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.OBJECT), SharedMethodRuleFamily.OBJECT)

        put(MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.STRING), SharedMethodRuleFamily.OBJECT)
        put(MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.INT32), SharedMethodRuleFamily.OBJECT)
        put(MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.UINT32), SharedMethodRuleFamily.OBJECT)
        put(MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.BOOLEAN), SharedMethodRuleFamily.OBJECT)
        put(MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.OBJECT), SharedMethodRuleFamily.OBJECT)
    }

    fun sharedMethodRuleFamily(signatureKey: MethodSignatureKey): SharedMethodRuleFamily? =
        sharedMethodRules[signatureKey]
}
