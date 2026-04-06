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
        if (
            typeRegistry.isEnumType(typeName, currentNamespace) ||
            typeRegistry.isStructType(typeName, currentNamespace) ||
            supportsIReferenceValueProjection(typeName, currentNamespace, typeRegistry) ||
            supportsGenericIReferenceStructProjection(typeName, currentNamespace, typeRegistry) ||
            supportsGenericIReferenceEnumProjection(typeName, currentNamespace, typeRegistry)
        ) {
            return false
        }
        return supportsProjectedObjectTypeName(typeName) || supportsClosedGenericProjectedType(typeName, currentNamespace)
    }

    fun expression(argumentName: String, typeName: String, currentNamespace: String): CodeBlock {
        val inputSignature = signatureForInputType(typeName, currentNamespace)
        if (canUseDirectObjectPointer(typeName, currentNamespace, inputSignature)) {
            return CodeBlock.of("%N.pointer", argumentName)
        }
        return CodeBlock.of(
            "%M(%N, %S, %S)",
            PoetSymbols.projectedObjectArgumentPointerMember,
            argumentName,
            winRtProjectionTypeMapper.projectionTypeKeyFor(typeName, currentNamespace),
            inputSignature,
        )
    }

    private fun canUseDirectObjectPointer(
        typeName: String,
        currentNamespace: String,
        inputSignature: String,
    ): Boolean {
        if (inputSignature != WinRtTypeSignature.object_()) {
            return false
        }
        val runtimeType = typeRegistry.findType(typeName, currentNamespace)
        return runtimeType?.kind == WinMdTypeKind.RuntimeClass
    }

    private fun supportsClosedGenericProjectedType(typeName: String, currentNamespace: String): Boolean {
        return typeRegistry.supportsClosedGenericProjectedType(
            typeName = typeName,
            currentNamespace = currentNamespace,
            winRtSignatureMapper = winRtSignatureMapper,
            supportedKinds = setOf(WinMdTypeKind.Interface, WinMdTypeKind.Delegate),
        )
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

}
