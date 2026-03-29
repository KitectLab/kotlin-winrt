package dev.winrt.winmd.parser

internal enum class RuntimePropertyGetterRuleFamily {
    IREFERENCE_STRING,
    STRING,
    FLOAT32,
    BOOLEAN,
    GUID,
    DATE_TIME,
    TIME_SPAN,
    EVENT_REGISTRATION_TOKEN,
    INT32,
    UINT32,
    INT64,
    UINT64,
}

internal enum class RuntimePropertySetterRuleFamily {
    STRING,
    INT32,
}

internal enum class InterfacePropertyRuleFamily {
    ENUM,
    OBJECT,
    STRING,
    FLOAT32,
    BOOLEAN,
    GUID,
    DATE_TIME,
    TIME_SPAN,
    EVENT_REGISTRATION_TOKEN,
    INT32,
    UINT32,
    INT64,
    UINT64,
}

internal object PropertyRuleRegistry {
    private val getterRules: Map<String, RuntimePropertyGetterRuleFamily> = mapOf(
        "IReference<String>" to RuntimePropertyGetterRuleFamily.IREFERENCE_STRING,
        "String" to RuntimePropertyGetterRuleFamily.STRING,
        "Float32" to RuntimePropertyGetterRuleFamily.FLOAT32,
        "Boolean" to RuntimePropertyGetterRuleFamily.BOOLEAN,
        "Guid" to RuntimePropertyGetterRuleFamily.GUID,
        "DateTime" to RuntimePropertyGetterRuleFamily.DATE_TIME,
        "TimeSpan" to RuntimePropertyGetterRuleFamily.TIME_SPAN,
        "EventRegistrationToken" to RuntimePropertyGetterRuleFamily.EVENT_REGISTRATION_TOKEN,
        "Int32" to RuntimePropertyGetterRuleFamily.INT32,
        "UInt32" to RuntimePropertyGetterRuleFamily.UINT32,
        "Int64" to RuntimePropertyGetterRuleFamily.INT64,
        "UInt64" to RuntimePropertyGetterRuleFamily.UINT64,
    )

    private val setterRules: Map<String, RuntimePropertySetterRuleFamily> = mapOf(
        "String" to RuntimePropertySetterRuleFamily.STRING,
        "Int32" to RuntimePropertySetterRuleFamily.INT32,
    )

    fun interfaceGetterRuleFamily(
        type: String,
        isEnumType: Boolean,
        isObjectType: Boolean,
    ): InterfacePropertyRuleFamily? {
        return when {
            isEnumType -> InterfacePropertyRuleFamily.ENUM
            isObjectType -> InterfacePropertyRuleFamily.OBJECT
            type == "String" -> InterfacePropertyRuleFamily.STRING
            type == "Float32" -> InterfacePropertyRuleFamily.FLOAT32
            type == "Boolean" -> InterfacePropertyRuleFamily.BOOLEAN
            type == "Guid" -> InterfacePropertyRuleFamily.GUID
            type == "DateTime" -> InterfacePropertyRuleFamily.DATE_TIME
            type == "TimeSpan" -> InterfacePropertyRuleFamily.TIME_SPAN
            type == "EventRegistrationToken" -> InterfacePropertyRuleFamily.EVENT_REGISTRATION_TOKEN
            type == "Int32" -> InterfacePropertyRuleFamily.INT32
            type == "UInt32" -> InterfacePropertyRuleFamily.UINT32
            type == "Int64" -> InterfacePropertyRuleFamily.INT64
            type == "UInt64" -> InterfacePropertyRuleFamily.UINT64
            else -> null
        }
    }

    fun interfaceSetterRuleFamily(type: String, isObjectType: Boolean): InterfacePropertyRuleFamily? {
        return when (type) {
            in setOf(type).takeIf { isObjectType } ?: emptySet() -> InterfacePropertyRuleFamily.OBJECT
            "String" -> InterfacePropertyRuleFamily.STRING
            "Int32" -> InterfacePropertyRuleFamily.INT32
            else -> null
        }
    }

    fun getterRuleFamily(type: String): RuntimePropertyGetterRuleFamily? = getterRules[type]

    fun setterRuleFamily(type: String): RuntimePropertySetterRuleFamily? = setterRules[type]
}
