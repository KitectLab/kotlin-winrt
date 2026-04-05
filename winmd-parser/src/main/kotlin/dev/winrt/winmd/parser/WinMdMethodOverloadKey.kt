package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter

internal fun WinMdParameter.overloadKeyFragment(): String =
    buildString {
        append(type)
        if (byRef) {
            append('&')
        }
        if (isIn) {
            append(":in")
        }
        if (isOut) {
            append(":out")
        }
    }

internal fun WinMdMethod.overloadKey(renderedName: String = name): String =
    buildString {
        append(renderedName)
        append('(')
        append(parameters.joinToString(",") { parameter -> parameter.overloadKeyFragment() })
        append(')')
    }
