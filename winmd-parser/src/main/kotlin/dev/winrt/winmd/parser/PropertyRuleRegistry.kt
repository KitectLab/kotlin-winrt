package dev.winrt.winmd.parser

internal enum class RuntimePropertyGetterRuleFamily {
    OBJECT,
    IREFERENCE_STRING,
    STRING,
    UINT8,
    INT16,
    UINT16,
    CHAR16,
    FLOAT32,
    FLOAT64,
    BOOLEAN,
    GUID,
    DATE_TIME,
    TIME_SPAN,
    EVENT_REGISTRATION_TOKEN,
    HRESULT,
    INT32,
    UINT32,
    INT64,
    UINT64,
}

internal enum class RuntimePropertySetterRuleFamily {
    OBJECT,
    STRING,
    UINT8,
    INT16,
    UINT16,
    CHAR16,
    HRESULT,
    INT32,
    UINT32,
    FLOAT32,
    BOOLEAN,
    FLOAT64,
    INT64,
    UINT64,
}

internal enum class InterfacePropertyRuleFamily {
    ENUM,
    OBJECT,
    STRING,
    UINT8,
    INT16,
    UINT16,
    CHAR16,
    FLOAT32,
    FLOAT64,
    BOOLEAN,
    GUID,
    DATE_TIME,
    TIME_SPAN,
    EVENT_REGISTRATION_TOKEN,
    HRESULT,
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
        "UInt8" to RuntimePropertyGetterRuleFamily.UINT8,
        "Int16" to RuntimePropertyGetterRuleFamily.INT16,
        "UInt16" to RuntimePropertyGetterRuleFamily.UINT16,
        "Char16" to RuntimePropertyGetterRuleFamily.CHAR16,
        "Float32" to RuntimePropertyGetterRuleFamily.FLOAT32,
        "Float64" to RuntimePropertyGetterRuleFamily.FLOAT64,
        "Boolean" to RuntimePropertyGetterRuleFamily.BOOLEAN,
        "Guid" to RuntimePropertyGetterRuleFamily.GUID,
        "DateTime" to RuntimePropertyGetterRuleFamily.DATE_TIME,
        "TimeSpan" to RuntimePropertyGetterRuleFamily.TIME_SPAN,
        "EventRegistrationToken" to RuntimePropertyGetterRuleFamily.EVENT_REGISTRATION_TOKEN,
        "HResult" to RuntimePropertyGetterRuleFamily.HRESULT,
        "Int32" to RuntimePropertyGetterRuleFamily.INT32,
        "UInt32" to RuntimePropertyGetterRuleFamily.UINT32,
        "Int64" to RuntimePropertyGetterRuleFamily.INT64,
        "UInt64" to RuntimePropertyGetterRuleFamily.UINT64,
    )

    private val setterRules: Map<String, RuntimePropertySetterRuleFamily> = mapOf(
        "Object" to RuntimePropertySetterRuleFamily.OBJECT,
        "String" to RuntimePropertySetterRuleFamily.STRING,
        "UInt8" to RuntimePropertySetterRuleFamily.UINT8,
        "Int16" to RuntimePropertySetterRuleFamily.INT16,
        "UInt16" to RuntimePropertySetterRuleFamily.UINT16,
        "Char16" to RuntimePropertySetterRuleFamily.CHAR16,
        "HResult" to RuntimePropertySetterRuleFamily.HRESULT,
        "Int32" to RuntimePropertySetterRuleFamily.INT32,
        "UInt32" to RuntimePropertySetterRuleFamily.UINT32,
        "Float32" to RuntimePropertySetterRuleFamily.FLOAT32,
        "Boolean" to RuntimePropertySetterRuleFamily.BOOLEAN,
        "Float64" to RuntimePropertySetterRuleFamily.FLOAT64,
        "Int64" to RuntimePropertySetterRuleFamily.INT64,
        "UInt64" to RuntimePropertySetterRuleFamily.UINT64,
    )

    fun interfaceGetterRuleFamily(
        type: String,
        isEnumType: Boolean,
        isObjectType: Boolean,
    ): InterfacePropertyRuleFamily? {
        val canonicalType = canonicalWinRtSpecialType(type)
        return when {
            isEnumType -> InterfacePropertyRuleFamily.ENUM
            isObjectType -> InterfacePropertyRuleFamily.OBJECT
            canonicalType == "String" -> InterfacePropertyRuleFamily.STRING
            canonicalType == "UInt8" -> InterfacePropertyRuleFamily.UINT8
            canonicalType == "Int16" -> InterfacePropertyRuleFamily.INT16
            canonicalType == "UInt16" -> InterfacePropertyRuleFamily.UINT16
            canonicalType == "Char16" -> InterfacePropertyRuleFamily.CHAR16
            canonicalType == "Float32" -> InterfacePropertyRuleFamily.FLOAT32
            canonicalType == "Float64" -> InterfacePropertyRuleFamily.FLOAT64
            canonicalType == "Boolean" -> InterfacePropertyRuleFamily.BOOLEAN
            canonicalType == "Guid" -> InterfacePropertyRuleFamily.GUID
            canonicalType == "DateTime" -> InterfacePropertyRuleFamily.DATE_TIME
            canonicalType == "TimeSpan" -> InterfacePropertyRuleFamily.TIME_SPAN
            canonicalType == "EventRegistrationToken" -> InterfacePropertyRuleFamily.EVENT_REGISTRATION_TOKEN
            canonicalType == "HResult" -> InterfacePropertyRuleFamily.HRESULT
            canonicalType == "Int32" -> InterfacePropertyRuleFamily.INT32
            canonicalType == "UInt32" -> InterfacePropertyRuleFamily.UINT32
            canonicalType == "Int64" -> InterfacePropertyRuleFamily.INT64
            canonicalType == "UInt64" -> InterfacePropertyRuleFamily.UINT64
            else -> null
        }
    }

    fun interfaceSetterRuleFamily(type: String, isObjectType: Boolean): InterfacePropertyRuleFamily? {
        val canonicalType = canonicalWinRtSpecialType(type)
        return when {
            isObjectType -> InterfacePropertyRuleFamily.OBJECT
            canonicalType == "String" -> InterfacePropertyRuleFamily.STRING
            canonicalType == "UInt8" -> InterfacePropertyRuleFamily.UINT8
            canonicalType == "Int16" -> InterfacePropertyRuleFamily.INT16
            canonicalType == "UInt16" -> InterfacePropertyRuleFamily.UINT16
            canonicalType == "Char16" -> InterfacePropertyRuleFamily.CHAR16
            canonicalType == "Float32" -> InterfacePropertyRuleFamily.FLOAT32
            canonicalType == "Boolean" -> InterfacePropertyRuleFamily.BOOLEAN
            canonicalType == "Float64" -> InterfacePropertyRuleFamily.FLOAT64
            canonicalType == "HResult" -> InterfacePropertyRuleFamily.HRESULT
            canonicalType == "Int32" -> InterfacePropertyRuleFamily.INT32
            canonicalType == "UInt32" -> InterfacePropertyRuleFamily.UINT32
            canonicalType == "Int64" -> InterfacePropertyRuleFamily.INT64
            canonicalType == "UInt64" -> InterfacePropertyRuleFamily.UINT64
            else -> null
        }
    }

    fun getterRuleFamily(type: String): RuntimePropertyGetterRuleFamily? = getterRules[canonicalWinRtSpecialType(type)]

    fun setterRuleFamily(type: String): RuntimePropertySetterRuleFamily? = setterRules[canonicalWinRtSpecialType(type)]
}
