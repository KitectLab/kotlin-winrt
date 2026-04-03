package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind

internal class TypeRegistry(
    model: WinMdModel,
) {
    private val allTypes = model.namespaces.flatMap { it.types }
    private val typesByQualifiedName = model.namespaces
        .flatMap { namespace ->
            namespace.types.map { type -> canonicalQualifiedName("${type.namespace}.${type.name}") to type }
        }
        .toMap()
    private val runtimeImplementedInterfaces = model.namespaces
        .flatMap { namespace ->
            namespace.types
                .filter { it.kind == WinMdTypeKind.RuntimeClass }
                .flatMap { type ->
                    buildList {
                        type.defaultInterface?.let(::add)
                        addAll(type.implementedInterfaces)
                        addAll(type.baseInterfaces)
                    }
                }
        }
        .map(::canonicalQualifiedName)
        .toSet()

    fun findType(typeName: String, currentNamespace: String? = null): WinMdType? {
        val qualifiedName = resolveQualifiedName(typeName, currentNamespace)
        return typesByQualifiedName[qualifiedName]
    }

    fun isEnumType(typeName: String, currentNamespace: String): Boolean {
        return findType(typeName, currentNamespace)?.kind == WinMdTypeKind.Enum
    }

    fun findRuntimeClassStaticsType(typeName: String, currentNamespace: String): WinMdType? {
        return findRuntimeClassStaticsTypes(typeName, currentNamespace).firstOrNull()
    }

    fun findRuntimeClassStaticsTypes(typeName: String, currentNamespace: String): List<WinMdType> {
        return findRuntimeClassHelperTypes(typeName, currentNamespace, "Statics")
    }

    fun findRuntimeClassFactoryTypes(typeName: String, currentNamespace: String): List<WinMdType> {
        return findRuntimeClassHelperTypes(typeName, currentNamespace, "Factory")
    }

    fun findRuntimeClassOverridesTypes(typeName: String, currentNamespace: String): List<WinMdType> {
        return findRuntimeClassHelperTypes(typeName, currentNamespace, "Overrides")
    }

    private fun findRuntimeClassHelperTypes(typeName: String, currentNamespace: String, helperKind: String): List<WinMdType> {
        val runtimeClass = findType(typeName, currentNamespace) ?: return emptyList()
        if (runtimeClass.kind != WinMdTypeKind.RuntimeClass) {
            return emptyList()
        }
        val prefix = "I${runtimeClass.name}$helperKind"
        return allTypes
            .asSequence()
            .filter { type ->
                type.namespace == runtimeClass.namespace &&
                    type.kind == WinMdTypeKind.Interface &&
                    helperInterfaceOrder(type.name, prefix) != null
            }
            .sortedBy { type -> helperInterfaceOrder(type.name, prefix) }
            .toList()
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

    fun isRuntimeProjectedInterface(typeName: String, currentNamespace: String? = null): Boolean {
        val qualifiedName = resolveQualifiedName(typeName, currentNamespace)
        return qualifiedName in runtimeImplementedInterfaces
    }

    fun isRuntimeClassHelperInterface(typeName: String, currentNamespace: String): Boolean {
        val interfaceType = findType(typeName, currentNamespace) ?: return false
        if (interfaceType.kind != WinMdTypeKind.Interface) return false
        return allTypes
            .asSequence()
            .filter { type -> type.kind == WinMdTypeKind.RuntimeClass && type.namespace == interfaceType.namespace }
            .flatMap { runtimeClass ->
                sequenceOf(
                    findRuntimeClassStaticsTypes(runtimeClass.name, runtimeClass.namespace).asSequence(),
                    findRuntimeClassFactoryTypes(runtimeClass.name, runtimeClass.namespace).asSequence(),
                ).flatten()
            }
            .any { helperType -> helperType.name == interfaceType.name }
    }

    fun isVersionedRuntimeClassInterface(typeName: String, currentNamespace: String): Boolean {
        val interfaceType = findType(typeName, currentNamespace) ?: return false
        if (interfaceType.kind != WinMdTypeKind.Interface) return false
        if (!isRuntimeProjectedInterface(typeName, currentNamespace)) return false
        return allTypes.any { type ->
            type.kind == WinMdTypeKind.RuntimeClass &&
                type.namespace == interfaceType.namespace &&
                interfaceType.name.matches(Regex("I${Regex.escape(type.name)}\\d+"))
        }
    }

    fun isPrimaryRuntimeClassInterface(typeName: String, currentNamespace: String): Boolean {
        val interfaceType = findType(typeName, currentNamespace) ?: return false
        if (interfaceType.kind != WinMdTypeKind.Interface) return false
        return allTypes.any { type ->
            type.kind == WinMdTypeKind.RuntimeClass &&
                type.namespace == interfaceType.namespace &&
                interfaceType.name == "I${type.name}"
        }
    }

    fun isRuntimeClassOverridesInterface(typeName: String, currentNamespace: String): Boolean {
        val interfaceType = findType(typeName, currentNamespace) ?: return false
        if (interfaceType.kind != WinMdTypeKind.Interface) return false
        return allTypes
            .asSequence()
            .filter { type -> type.kind == WinMdTypeKind.RuntimeClass && type.namespace == interfaceType.namespace }
            .flatMap { runtimeClass -> findRuntimeClassOverridesTypes(runtimeClass.name, runtimeClass.namespace).asSequence() }
            .any { helperType -> helperType.name == interfaceType.name }
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

    private fun helperInterfaceOrder(typeName: String, prefix: String): Int? {
        if (!typeName.startsWith(prefix)) return null
        val suffix = typeName.removePrefix(prefix)
        return when {
            suffix.isEmpty() -> 0
            suffix.all(Char::isDigit) -> suffix.toInt()
            else -> null
        }
    }
}

internal fun helperAccessorName(typeName: String): String {
    val rawName = typeName.removePrefix("I")
    Regex(".*(Statics|Factory)(\\d*)$")
        .matchEntire(rawName)
        ?.let { match ->
            val kind = match.groupValues[1].replaceFirstChar(Char::lowercase)
            val version = match.groupValues[2]
            return kind + version
        }
    return rawName.replaceFirstChar(Char::lowercase)
}
