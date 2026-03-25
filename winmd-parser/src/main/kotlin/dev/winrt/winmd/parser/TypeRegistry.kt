package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdTypeKind

internal class TypeRegistry(
    model: WinMdModel,
) {
    private val kindsByQualifiedName = model.namespaces
        .flatMap { namespace -> namespace.types.map { type -> "${type.namespace}.${type.name}" to type.kind } }
        .toMap()

    fun isEnumType(typeName: String, currentNamespace: String): Boolean {
        val qualifiedName = when {
            '.' in typeName -> typeName
            else -> "$currentNamespace.$typeName"
        }.substringBefore('<')
            .substringBefore('`')
            .removeSuffix("[]")
        return kindsByQualifiedName[qualifiedName] == WinMdTypeKind.Enum
    }
}
