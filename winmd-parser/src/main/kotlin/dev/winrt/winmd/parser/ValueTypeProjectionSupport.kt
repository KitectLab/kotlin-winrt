package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

private val propertyValueScalarTypes = setOf("UInt8", "Int16", "UInt16", "Char16")

internal fun iReferenceInnerType(type: String): String? {
    val normalizedType = type.trim()
    val rawType = normalizedType.substringBefore('<').substringAfterLast('.').substringBefore('`')
    if (rawType != "IReference" || '<' !in normalizedType || !normalizedType.endsWith(">")) {
        return null
    }
    return normalizedType.substringAfter('<').substringBeforeLast('>').trim()
}

internal fun propertyValueGetterName(
    type: String,
    currentNamespace: String,
    typeRegistry: TypeRegistry,
): String? = when {
    typeRegistry.isStructType(type.trim(), currentNamespace) -> when (type.trim().substringAfterLast('.').substringBefore('`')) {
        "Point" -> "getPoint"
        "Size" -> "getSize"
        "Rect" -> "getRect"
        else -> null
    }
    else -> when (canonicalWinRtSpecialType(type.trim())) {
        "String" -> "getString"
        "Boolean" -> "getBoolean"
        "UInt8" -> "getUInt8"
        "Int16" -> "getInt16"
        "UInt16" -> "getUInt16"
        "Char16" -> "getChar16"
        "Int32" -> "getInt32"
        "UInt32" -> "getUInt32"
        "Int64" -> "getInt64"
        "UInt64" -> "getUInt64"
        "Float32" -> "getSingle"
        "Float64" -> "getDouble"
        "Guid" -> "getGuid"
        "DateTime" -> "getDateTime"
        "TimeSpan" -> "getTimeSpan"
        else -> null
    }
}

internal fun propertyValueFactoryMethodName(
    type: String,
    currentNamespace: String,
    typeRegistry: TypeRegistry,
): String? = when {
    typeRegistry.isStructType(type.trim(), currentNamespace) -> when (type.trim().substringAfterLast('.').substringBefore('`')) {
        "Point" -> "createPoint"
        "Size" -> "createSize"
        "Rect" -> "createRect"
        else -> null
    }
    else -> when (canonicalWinRtSpecialType(type.trim())) {
        "String" -> "createString"
        "Boolean" -> "createBoolean"
        "UInt8" -> "createUInt8"
        "Int16" -> "createInt16"
        "UInt16" -> "createUInt16"
        "Char16" -> "createChar16"
        "Int32" -> "createInt32"
        "UInt32" -> "createUInt32"
        "Int64" -> "createInt64"
        "UInt64" -> "createUInt64"
        "Float32" -> "createSingle"
        "Float64" -> "createDouble"
        "Guid" -> "createGuid"
        "DateTime" -> "createDateTime"
        "TimeSpan" -> "createTimeSpan"
        else -> null
    }
}

internal fun supportsIReferenceValueProjection(
    type: String,
    currentNamespace: String,
    typeRegistry: TypeRegistry,
): Boolean {
    val innerType = iReferenceInnerType(type) ?: return false
    return propertyValueGetterName(innerType, currentNamespace, typeRegistry) != null &&
        propertyValueFactoryMethodName(innerType, currentNamespace, typeRegistry) != null
}

internal fun supportsGenericIReferenceStructProjection(
    type: String,
    currentNamespace: String,
    typeRegistry: TypeRegistry,
): Boolean {
    val innerType = iReferenceInnerType(type) ?: return false
    return typeRegistry.isStructType(innerType, currentNamespace) &&
        !supportsIReferenceValueProjection(type, currentNamespace, typeRegistry)
}

internal fun supportsGenericIReferenceEnumProjection(
    type: String,
    currentNamespace: String,
    typeRegistry: TypeRegistry,
): Boolean {
    val innerType = iReferenceInnerType(type) ?: return false
    return typeRegistry.isEnumType(innerType, currentNamespace)
}

