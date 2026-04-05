package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter
import dev.winrt.winmd.plugin.stripValueTypeNameMarker

private val supportedStructReceiveArrayTypes = mapOf(
    "Point" to "Windows.Foundation.Point",
    "Size" to "Windows.Foundation.Size",
    "Rect" to "Windows.Foundation.Rect",
)

internal fun WinMdParameter.isSupportedStructPassArrayParameter(
    currentNamespace: String,
    typeRegistry: TypeRegistry,
    expectedElementType: String? = null,
): Boolean {
    if (arrayParameterCategory() != WinRtArrayParameterCategory.PASS_ARRAY) {
        return false
    }
    val elementType = supportedStructArrayElementType(type) ?: return false
    if (expectedElementType != null && elementType != expectedElementType) {
        return false
    }
    return true
}

internal fun WinMdMethod.supportedStructReceiveArrayElementType(
    currentNamespace: String,
    typeRegistry: TypeRegistry,
): String? {
    if (arrayReturnCategory() != WinRtArrayParameterCategory.RECEIVE_ARRAY) {
        return null
    }
    val elementType = supportedStructArrayElementType(returnType) ?: return null
    val passArrayCount = parameters.count { parameter ->
        parameter.isSupportedStructPassArrayParameter(currentNamespace, typeRegistry, elementType)
    }
    if (passArrayCount > 1) {
        return null
    }
    if (parameters.any { parameter ->
            parameter.type.isWinRtArrayType() &&
                !parameter.isSupportedStructPassArrayParameter(currentNamespace, typeRegistry, elementType)
        }
    ) {
        return null
    }
    return elementType
}

internal fun structReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    currentNamespace: String,
    typeRegistry: TypeRegistry,
    expectedElementType: String,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = buildList {
    parameters.forEach { parameter ->
        val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
        if (parameter.isSupportedStructPassArrayParameter(currentNamespace, typeRegistry, expectedElementType)) {
            add(CodeBlock.of("%N.size", parameterName))
            add(structPassArrayBufferExpression(parameterName, expectedElementType))
        } else {
            add(lowerArgument(parameter) ?: return null)
        }
    }
}

internal fun WinMdMethod.supportedStructPassArrayElementType(
    currentNamespace: String,
    typeRegistry: TypeRegistry,
): String? {
    if (arrayReturnCategory() != null) {
        return null
    }
    val passArrayParameters = parameters.filter { parameter ->
        parameter.isSupportedStructPassArrayParameter(currentNamespace, typeRegistry)
    }
    if (passArrayParameters.size != 1) {
        return null
    }
    if (parameters.any { parameter ->
            parameter.type.isWinRtArrayType() &&
                !parameter.isSupportedStructPassArrayParameter(currentNamespace, typeRegistry)
        }
    ) {
        return null
    }
    return supportedStructArrayElementType(passArrayParameters.single().type)
}

internal fun structPassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    currentNamespace: String,
    typeRegistry: TypeRegistry,
    expectedElementType: String,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = structReceiveArrayAbiArguments(parameters, currentNamespace, typeRegistry, expectedElementType, lowerArgument)

internal fun structReceiveArrayReturnExpression(
    vtableIndex: Int,
    structType: TypeName,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = CodeBlock.builder()
    .add("%M(pointer, %L, %T.ABI_LAYOUT", PoetSymbols.invokeStructReceiveArrayMethodMember, vtableIndex, structType)
    .apply {
        abiArguments.forEach { argument ->
            add(", %L", argument)
        }
    }
    .add(").getOrThrow().map { %T.fromAbi(it) }.toTypedArray()", structType)
    .build()

private fun structPassArrayBufferExpression(
    parameterName: String,
    elementType: String,
): CodeBlock {
    return when (elementType.substringAfterLast('.').substringBefore('`')) {
        "Point" -> CodeBlock.of(
            "FloatArray(%N.size * 2) { index -> val item = %N[index / 2]; if (index %% 2 == 0) item.x else item.y }",
            parameterName,
            parameterName,
        )
        "Size" -> CodeBlock.of(
            "FloatArray(%N.size * 2) { index -> val item = %N[index / 2]; if (index %% 2 == 0) item.width else item.height }",
            parameterName,
            parameterName,
        )
        "Rect" -> CodeBlock.of(
            "FloatArray(%N.size * 4) { index -> val item = %N[index / 4]; when (index %% 4) { 0 -> item.x; 1 -> item.y; 2 -> item.width; else -> item.height } }",
            parameterName,
            parameterName,
        )
        else -> error("Unsupported struct pass-array element type: $elementType")
    }
}

private fun supportedStructArrayElementType(typeName: String): String? {
    val elementType = stripValueTypeNameMarker(typeName)
        .takeIf { it.endsWith("[]") }
        ?.removeSuffix("[]")
        ?: return null
    return when (elementType) {
        "Point" -> supportedStructReceiveArrayTypes.getValue("Point")
        "Size" -> supportedStructReceiveArrayTypes.getValue("Size")
        "Rect" -> supportedStructReceiveArrayTypes.getValue("Rect")
        else -> supportedStructReceiveArrayTypes[elementType.substringAfterLast('.').substringBefore('`')]
            ?.takeIf { supportedType -> elementType == supportedType }
    }
}
