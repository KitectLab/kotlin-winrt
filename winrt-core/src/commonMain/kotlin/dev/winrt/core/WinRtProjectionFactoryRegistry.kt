package dev.winrt.core

object WinRtProjectionFactoryRegistry {
    private val factories: MutableMap<String, (Inspectable) -> Any> = linkedMapOf()
    private val openFactories: MutableMap<String, (Inspectable, List<String>, List<String>) -> Any> = linkedMapOf()

    fun registerFactory(
        projectionTypeKey: String,
        factory: (Inspectable) -> Any,
    ) {
        if (projectionTypeKey !in factories) {
            factories[projectionTypeKey] = factory
        }
    }

    fun registerOpenFactory(
        rawProjectionTypeKey: String,
        factory: (Inspectable, List<String>, List<String>) -> Any,
    ) {
        if (rawProjectionTypeKey !in openFactories) {
            openFactories[rawProjectionTypeKey] = factory
        }
    }

    fun create(
        inspectable: Inspectable,
        projectionTypeKey: String,
        signature: String,
    ): Any? {
        factories[projectionTypeKey]?.let { factory ->
            return factory(inspectable)
        }
        val rawProjectionTypeKey = projectionTypeKey.substringBefore('<')
        val projectionTypeArguments = splitProjectionTypeArguments(projectionTypeKey)
        val signatureArguments = splitSignatureArguments(signature)
        return openFactories[rawProjectionTypeKey]?.invoke(inspectable, signatureArguments, projectionTypeArguments)
    }

    internal fun resetForTests() {
        factories.clear()
        openFactories.clear()
    }

    private fun splitProjectionTypeArguments(projectionTypeKey: String): List<String> {
        val genericStart = projectionTypeKey.indexOf('<')
        if (genericStart < 0 || !projectionTypeKey.endsWith(">")) {
            return emptyList()
        }
        return splitTopLevel(
            projectionTypeKey.substring(genericStart + 1, projectionTypeKey.length - 1),
            ',',
        )
    }

    private fun splitSignatureArguments(signature: String): List<String> {
        return when {
            signature.startsWith("pinterface(") && signature.endsWith(")") -> {
                val content = signature.removePrefix("pinterface(").removeSuffix(")")
                splitTopLevel(content, ';').drop(1)
            }
            signature.startsWith("delegate(") && signature.endsWith(")") ->
                splitSignatureArguments(signature.removePrefix("delegate(").removeSuffix(")"))
            signature.startsWith("rc(") && signature.endsWith(")") -> {
                val content = signature.removePrefix("rc(").removeSuffix(")")
                splitTopLevel(content, ';')
                    .getOrNull(1)
                    ?.let(::splitSignatureArguments)
                    ?: emptyList()
            }
            else -> emptyList()
        }
    }

    private fun splitTopLevel(
        source: String,
        separator: Char,
    ): List<String> {
        if (source.isBlank()) {
            return emptyList()
        }
        val parts = mutableListOf<String>()
        var parenthesisDepth = 0
        var angleDepth = 0
        var start = 0
        source.forEachIndexed { index, char ->
            when (char) {
                '(' -> parenthesisDepth += 1
                ')' -> parenthesisDepth -= 1
                '<' -> angleDepth += 1
                '>' -> angleDepth -= 1
                separator -> if (parenthesisDepth == 0 && angleDepth == 0) {
                    parts += source.substring(start, index).trim()
                    start = index + 1
                }
            }
        }
        parts += source.substring(start).trim()
        return parts
    }
}
