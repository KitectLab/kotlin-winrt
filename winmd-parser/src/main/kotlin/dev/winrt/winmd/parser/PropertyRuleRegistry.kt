package dev.winrt.winmd.parser

internal enum class RuntimePropertyGetterRuleFamily {
    IREFERENCE_STRING,
    STRING,
    BOOLEAN,
    GUID,
    DATE_TIME,
    TIME_SPAN,
    EVENT_REGISTRATION_TOKEN,
    INT32,
}

internal enum class RuntimePropertySetterRuleFamily {
    STRING,
    INT32,
}

internal enum class InterfacePropertyRuleFamily {
    ENUM,
    STRING,
    BOOLEAN,
    INT32,
}

internal object PropertyRuleRegistry {
    private val getterRules: Map<String, RuntimePropertyGetterRuleFamily> = mapOf(
        "IReference<String>" to RuntimePropertyGetterRuleFamily.IREFERENCE_STRING,
        "String" to RuntimePropertyGetterRuleFamily.STRING,
        "Boolean" to RuntimePropertyGetterRuleFamily.BOOLEAN,
        "Guid" to RuntimePropertyGetterRuleFamily.GUID,
        "DateTime" to RuntimePropertyGetterRuleFamily.DATE_TIME,
        "TimeSpan" to RuntimePropertyGetterRuleFamily.TIME_SPAN,
        "EventRegistrationToken" to RuntimePropertyGetterRuleFamily.EVENT_REGISTRATION_TOKEN,
        "Int32" to RuntimePropertyGetterRuleFamily.INT32,
    )

    private val setterRules: Map<String, RuntimePropertySetterRuleFamily> = mapOf(
        "String" to RuntimePropertySetterRuleFamily.STRING,
        "Int32" to RuntimePropertySetterRuleFamily.INT32,
    )

    fun interfaceGetterRuleFamily(
        type: String,
        isEnumType: Boolean,
    ): InterfacePropertyRuleFamily? {
        return when {
            isEnumType -> InterfacePropertyRuleFamily.ENUM
            type == "String" -> InterfacePropertyRuleFamily.STRING
            type == "Boolean" -> InterfacePropertyRuleFamily.BOOLEAN
            type == "Int32" -> InterfacePropertyRuleFamily.INT32
            else -> null
        }
    }

    fun interfaceSetterRuleFamily(type: String): InterfacePropertyRuleFamily? {
        return when (type) {
            "String" -> InterfacePropertyRuleFamily.STRING
            "Int32" -> InterfacePropertyRuleFamily.INT32
            else -> null
        }
    }

    fun getterRuleFamily(type: String): RuntimePropertyGetterRuleFamily? = getterRules[type]

    fun setterRuleFamily(type: String): RuntimePropertySetterRuleFamily? = setterRules[type]
}
