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

    fun findType(typeName: String, currentNamespace: String? = null): WinMdType? {
        val qualifiedName = resolveQualifiedName(typeName, currentNamespace)
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

    private fun resolveQualifiedName(typeName: String, currentNamespace: String?): String {
        return canonicalQualifiedName(
            when {
                '.' in typeName -> typeName
                currentNamespace != null -> "$currentNamespace.$typeName"
                else -> typeName
            },
        )
    }
}
