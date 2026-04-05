package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import dev.winrt.core.WinRtTypeSignature
import dev.winrt.winmd.plugin.WinMdTypeKind

internal class ProjectedObjectArgumentLowering(
    private val typeRegistry: TypeRegistry,
    private val winRtSignatureMapper: WinRtSignatureMapper,
    private val winRtProjectionTypeMapper: WinRtProjectionTypeMapper,
) {
    fun supportsInputType(typeName: String, currentNamespace: String): Boolean {
        if (typeRegistry.isEnumType(typeName, currentNamespace)) {
            return false
        }
        return supportsProjectedObjectTypeName(typeName) || supportsClosedGenericProjectedType(typeName, currentNamespace)
    }

    fun expression(argumentName: String, typeName: String, currentNamespace: String): CodeBlock {
        return CodeBlock.of(
            "%M(%N, %S, %S)",
            PoetSymbols.projectedObjectArgumentPointerMember,
            argumentName,
            winRtProjectionTypeMapper.projectionTypeKeyFor(typeName, currentNamespace),
            signatureForInputType(typeName, currentNamespace),
        )
    }

    private fun supportsClosedGenericProjectedType(typeName: String, currentNamespace: String): Boolean {
        val rawTypeName = typeName
            .takeIf { '<' in it && it.endsWith(">") }
            ?.substringBefore('<')
            ?: return false
        val genericArgumentSource = typeName.substringAfter('<').substringBeforeLast('>')
        val hasResolvableArguments = splitGenericArguments(genericArgumentSource).all { argument ->
            try {
                winRtSignatureMapper.signatureFor(argument, currentNamespace)
                true
            } catch (_: IllegalStateException) {
                false
            }
        }
        if (!hasResolvableArguments) {
            return false
        }
        val rawType = typeRegistry.findType(rawTypeName, currentNamespace)
        return when {
            rawType != null -> rawType.kind in setOf(WinMdTypeKind.Interface, WinMdTypeKind.Delegate)
            '.' in rawTypeName -> true
            else -> false
        }
    }

    private fun signatureForInputType(typeName: String, currentNamespace: String): String {
        return try {
            winRtSignatureMapper.signatureFor(typeName, currentNamespace)
        } catch (error: IllegalStateException) {
            val runtimeType = typeRegistry.findType(typeName, currentNamespace)
            if (runtimeType?.kind == WinMdTypeKind.RuntimeClass &&
                error.message?.contains("Missing default interface") == true
            ) {
                WinRtTypeSignature.object_()
            } else if (
                runtimeType == null &&
                supportsProjectedObjectTypeName(typeName) &&
                error.message?.contains("Unknown WinRT type:") == true
            ) {
                WinRtTypeSignature.object_()
            } else {
                throw error
            }
        }
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
