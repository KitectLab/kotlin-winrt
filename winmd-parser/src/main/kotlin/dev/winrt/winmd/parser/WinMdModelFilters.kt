package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace

object WinMdModelFilters {
    fun filterNamespaces(model: WinMdModel, namespaceFilters: List<String>): WinMdModel {
        if (namespaceFilters.isEmpty()) {
            return model
        }

        val filteredNamespaces = model.namespaces.mapNotNull { namespace ->
            if (namespaceFilters.any { namespace.name == it || namespace.name.startsWith("$it.") }) {
                namespace
            } else {
                null
            }
        }

        return model.copy(
            namespaces = filteredNamespaces.map { namespace ->
                WinMdNamespace(namespace.name, namespace.types)
            },
        )
    }

    fun filterNamespacesWithProjectionDependencies(model: WinMdModel, namespaceFilters: List<String>): WinMdModel {
        if (namespaceFilters.isEmpty()) {
            return model
        }
        return WinMdProjectionModelClosure.retainTypesWithProjectionDependencies(
            model = model,
            visibleTypesByNamespace = visibleTypesByNamespace(model, namespaceFilters),
        )
    }

    internal fun visibleTypesByNamespace(model: WinMdModel, namespaceFilters: List<String>): Map<String, Set<String>> {
        if (namespaceFilters.isEmpty()) {
            return model.namespaces.associate { namespace ->
                namespace.name to namespace.types.mapTo(linkedSetOf()) { type -> type.name }
            }
        }

        return buildMap {
            model.namespaces.forEach { namespace ->
                if (namespaceFilters.any { namespace.name == it || namespace.name.startsWith("$it.") }) {
                    put(namespace.name, namespace.types.mapTo(linkedSetOf()) { type -> type.name })
                }
            }
        }
    }
}
