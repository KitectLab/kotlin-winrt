package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter
import dev.winrt.winmd.plugin.WinMdTypeKind
import dev.winrt.winmd.plugin.stripValueTypeNameMarker

internal fun WinMdParameter.isRuntimeClassPassArrayParameter(
    currentNamespace: String,
    typeRegistry: TypeRegistry,
    expectedElementType: String? = null,
): Boolean {
    if (arrayParameterCategory() != WinRtArrayParameterCategory.PASS_ARRAY) {
        return false
    }
    val elementType = stripValueTypeNameMarker(type).takeIf { it.endsWith("[]") }?.removeSuffix("[]") ?: return false
    if (expectedElementType != null && elementType != expectedElementType) {
        return false
    }
    return typeRegistry.findType(elementType, currentNamespace)?.kind == WinMdTypeKind.RuntimeClass
}

internal fun WinMdMethod.runtimeClassReceiveArrayElementType(
    currentNamespace: String,
    typeRegistry: TypeRegistry,
): String? {
    if (arrayReturnCategory() != WinRtArrayParameterCategory.RECEIVE_ARRAY) {
        return null
    }
    val elementType = stripValueTypeNameMarker(returnType).takeIf { it.endsWith("[]") }?.removeSuffix("[]") ?: return null
    if (typeRegistry.findType(elementType, currentNamespace)?.kind != WinMdTypeKind.RuntimeClass) {
        return null
    }
    val passArrayCount = parameters.count { parameter ->
        parameter.isRuntimeClassPassArrayParameter(currentNamespace, typeRegistry, elementType)
    }
    if (passArrayCount > 1) {
        return null
    }
    if (parameters.any { parameter ->
            parameter.type.isWinRtArrayType() &&
                !parameter.isRuntimeClassPassArrayParameter(currentNamespace, typeRegistry, elementType)
        }
    ) {
        return null
    }
    return elementType
}

internal fun runtimeClassReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    currentNamespace: String,
    typeRegistry: TypeRegistry,
    expectedElementType: String,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = buildList {
    parameters.forEach { parameter ->
        val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
        if (parameter.isRuntimeClassPassArrayParameter(currentNamespace, typeRegistry, expectedElementType)) {
            add(CodeBlock.of("%N.size", parameterName))
            add(
                CodeBlock.of(
                    "Array(%N.size) { index -> %N[index].pointer }",
                    parameterName,
                    parameterName,
                ),
            )
        } else {
            add(lowerArgument(parameter) ?: return null)
        }
    }
}

internal fun runtimeClassReceiveArrayReturnExpression(
    vtableIndex: Int,
    runtimeClassType: TypeName,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = CodeBlock.builder()
    .add("%M(pointer, %L", PoetSymbols.invokeObjectReceiveArrayMethodMember, vtableIndex)
    .apply {
        abiArguments.forEach { argument ->
            add(", %L", argument)
        }
    }
    .add(").getOrThrow().map { %T(it) }.toTypedArray()", runtimeClassType)
    .build()
