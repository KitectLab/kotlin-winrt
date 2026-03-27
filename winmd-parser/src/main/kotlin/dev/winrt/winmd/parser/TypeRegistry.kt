package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind

internal class TypeRegistry(
    model: WinMdModel,
) {
    private val typesByQualifiedName = model.namespaces
        .flatMap { namespace ->
            namespace.types.map { type -> canonicalQualifiedName("${type.namespace}.${type.name}") to type }
        }
        .toMap()

    private val kindsByQualifiedName = model.namespaces
        .flatMap { namespace -> namespace.types.map { type -> "${type.namespace}.${type.name}" to type.kind } }
        .toMap()

    fun findType(typeName: String, currentNamespace: String? = null): WinMdType? {
        val qualifiedName = canonicalQualifiedName(
            when {
            '.' in typeName -> typeName
            currentNamespace != null -> "$currentNamespace.$typeName"
            else -> typeName
            },
        )
        return typesByQualifiedName[qualifiedName]
    }

    fun isEnumType(typeName: String, currentNamespace: String): Boolean {
        return findType(typeName, currentNamespace)?.kind == WinMdTypeKind.Enum
    }

    private fun canonicalQualifiedName(typeName: String): String {
        return typeName.substringBefore('<')
            .substringBefore('`')
            .removeSuffix("[]")
    }
}
