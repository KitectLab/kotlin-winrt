package dev.winrt.winmd.plugin

private const val valueTypeNameMarkerPrefix = "valuetype("

fun encodeValueTypeName(typeName: String): String = "$valueTypeNameMarkerPrefix$typeName)"

fun hasValueTypeNameMarker(typeName: String): Boolean =
    typeName.startsWith(valueTypeNameMarkerPrefix) && typeName.endsWith(")")

fun stripValueTypeNameMarker(typeName: String): String =
    if (hasValueTypeNameMarker(typeName)) {
        typeName.substring(valueTypeNameMarkerPrefix.length, typeName.length - 1)
    } else {
        typeName
    }
