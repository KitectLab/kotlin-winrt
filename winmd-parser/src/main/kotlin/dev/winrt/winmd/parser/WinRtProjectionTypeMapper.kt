package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.stripValueTypeNameMarker

internal class WinRtProjectionTypeMapper {
    fun projectionTypeKeyFor(typeName: String, currentNamespace: String): String {
        val normalizedTypeName = normalizeTypeName(typeName, currentNamespace)
        if ('<' in normalizedTypeName && normalizedTypeName.endsWith(">")) {
            return projectionTypeKeyForGenericType(normalizedTypeName, currentNamespace)
        }

        return scalarProjectionTypeKey(normalizedTypeName)
            ?: interfaceProjectionTypeKey(normalizedTypeName)
            ?: normalizedTypeName
    }

    private fun projectionTypeKeyForGenericType(typeName: String, currentNamespace: String): String {
        val genericStart = typeName.indexOf('<')
        val rawTypeName = normalizeTypeName(typeName.substring(0, genericStart), currentNamespace)
        val argumentSource = typeName.substring(genericStart + 1, typeName.length - 1)
        val argumentTypeKeys = splitGenericArguments(argumentSource)
            .map { projectionTypeKeyFor(it, currentNamespace) }

        val mappedRawType = interfaceProjectionTypeKey(rawTypeName) ?: rawTypeName
        return "$mappedRawType<${argumentTypeKeys.joinToString(", ")}>"
    }

    private fun normalizeTypeName(typeName: String, currentNamespace: String): String {
        val unwrappedTypeName = stripValueTypeNameMarker(typeName.removeSuffix("[]"))
        scalarProjectionTypeKey(unwrappedTypeName)?.let { return unwrappedTypeName }
        return when {
            '.' in unwrappedTypeName -> unwrappedTypeName
            else -> "$currentNamespace.$unwrappedTypeName"
        }
    }

    private fun scalarProjectionTypeKey(typeName: String): String? = when (canonicalWinRtSpecialType(typeName)) {
        "String" -> "String"
        "Object" -> "Object"
        "Boolean" -> "Boolean"
        "HResult" -> "Exception"
        "UInt8" -> "UInt8"
        "Int16" -> "Int16"
        "UInt16" -> "UInt16"
        "Char16" -> "Char16"
        "Int32" -> "Int32"
        "UInt32" -> "UInt32"
        "Int64" -> "Int64"
        "UInt64" -> "UInt64"
        "Float32" -> "Float32"
        "Float64" -> "Float64"
        "Guid" -> "Guid"
        "DateTime" -> "DateTime"
        "TimeSpan" -> "TimeSpan"
        "EventRegistrationToken" -> "EventRegistrationToken"
        else -> null
    }

    private fun interfaceProjectionTypeKey(typeName: String): String? = when (typeName) {
        "Microsoft.UI.Xaml.Interop.IBindableIterable" -> "kotlin.collections.Iterable"
        "Microsoft.UI.Xaml.Interop.IBindableIterator" -> "kotlin.collections.Iterator"
        "Microsoft.UI.Xaml.Interop.IBindableVector" -> "kotlin.collections.MutableList"
        "Microsoft.UI.Xaml.Interop.IBindableVectorView" -> "kotlin.collections.List"
        "Windows.Foundation.Collections.IIterable" -> "kotlin.collections.Iterable"
        "Windows.Foundation.Collections.IIterable`1" -> "kotlin.collections.Iterable"
        "Windows.Foundation.Collections.IIterator" -> "kotlin.collections.Iterator"
        "Windows.Foundation.Collections.IIterator`1" -> "kotlin.collections.Iterator"
        "Windows.Foundation.Collections.IVector" -> "kotlin.collections.MutableList"
        "Windows.Foundation.Collections.IVector`1" -> "kotlin.collections.MutableList"
        "Windows.Foundation.Collections.IVectorView" -> "kotlin.collections.List"
        "Windows.Foundation.Collections.IVectorView`1" -> "kotlin.collections.List"
        "Windows.Foundation.Collections.IMap" -> "kotlin.collections.MutableMap"
        "Windows.Foundation.Collections.IMap`2" -> "kotlin.collections.MutableMap"
        "Windows.Foundation.Collections.IMapView" -> "kotlin.collections.Map"
        "Windows.Foundation.Collections.IMapView`2" -> "kotlin.collections.Map"
        "Windows.Foundation.Collections.IKeyValuePair" -> "kotlin.collections.Map.Entry"
        "Windows.Foundation.Collections.IKeyValuePair`2" -> "kotlin.collections.Map.Entry"
        "Windows.Foundation.Collections.IObservableVector" -> "kotlin.collections.MutableList"
        "Windows.Foundation.Collections.IObservableVector`1" -> "kotlin.collections.MutableList"
        "Windows.Foundation.Collections.IObservableMap" -> "kotlin.collections.MutableMap"
        "Windows.Foundation.Collections.IObservableMap`2" -> "kotlin.collections.MutableMap"
        else -> null
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
