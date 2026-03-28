package dev.winrt.winmd.parser

import dev.winrt.core.ParameterizedInterfaceId
import dev.winrt.core.WinRtTypeSignature
import dev.winrt.winmd.plugin.WinMdTypeKind

internal class WinRtSignatureMapper(
    private val typeRegistry: TypeRegistry,
) {
    fun signatureFor(typeName: String, currentNamespace: String): String {
        val normalizedTypeName = normalizeTypeName(typeName, currentNamespace)
        if ('<' in normalizedTypeName && normalizedTypeName.endsWith(">")) {
            return signatureForGenericType(normalizedTypeName, currentNamespace)
        }

        scalarSignature(normalizedTypeName)?.let { return it }

        val type = typeRegistry.findType(normalizedTypeName)
            ?: error("Unknown WinRT type: $typeName")
        val qualifiedName = "${type.namespace}.${type.name}"
        return when (type.kind) {
            WinMdTypeKind.Interface -> WinRtTypeSignature.guid(type.guid ?: error("Missing GUID for interface $qualifiedName"))
            WinMdTypeKind.Delegate -> "delegate(${WinRtTypeSignature.guid(type.guid ?: error("Missing GUID for delegate $qualifiedName"))})"
            WinMdTypeKind.RuntimeClass -> {
                val defaultInterfaceName = type.defaultInterface
                    ?: error("Missing default interface for runtime class $qualifiedName")
                WinRtTypeSignature.runtimeClass(
                    qualifiedName = qualifiedName,
                    defaultInterfaceSignature = signatureFor(defaultInterfaceName, type.namespace),
                )
            }
            WinMdTypeKind.Enum -> "enum($qualifiedName;i4)"
            WinMdTypeKind.Struct -> error("Struct signatures are not implemented yet for $qualifiedName")
        }
    }

    fun interfaceIdFor(typeName: String, currentNamespace: String): String {
        val signature = signatureFor(typeName, currentNamespace)
        return when {
            signature.startsWith("pinterface(") -> {
                ParameterizedInterfaceId.createFromSignature(signature).toString()
            }
            signature.startsWith("{") && signature.endsWith("}") -> {
                signature.removePrefix("{").removeSuffix("}")
            }
            else -> error("Type $typeName does not project to an interface IID")
        }
    }

    private fun signatureForGenericType(typeName: String, currentNamespace: String): String {
        val genericStart = typeName.indexOf('<')
        val rawTypeName = normalizeTypeName(typeName.substring(0, genericStart), currentNamespace)
        val argumentSource = typeName.substring(genericStart + 1, typeName.length - 1)
        val type = typeRegistry.findType(rawTypeName)
            ?: error("Unknown generic WinRT type: $rawTypeName")
        val interfaceGuid = type.guid ?: error("Missing GUID for generic interface $rawTypeName")
        val argumentSignatures = splitGenericArguments(argumentSource)
            .map { signatureFor(it, currentNamespace) }
            .toTypedArray()
        return WinRtTypeSignature.parameterizedInterface(interfaceGuid, *argumentSignatures)
    }

    private fun normalizeTypeName(typeName: String, currentNamespace: String): String {
        scalarSignature(typeName)?.let { return typeName }
        return when {
            '.' in typeName -> typeName
            else -> "$currentNamespace.$typeName"
        }.removeSuffix("[]")
    }

    private fun scalarSignature(typeName: String): String? = when (typeName) {
        "String" -> WinRtTypeSignature.string()
        "Object" -> WinRtTypeSignature.object_()
        "Boolean" -> "b1"
        "Int32" -> "i4"
        "UInt32" -> "u4"
        "Int64" -> "i8"
        "UInt64" -> "u8"
        "Float32" -> "f4"
        "Float64" -> "f8"
        "Guid" -> "g16"
        "DateTime" -> "struct(Windows.Foundation.DateTime;i8)"
        "TimeSpan" -> "struct(Windows.Foundation.TimeSpan;i8)"
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
