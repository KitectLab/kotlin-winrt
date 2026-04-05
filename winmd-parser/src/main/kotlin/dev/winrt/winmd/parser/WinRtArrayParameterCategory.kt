package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal enum class WinRtArrayParameterCategory {
    PASS_ARRAY,
    FILL_ARRAY,
    RECEIVE_ARRAY,
}

internal fun String.isWinRtArrayType(): Boolean = endsWith("[]")

// Mirrors CsWinRT helpers.h get_param_category() for szarray parameters.
internal fun WinMdParameter.arrayParameterCategory(): WinRtArrayParameterCategory? {
    if (!type.isWinRtArrayType()) {
        return null
    }
    return when {
        isIn -> WinRtArrayParameterCategory.PASS_ARRAY
        byRef && isOut -> WinRtArrayParameterCategory.RECEIVE_ARRAY
        isOut -> WinRtArrayParameterCategory.FILL_ARRAY
        else -> null
    }
}

internal fun WinMdMethod.arrayReturnCategory(): WinRtArrayParameterCategory? =
    if (returnType.isWinRtArrayType()) {
        WinRtArrayParameterCategory.RECEIVE_ARRAY
    } else {
        null
    }

internal fun Iterable<WinMdParameter>.requiresArrayMarshaling(): Boolean =
    any { parameter -> parameter.type.isWinRtArrayType() }

internal fun WinMdMethod.requiresArrayMarshaling(): Boolean =
    arrayReturnCategory() != null || parameters.requiresArrayMarshaling()