internal class ValueTypeProjectionSupport(
    private val typeNameMapper: TypeNameMapper,
    private val typeRegistry: TypeRegistry,
) {
    private val winRtSignatureMapper = WinRtSignatureMapper(typeRegistry)
    private val winRtProjectionTypeMapper = WinRtProjectionTypeMapper()

    fun supportsSmallScalarProjection(type: String): Boolean = canonicalWinRtSpecialType(type.trim()) in propertyValueScalarTypes

    fun structDefaultValue(type: String, currentNamespace: String): CodeBlock {
        val mappedType = typeNameMapper.mapTypeName(type, currentNamespace)
        return CodeBlock.of(
            "%T.fromAbi(%T(%T.ABI_LAYOUT, ByteArray(%T.ABI_LAYOUT.byteSize)))",
            mappedType,
            PoetSymbols.comStructValueClass,
            mappedType,
            mappedType,
        )
    }

    fun structReturnExpression(
        type: String,
        currentNamespace: String,
        abiCall: CodeBlock,
        genericParameters: Set<String> = emptySet(),
    ): CodeBlock {
        val mappedType = typeNameMapper.mapTypeName(type, currentNamespace, genericParameters)
        return CodeBlock.of("%T.fromAbi(%L)", mappedType, abiCall)
    }

    fun nullableValueReturnExpression(
        referenceType: String,
        currentNamespace: String,
        abiCall: CodeBlock,
    ): CodeBlock? {
        val innerType = iReferenceInnerType(referenceType) ?: return null
        val getterName = propertyValueGetterName(innerType, currentNamespace, typeRegistry) ?: return null
        return CodeBlock.of(
            "%L.let { if (it.isNull) null else %T.from(%T(it)).%L() }",
            abiCall,
            PoetSymbols.iPropertyValueClass,
            PoetSymbols.inspectableClass,
            getterName,
        )
    }

    fun nullableValuePointerExpression(
        referenceType: String,
        currentNamespace: String,
        valueExpression: String,
    ): CodeBlock? {
        val innerType = iReferenceInnerType(referenceType) ?: return null
        val factoryMethodName = propertyValueFactoryMethodName(innerType, currentNamespace, typeRegistry) ?: return null
        return CodeBlock.of(
            "if (%N == null) %T.NULL else %T.%L(%N).pointer",
            valueExpression,
            PoetSymbols.comPtrClass,
            PoetSymbols.propertyValueClass,
            factoryMethodName,
            valueExpression,
        )
    }

    fun genericStructReferenceReturnExpression(
        referenceType: String,
        currentNamespace: String,
        abiCall: CodeBlock,
    ): CodeBlock? {
        if (!supportsGenericIReferenceStructProjection(referenceType, currentNamespace, typeRegistry)) {
            return null
        }
        val innerType = iReferenceInnerType(referenceType) ?: return null
        val mappedInnerType = typeNameMapper.mapTypeName(innerType, currentNamespace)
        return CodeBlock.of(
            "%L.let { if (it.isNull) null else %T.from<%T>(%T(it), %S, %S).let { reference -> %T.fromAbi(%T.invokeStructMethodWithArgs(reference.pointer, 6, %T.ABI_LAYOUT).getOrThrow()) } }",
            abiCall,
            PoetSymbols.winRtIReferenceClass,
            mappedInnerType,
            PoetSymbols.inspectableClass,
            winRtSignatureMapper.signatureFor(innerType, currentNamespace),
            winRtProjectionTypeMapper.projectionTypeKeyFor(innerType, currentNamespace),
            mappedInnerType,
            PoetSymbols.platformComInteropClass,
            mappedInnerType,
        )
    }

    fun genericEnumReferenceReturnExpression(
        referenceType: String,
        currentNamespace: String,
        abiCall: CodeBlock,
    ): CodeBlock? {
        if (!supportsGenericIReferenceEnumProjection(referenceType, currentNamespace, typeRegistry)) {
            return null
        }
        val innerType = iReferenceInnerType(referenceType) ?: return null
        val mappedInnerType = typeNameMapper.mapTypeName(innerType, currentNamespace)
        val underlyingType = enumUnderlyingTypeOrDefault(typeRegistry, innerType, currentNamespace)
        return CodeBlock.of(
            "%L.let { if (it.isNull) null else %T.from<%T>(%T(it), %S, %S).let { reference -> %T.fromValue(%L) } }",
            abiCall,
            PoetSymbols.winRtIReferenceClass,
            mappedInnerType,
            PoetSymbols.inspectableClass,
            winRtSignatureMapper.signatureFor(innerType, currentNamespace),
            winRtProjectionTypeMapper.projectionTypeKeyFor(innerType, currentNamespace),
            mappedInnerType,
            enumGetterAbiCall(underlyingType, 6, "reference.pointer"),
        )
    }

    private fun genericReferencePointerExpression(
        referenceType: String,
        currentNamespace: String,
        valueExpression: String,
    ): CodeBlock {
        return CodeBlock.of(
            "if (%N == null) %T.NULL else %M(%T(%N), %S, %S)",
            valueExpression,
            PoetSymbols.comPtrClass,
            PoetSymbols.projectedObjectArgumentPointerMember,
            PoetSymbols.iReferenceClass,
            valueExpression,
            winRtProjectionTypeMapper.projectionTypeKeyFor(referenceType, currentNamespace),
            winRtSignatureMapper.signatureFor(referenceType, currentNamespace),
        )
    }

    fun genericStructReferencePointerExpression(
        referenceType: String,
        currentNamespace: String,
        valueExpression: String,
    ): CodeBlock? {
        if (!supportsGenericIReferenceStructProjection(referenceType, currentNamespace, typeRegistry)) {
            return null
        }
        return genericReferencePointerExpression(referenceType, currentNamespace, valueExpression)
    }

    fun genericEnumReferencePointerExpression(
        referenceType: String,
        currentNamespace: String,
        valueExpression: String,
    ): CodeBlock? {
        if (!supportsGenericIReferenceEnumProjection(referenceType, currentNamespace, typeRegistry)) {
            return null
        }
        return genericReferencePointerExpression(referenceType, currentNamespace, valueExpression)
    }

    fun smallScalarDefaultValue(type: String, currentNamespace: String): CodeBlock =
        typeNameMapper.defaultValueFor(typeNameMapper.mapTypeName(type, currentNamespace))

    fun smallScalarReturnExpression(
        type: String,
        abiCall: CodeBlock,
    ): CodeBlock? {
        return when (canonicalWinRtSpecialType(type.trim())) {
            "UInt8",
            "Int16",
            "UInt16",
            "Char16",
            -> abiCall
            else -> null
        }
    }

    fun smallScalarAbiCall(
        type: String,
        vtableIndex: Int,
        arguments: List<CodeBlock>,
        pointerExpression: String = "pointer",
    ): CodeBlock? {
        val resultKindName = when (canonicalWinRtSpecialType(type.trim())) {
            "UInt8" -> "UINT8"
            "Int16" -> "INT16"
            "UInt16" -> "UINT16"
            "Char16" -> "CHAR16"
            else -> return null
        }
        val extractor = when (canonicalWinRtSpecialType(type.trim())) {
            "UInt8" -> PoetSymbols.requireUInt8Member
            "Int16" -> PoetSymbols.requireInt16Member
            "UInt16" -> PoetSymbols.requireUInt16Member
            "Char16" -> PoetSymbols.requireChar16Member
            else -> return null
        }
        return CodeBlock.builder()
            .add("%T.invokeMethodWithResultKind(%L, %L, %T.%L", PoetSymbols.platformComInteropClass, pointerExpression, vtableIndex, PoetSymbols.comMethodResultKindClass, resultKindName)
            .apply {
                arguments.forEach { add(", %L", it) }
            }
            .add(").getOrThrow().%M()", extractor)
            .build()
    }

    fun requiresValueAwareGenericAbi(type: String, currentNamespace: String): Boolean {
        return typeRegistry.isStructType(type, currentNamespace) ||
            supportsSmallScalarProjection(type) ||
            supportsGenericIReferenceStructProjection(type, currentNamespace, typeRegistry) ||
            supportsGenericIReferenceEnumProjection(type, currentNamespace, typeRegistry) ||
            supportsIReferenceValueProjection(type, currentNamespace, typeRegistry)
    }

    fun canLowerGenericAbiArgument(
        type: String,
        currentNamespace: String,
        supportsObjectType: (String) -> Boolean,
    ): Boolean {
        return when {
            supportsIReferenceValueProjection(type, currentNamespace, typeRegistry) -> true
            supportsGenericIReferenceEnumProjection(type, currentNamespace, typeRegistry) -> true
            typeRegistry.isStructType(type, currentNamespace) -> true
            typeRegistry.isEnumType(type, currentNamespace) -> {
                enumUnderlyingTypeOrDefault(typeRegistry, type, currentNamespace) in setOf("Int32", "UInt32")
            }
            supportsObjectType(type) -> true
            else -> when (canonicalWinRtSpecialType(type)) {
                "String",
                "UInt8",
                "Int16",
                "UInt16",
                "Char16",
                "Int32",
                "UInt32",
                "Boolean",
                "Float32",
                "Float64",
                "Int64",
                "UInt64",
                "DateTime",
                "TimeSpan",
                "EventRegistrationToken",
                -> true
                else -> false
            }
        }
    }

    fun lowerGenericAbiArgument(
        type: String,
        currentNamespace: String,
        argumentName: String,
        supportsObjectType: (String) -> Boolean,
        lowerObjectArgument: (String, String) -> CodeBlock,
    ): CodeBlock? {
        return when {
            supportsIReferenceValueProjection(type, currentNamespace, typeRegistry) ->
                nullableValuePointerExpression(type, currentNamespace, argumentName)
            supportsGenericIReferenceEnumProjection(type, currentNamespace, typeRegistry) ->
                genericEnumReferencePointerExpression(type, currentNamespace, argumentName)
            supportsGenericIReferenceStructProjection(type, currentNamespace, typeRegistry) ->
                genericStructReferencePointerExpression(type, currentNamespace, argumentName)
            typeRegistry.isStructType(type, currentNamespace) -> CodeBlock.of("%N.toAbi()", argumentName)
            typeRegistry.isEnumType(type, currentNamespace) -> when (enumUnderlyingTypeOrDefault(typeRegistry, type, currentNamespace)) {
                "Int32",
                "UInt32",
                -> CodeBlock.of("%N.value", argumentName)
                else -> null
            }
            supportsObjectType(type) -> lowerObjectArgument(argumentName, type)
            else -> when (canonicalWinRtSpecialType(type)) {
                "String" -> CodeBlock.of("%N", argumentName)
                "UInt8",
                "Int16",
                "UInt16",
                "Char16",
                -> CodeBlock.of("%N", argumentName)
                "Int32",
                "UInt32",
                "Boolean",
                "Float32",
                "Float64",
                "UInt64",
                -> CodeBlock.of("%N.value", argumentName)
                "Int64",
                "DateTime",
                "TimeSpan",
                "EventRegistrationToken",
                -> CodeBlock.of("%L", int64AbiArgumentExpression(argumentName, type))
                else -> null
            }
        }
    }

    fun invokeUnitMethodWithArgs(
        vtableIndex: Int,
        arguments: List<CodeBlock>,
        pointerExpression: String = "pointer",
    ): CodeBlock = invokeVarargMethod("invokeUnitMethodWithArgs", pointerExpression, vtableIndex, arguments = arguments)

    fun invokeObjectMethodWithArgs(
        vtableIndex: Int,
        arguments: List<CodeBlock>,
        pointerExpression: String = "pointer",
    ): CodeBlock = invokeVarargMethod("invokeObjectMethodWithArgs", pointerExpression, vtableIndex, arguments = arguments)

    fun invokeStructMethodWithArgs(
        vtableIndex: Int,
        structType: TypeName,
        arguments: List<CodeBlock>,
        pointerExpression: String = "pointer",
    ): CodeBlock = invokeVarargMethod(
        methodName = "invokeStructMethodWithArgs",
        pointerExpression = pointerExpression,
        vtableIndex = vtableIndex,
        prefixArguments = listOf(CodeBlock.of("%T.ABI_LAYOUT", structType)),
        arguments = arguments,
    )

    private fun invokeVarargMethod(
        methodName: String,
        pointerExpression: String,
        vtableIndex: Int,
        prefixArguments: List<CodeBlock> = emptyList(),
        arguments: List<CodeBlock> = emptyList(),
    ): CodeBlock {
        return CodeBlock.builder()
            .add("%T.$methodName(%L, %L", PoetSymbols.platformComInteropClass, pointerExpression, vtableIndex)
            .apply {
                prefixArguments.forEach { add(", %L", it) }
                arguments.forEach { add(", %L", it) }
            }
            .add(").getOrThrow()")
            .build()
    }
}
