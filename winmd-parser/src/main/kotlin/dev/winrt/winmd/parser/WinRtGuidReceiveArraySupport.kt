package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isGuidPassArrayParameter(): Boolean =
    isExactPassArrayParameter("Guid")

internal fun WinMdMethod.isGuidPassArrayMethod(
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    isStandardPassArrayMethod(WinMdParameter::isGuidPassArrayParameter, supportsObjectReturn)

internal fun WinMdMethod.isGuidReceiveArrayReturnMethod(): Boolean =
    isStandardReceiveArrayReturnMethod("Guid", WinMdParameter::isGuidPassArrayParameter)

internal fun guidReceiveArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = passArrayAbiArguments(
    parameters = parameters,
    lowerArgument = lowerArgument,
    isPassArrayParameter = WinMdParameter::isGuidPassArrayParameter,
) { parameterName ->
    CodeBlock.of(
        "ByteArray(%N.size * 16) { index -> val guid = guidOf(%N[index / 16].toString()); when (index %% 16) { 0 -> guid.data1.toByte(); 1 -> (guid.data1 shr 8).toByte(); 2 -> (guid.data1 shr 16).toByte(); 3 -> (guid.data1 shr 24).toByte(); 4 -> guid.data2.toByte(); 5 -> (guid.data2.toInt() shr 8).toByte(); 6 -> guid.data3.toByte(); 7 -> (guid.data3.toInt() shr 8).toByte(); else -> guid.data4[index %% 16 - 8] } }",
        parameterName,
        parameterName,
    )
}

internal fun guidPassArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = guidReceiveArrayAbiArguments(parameters, lowerArgument)

internal fun guidReceiveArrayReturnExpression(
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
): CodeBlock = receiveArrayReturnExpression(
    member = PoetSymbols.invokeGuidReceiveArrayMethodMember,
    vtableIndex = vtableIndex,
    abiArguments = abiArguments,
    suffixFormat = ").getOrThrow().map { %T.parse(it.toString()) }.toTypedArray()",
    PoetSymbols.guidValueClass,
)
