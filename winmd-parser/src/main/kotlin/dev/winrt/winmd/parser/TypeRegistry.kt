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

    fun findRuntimeClassStaticsType(typeName: String, currentNamespace: String): WinMdType? {
        val runtimeClass = findType(typeName, currentNamespace) ?: return null
        if (runtimeClass.kind != WinMdTypeKind.RuntimeClass) {
            return null
        }
        return findType("I${runtimeClass.name}Statics", runtimeClass.namespace)
    }

    fun findDefaultInterfaceType(typeName: String, currentNamespace: String): WinMdType? {
        val runtimeClass = findType(typeName, currentNamespace) ?: return null
        val defaultInterfaceName = runtimeClass.defaultInterface ?: return null
        return findType(defaultInterfaceName, runtimeClass.namespace)
    }

    fun findImplementedInterfaceTypes(typeName: String, currentNamespace: String): List<WinMdType> {
        val runtimeClass = findType(typeName, currentNamespace) ?: return emptyList()
        return runtimeClass.baseInterfaces
            .asSequence()
            .filterNot { it == runtimeClass.defaultInterface }
            .mapNotNull { findType(it, runtimeClass.namespace) }
            .toList()
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
