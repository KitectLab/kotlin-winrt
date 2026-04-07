package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isObjectPassArrayParameter(): Boolean =
    stripArraySuffix(type)?.let(::canonicalWinRtSpecialType) == "Object" &&
        arrayParameterCategory() == WinRtArrayParameterCategory.PASS_ARRAY

internal fun WinMdMethod.isObjectPassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isStandardPassArrayMethod(WinMdParameter::isObjectPassArrayParameter, supportsObjectReturn)

internal fun WinMdMethod.isObjectReceiveArrayReturnMethod(): Boolean =
    stripArraySuffix(returnType)?.let(::canonicalWinRtSpecialType) == "Object" &&
        arrayReturnCategory() == WinRtArrayParameterCategory.RECEIVE_ARRAY &&
        parameters.count(WinMdParameter::isObjectPassArrayParameter) <= 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || parameter.isObjectPassArrayParameter() }

internal fun objectReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = passArrayAbiArguments(
    parameters = parameters,
    lowerArgument = lowerArgument,
    isPassArrayParameter = WinMdParameter::isObjectPassArrayParameter,
) { parameterName ->
    CodeBlock.of("Array(%N.size) { index -> %N[index].pointer }", parameterName, parameterName)
}

internal fun objectPassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = objectReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun objectReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = receiveArrayReturnExpression(
    member = PoetSymbols.invokeObjectReceiveArrayMethodMember,
    vtableIndex = vtableIndex,
    abiArguments = abiArguments,
    suffixFormat = ").getOrThrow().map { %T(it) }.toTypedArray()",
    PoetSymbols.inspectableClass,
)

private fun stripArraySuffix(typeName: String): String? =
    typeName.takeIf { it.endsWith("[]") }?.removeSuffix("[]")
