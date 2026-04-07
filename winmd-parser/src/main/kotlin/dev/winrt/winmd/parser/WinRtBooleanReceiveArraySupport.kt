package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isBooleanPassArrayParameter(): Boolean =
    isExactPassArrayParameter("Boolean")

internal fun WinMdMethod.isBooleanPassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isStandardPassArrayMethod(WinMdParameter::isBooleanPassArrayParameter, supportsObjectReturn)

internal fun WinMdMethod.isBooleanReceiveArrayReturnMethod(): Boolean =
    isStandardReceiveArrayReturnMethod("Boolean", WinMdParameter::isBooleanPassArrayParameter)

internal fun booleanReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = passArrayAbiArguments(
    parameters = parameters,
    lowerArgument = lowerArgument,
    isPassArrayParameter = WinMdParameter::isBooleanPassArrayParameter,
) { parameterName ->
    CodeBlock.of(
        "ByteArray(%N.size) { index -> if (%N[index].value) 1.toByte() else 0.toByte() }",
        parameterName,
        parameterName,
    )
}

internal fun booleanPassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = booleanReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun booleanReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = receiveArrayReturnExpression(
    member = PoetSymbols.invokeBooleanReceiveArrayMethodMember,
    vtableIndex = vtableIndex,
    abiArguments = abiArguments,
    suffixFormat = ").getOrThrow().map { %T(it) }.toTypedArray()",
    PoetSymbols.winRtBooleanClass,
)
