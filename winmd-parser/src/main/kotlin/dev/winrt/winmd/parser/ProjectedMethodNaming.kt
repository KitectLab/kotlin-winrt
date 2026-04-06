package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdMethod

internal fun WinMdMethod.isIndexOfOutUInt32Method(): Boolean {
    val indexParameter = parameters.getOrNull(1) ?: return false
    return name == "IndexOf" &&
        canonicalWinRtSpecialType(returnType) == "Boolean" &&
        parameters.size == 2 &&
        canonicalWinRtSpecialType(indexParameter.type) == "UInt32" &&
        indexParameter.byRef &&
        indexParameter.isOut
}

internal fun projectedMethodName(method: WinMdMethod): String =
    when {
        method.name == "ToString" &&
            canonicalWinRtSpecialType(method.returnType) == "String" &&
            method.parameters.isEmpty() -> "toString"
        method.isIndexOfOutUInt32Method() -> "winRtIndexOf"
        else -> method.name.replaceFirstChar(Char::lowercase)
    }
