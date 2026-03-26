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
}
