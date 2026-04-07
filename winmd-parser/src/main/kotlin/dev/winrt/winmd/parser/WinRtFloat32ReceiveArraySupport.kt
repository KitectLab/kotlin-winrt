package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isFloat32PassArrayParameter(): Boolean =
    isExactPassArrayParameter("Float32")

internal fun WinMdMethod.isFloat32PassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isStandardPassArrayMethod(WinMdParameter::isFloat32PassArrayParameter, supportsObjectReturn)

internal fun WinMdMethod.isFloat32ReceiveArrayReturnMethod(): Boolean =
    isStandardReceiveArrayReturnMethod("Float32", WinMdParameter::isFloat32PassArrayParameter)

internal fun float32ReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = passArrayAbiArguments(
    parameters = parameters,
    lowerArgument = lowerArgument,
    isPassArrayParameter = WinMdParameter::isFloat32PassArrayParameter,
) { parameterName ->
    CodeBlock.of("FloatArray(%N.size) { index -> %N[index].value }", parameterName, parameterName)
}

internal fun float32PassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = float32ReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun float32ReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = receiveArrayReturnExpression(
    member = PoetSymbols.invokeFloat32ReceiveArrayMethodMember,
    vtableIndex = vtableIndex,
    abiArguments = abiArguments,
    suffixFormat = ").getOrThrow().map { %T(it) }.toTypedArray()",
    PoetSymbols.float32Class,
)
