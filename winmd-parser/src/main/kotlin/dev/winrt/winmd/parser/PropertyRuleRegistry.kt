package dev.winrt.winmd.parser

internal enum class RuntimePropertyGetterRuleFamily {
    OBJECT,
    IREFERENCE_STRING,
    STRING,
    FLOAT32,
    FLOAT64,
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
    OBJECT,
    STRING,
    INT32,
    UINT32,
    FLOAT32,
    BOOLEAN,
    FLOAT64,
}

internal enum class InterfacePropertyRuleFamily {
    ENUM,
    OBJECT,
    STRING,
    FLOAT32,
    FLOAT64,
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
        "Object" to RuntimePropertyGetterRuleFamily.OBJECT,
        "IReference<String>" to RuntimePropertyGetterRuleFamily.IREFERENCE_STRING,
        "String" to RuntimePropertyGetterRuleFamily.STRING,
        "Float32" to RuntimePropertyGetterRuleFamily.FLOAT32,
        "Float64" to RuntimePropertyGetterRuleFamily.FLOAT64,
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
        "Object" to RuntimePropertySetterRuleFamily.OBJECT,
        "String" to RuntimePropertySetterRuleFamily.STRING,
        "Int32" to RuntimePropertySetterRuleFamily.INT32,
        "UInt32" to RuntimePropertySetterRuleFamily.UINT32,
        "Float32" to RuntimePropertySetterRuleFamily.FLOAT32,
        "Boolean" to RuntimePropertySetterRuleFamily.BOOLEAN,
        "Float64" to RuntimePropertySetterRuleFamily.FLOAT64,
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
            type == "Float64" -> InterfacePropertyRuleFamily.FLOAT64
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
            "Float32" -> InterfacePropertyRuleFamily.FLOAT32
            "Boolean" -> InterfacePropertyRuleFamily.BOOLEAN
            "Float64" -> InterfacePropertyRuleFamily.FLOAT64
            "Int32" -> InterfacePropertyRuleFamily.INT32
            "UInt32" -> InterfacePropertyRuleFamily.UINT32
            else -> null
        }
    }

    fun getterRuleFamily(type: String): RuntimePropertyGetterRuleFamily? = getterRules[type]

    fun setterRuleFamily(type: String): RuntimePropertySetterRuleFamily? = setterRules[type]
}
