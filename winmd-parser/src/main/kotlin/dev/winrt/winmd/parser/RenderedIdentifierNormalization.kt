package dev.winrt.winmd.parser

private val renderedEscapedIdentifierPattern = Regex("`([A-Za-z_][A-Za-z0-9_]*)`")

private val kotlinHardKeywords = setOf(
    "as",
    "break",
    "class",
    "continue",
    "do",
    "else",
    "false",
    "for",
    "fun",
    "if",
    "in",
    "interface",
    "is",
    "null",
    "object",
    "package",
    "return",
    "super",
    "this",
    "throw",
    "true",
    "try",
    "typealias",
    "typeof",
    "val",
    "var",
    "when",
    "while",
)

internal fun normalizeRenderedIdentifiers(source: String): String {
    return renderedEscapedIdentifierPattern.replace(source) { match ->
        val identifier = match.groupValues[1]
        if (identifier in kotlinHardKeywords) match.value else identifier
    }
}
