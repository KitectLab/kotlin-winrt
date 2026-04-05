package dev.winrt.winmd.parser

internal fun int64AbiArgumentExpression(argumentName: String, type: String): String =
    when (canonicalWinRtSpecialType(type)) {
        "DateTime" ->
            "(((${argumentName}.epochSeconds * 10000000L) + (${argumentName}.nanosecondsOfSecond / 100)) + $WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET)"
        "TimeSpan" -> "(${argumentName}.inWholeNanoseconds / 100)"
        else -> "${argumentName}.value"
    }
