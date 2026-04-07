package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.isExactPassArrayParameter(elementType: String): Boolean =
    type == "$elementType[]" && arrayParameterCategory() == WinRtArrayParameterCategory.PASS_ARRAY

internal fun WinMdMethod.isStandardPassArrayMethod(
    isPassArrayParameter: (WinMdParameter) -> Boolean,
    supportsObjectReturn: (String) -> Boolean,
): Boolean =
    arrayReturnCategory() == null &&
        parameters.count(isPassArrayParameter) == 1 &&
        parameters.all { parameter -> !parameter.type.isWinRtArrayType() || isPassArrayParameter(parameter) } &&
        (returnType == "Unit" || supportsObjectReturn(returnType))

internal fun WinMdMethod.isStandardReceiveArrayReturnMethod(
    elementType: String,
    isPassArrayParameter: ((WinMdParameter) -> Boolean)? = null,
): Boolean {
    if (returnType != "$elementType[]" || arrayReturnCategory() != WinRtArrayParameterCategory.RECEIVE_ARRAY) {
        return false
    }
    return if (isPassArrayParameter == null) {
        parameters.none { parameter -> parameter.type.isWinRtArrayType() }
    } else {
        parameters.count(isPassArrayParameter) <= 1 &&
            parameters.all { parameter ->
                !parameter.type.isWinRtArrayType() || isPassArrayParameter(parameter)
            }
    }
}

internal fun loweredAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
): List<CodeBlock>? = parameters.map { parameter -> lowerArgument(parameter) ?: return null }

internal fun passArrayAbiArguments(
    parameters: List<WinMdParameter>,
    lowerArgument: (WinMdParameter) -> CodeBlock?,
    isPassArrayParameter: (WinMdParameter) -> Boolean,
    passArrayBufferExpression: (String) -> CodeBlock,
): List<CodeBlock>? = buildList {
    parameters.forEach { parameter ->
        val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
        if (isPassArrayParameter(parameter)) {
            add(CodeBlock.of("%N.size", parameterName))
            add(passArrayBufferExpression(parameterName))
        } else {
            add(lowerArgument(parameter) ?: return null)
        }
    }
}

internal fun receiveArrayReturnExpression(
    member: MemberName,
    vtableIndex: Int,
    abiArguments: List<CodeBlock> = emptyList(),
    suffixFormat: String,
    vararg suffixArgs: Any,
): CodeBlock = CodeBlock.builder()
    .add("%M(pointer, %L", member, vtableIndex)
    .apply {
        abiArguments.forEach { argument ->
            add(", %L", argument)
        }
    }
    .add(suffixFormat, *suffixArgs)
    .build()
