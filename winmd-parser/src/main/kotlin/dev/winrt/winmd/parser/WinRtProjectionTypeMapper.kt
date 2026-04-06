package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.stripValueTypeNameMarker

internal class WinRtProjectionTypeMapper {
    fun projectionTypeKeyFor(typeName: String, currentNamespace: String): String {
        val normalizedTypeName = normalizeTypeName(typeName, currentNamespace)
        if ('<' in normalizedTypeName && normalizedTypeName.endsWith(">")) {
            return projectionTypeKeyForGenericType(normalizedTypeName, currentNamespace)
        }

        return scalarProjectionTypeKey(normalizedTypeName)
            ?: winRtCollectionProjectionTypeKey(normalizedTypeName)
            ?: normalizedTypeName
    }

    private fun projectionTypeKeyForGenericType(typeName: String, currentNamespace: String): String {
        val genericStart = typeName.indexOf('<')
        val rawTypeName = normalizeTypeName(typeName.substring(0, genericStart), currentNamespace)
        val argumentSource = typeName.substring(genericStart + 1, typeName.length - 1)
        val argumentTypeKeys = splitGenericArguments(argumentSource)
            .map { projectionTypeKeyFor(it, currentNamespace) }

        val mappedRawType = winRtCollectionProjectionTypeKey(rawTypeName) ?: rawTypeName
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
}
