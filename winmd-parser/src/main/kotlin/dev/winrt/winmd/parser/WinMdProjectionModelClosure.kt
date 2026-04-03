package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace

internal object WinMdProjectionModelClosure {
    fun retainTypesWithHiddenProjectionDependencies(
        model: WinMdModel,
        visibleTypesByNamespace: Map<String, Set<String>>,
    ): WinMdModel {
        val typeRegistry = TypeRegistry(model)
        val expandedVisibleTypes = visibleTypesByNamespace.mapValues { (namespace, typeNames) ->
            typeNames + hiddenProjectionDependencies(typeRegistry, namespace, typeNames)
        }

        return model.copy(
            namespaces = model.namespaces.mapNotNull { namespace ->
                val allowedTypes = expandedVisibleTypes[namespace.name] ?: return@mapNotNull null
                val types = namespace.types.filter { it.name in allowedTypes }
                if (types.isEmpty()) {
                    null
                } else {
                    WinMdNamespace(namespace.name, types)
                }
            },
        )
    }

    private fun hiddenProjectionDependencies(
        typeRegistry: TypeRegistry,
        namespace: String,
        visibleTypeNames: Set<String>,
    ): Set<String> {
        return buildSet {
            visibleTypeNames.forEach { typeName ->
                typeRegistry.findRuntimeClassStaticsTypes(typeName, namespace).forEach { add(it.name) }
                typeRegistry.findRuntimeClassFactoryTypes(typeName, namespace).forEach { add(it.name) }
                typeRegistry.findRuntimeClassOverridesTypes(typeName, namespace).forEach { add(it.name) }
            }
        }
    }
}
