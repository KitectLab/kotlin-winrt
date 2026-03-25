package dev.winrt.winmd.parser

private val kotlinIdentifierPattern = Regex("[A-Za-z_][A-Za-z0-9_]*")

internal fun isKotlinIdentifier(name: String): Boolean {
    return kotlinIdentifierPattern.matches(name)
}
