package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind
import dev.winrt.winmd.plugin.hasValueTypeNameMarker
import dev.winrt.winmd.plugin.stripValueTypeNameMarker

internal object WinMdProjectionModelClosure {
    fun retainTypesWithProjectionDependencies(
        model: WinMdModel,
        visibleTypesByNamespace: Map<String, Set<String>>,
    ): WinMdModel {
        if (visibleTypesByNamespace.isEmpty()) {
            return model.copy(namespaces = emptyList())
        }

        val typeRegistry = TypeRegistry(model)
        val retainedTypesByNamespace = linkedMapOf<String, LinkedHashSet<String>>()
        val pendingTypes = ArrayDeque<WinMdType>()

        fun retain(type: WinMdType) {
            val retainedTypes = retainedTypesByNamespace.getOrPut(type.namespace) { linkedSetOf() }
            if (retainedTypes.add(type.name)) {
                pendingTypes.addLast(type)
            }
        }

        visibleTypesByNamespace.forEach { (namespace, typeNames) ->
            typeNames.forEach { typeName ->
                typeRegistry.findType(typeName, namespace)?.let(::retain)
            }
        }

        while (pendingTypes.isNotEmpty()) {
            val type = pendingTypes.removeFirst()
            projectionDependencyTypeNames(typeRegistry, type).forEach { dependencyTypeName ->
                expandReferencedTypeNames(dependencyTypeName).forEach { referencedTypeName ->
                    typeRegistry.findType(referencedTypeName, type.namespace)?.let(::retain)
                }
            }
            if (type.kind == WinMdTypeKind.Interface) {
                typeRegistry.findRuntimeClassesProjectingInterface(type.name, type.namespace).forEach(::retain)
            }
        }

        return model.copy(
            namespaces = model.namespaces.mapNotNull { namespace ->
                val allowedTypes = retainedTypesByNamespace[namespace.name] ?: return@mapNotNull null
                val types = namespace.types.filter { it.name in allowedTypes }
                if (types.isEmpty()) {
                    null
                } else {
                    WinMdNamespace(namespace.name, types)
                }
            },
        )
    }

    fun retainTypesWithHiddenProjectionDependencies(
        model: WinMdModel,
        visibleTypesByNamespace: Map<String, Set<String>>,
    ): WinMdModel = retainTypesWithProjectionDependencies(model, visibleTypesByNamespace)

    private fun projectionDependencyTypeNames(
        typeRegistry: TypeRegistry,
        type: WinMdType,
    ): Sequence<String> = sequence {
        type.baseClass?.let { yield(it) }
        type.defaultInterface?.let { yield(it) }
        yieldAll(type.implementedInterfaces)
        yieldAll(type.baseInterfaces)
        yieldAll(type.activatableFactoryInterfaces)
        yieldAll(type.staticInterfaces)
        yieldAll(type.composableInterfaces.map { it.type })
        yieldAll(type.fields.map { it.type })
        yieldAll(type.properties.map { it.type })
        type.methods.forEach { method ->
            yield(method.returnType)
            yieldAll(method.parameters.map { it.type })
        }
        if (type.kind == WinMdTypeKind.RuntimeClass) {
            yieldAll(typeRegistry.findRuntimeClassStaticsTypes(type.name, type.namespace).map(::qualifiedName))
            yieldAll(typeRegistry.findRuntimeClassFactoryTypes(type.name, type.namespace).map(::qualifiedName))
            yieldAll(typeRegistry.findRuntimeClassOverridesTypes(type.name, type.namespace).map(::qualifiedName))
        }
    }

    private fun qualifiedName(type: WinMdType): String = "${type.namespace}.${type.name}"

    private fun expandReferencedTypeNames(typeName: String): Sequence<String> = sequence {
        val normalizedTypeName = typeName.trim()
        if (normalizedTypeName.isEmpty()) {
            return@sequence
        }
        if (hasValueTypeNameMarker(normalizedTypeName)) {
            yieldAll(expandReferencedTypeNames(stripValueTypeNameMarker(normalizedTypeName)))
            return@sequence
        }
        if (normalizedTypeName.endsWith("[]")) {
            yieldAll(expandReferencedTypeNames(normalizedTypeName.removeSuffix("[]")))
            return@sequence
        }
        val genericStart = normalizedTypeName.indexOf('<')
        if (genericStart > 0 && normalizedTypeName.endsWith(">")) {
            yield(normalizedTypeName.substring(0, genericStart))
            splitGenericArguments(normalizedTypeName.substring(genericStart + 1, normalizedTypeName.length - 1))
                .forEach { argumentType ->
                    yieldAll(expandReferencedTypeNames(argumentType))
                }
            return@sequence
        }
        yield(normalizedTypeName)
    }

    private fun splitGenericArguments(source: String): List<String> {
        if (source.isBlank()) {
            return emptyList()
        }
        val arguments = mutableListOf<String>()
        var depth = 0
        var start = 0
        source.forEachIndexed { index, char ->
            when (char) {
                '<' -> depth++
                '>' -> depth--
                ',' -> if (depth == 0) {
                    arguments += source.substring(start, index).trim()
                    start = index + 1
                }
            }
        }
        arguments += source.substring(start).trim()
        return arguments
    }
}
