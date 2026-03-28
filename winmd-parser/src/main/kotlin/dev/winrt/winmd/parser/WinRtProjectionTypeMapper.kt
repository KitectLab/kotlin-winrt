package dev.winrt.winmd.parser

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
        scalarProjectionTypeKey(typeName)?.let { return typeName }
        return when {
            '.' in typeName -> typeName
            else -> "$currentNamespace.$typeName"
        }.removeSuffix("[]")
    }

    private fun scalarProjectionTypeKey(typeName: String): String? = when (typeName) {
        "String" -> "String"
        "Object" -> "Object"
        "Boolean" -> "Boolean"
        "Int32" -> "Int32"
        "UInt32" -> "UInt32"
        "Int64" -> "Int64"
        "UInt64" -> "UInt64"
        "Float32" -> "Float32"
        "Float64" -> "Float64"
        "Guid" -> "Guid"
        "DateTime" -> "DateTime"
        "TimeSpan" -> "TimeSpan"
        else -> null
    }

    private fun interfaceProjectionTypeKey(typeName: String): String? = when (typeName) {
        "Microsoft.UI.Xaml.Interop.IBindableIterable" -> "System.Collections.IEnumerable"
        "Microsoft.UI.Xaml.Interop.IBindableVector" -> "System.Collections.IList"
        "Windows.Foundation.Collections.IIterable`1" -> "System.Collections.Generic.IEnumerable"
        "Windows.Foundation.Collections.IIterator`1" -> "System.Collections.Generic.IEnumerator"
        "Windows.Foundation.Collections.IVector`1" -> "System.Collections.Generic.IList"
        "Windows.Foundation.Collections.IVectorView`1" -> "System.Collections.Generic.IReadOnlyList"
        "Windows.Foundation.Collections.IMap`2" -> "System.Collections.Generic.IDictionary"
        "Windows.Foundation.Collections.IMapView`2" -> "System.Collections.Generic.IReadOnlyDictionary"
        "Windows.Foundation.Collections.IKeyValuePair`2" -> "System.Collections.Generic.KeyValuePair"
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
