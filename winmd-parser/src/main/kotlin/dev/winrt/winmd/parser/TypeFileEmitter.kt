package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.FileSpec
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind

internal class TypeFileEmitter(
    typeRegistry: TypeRegistry,
) {
    private val typeNameMapper = TypeNameMapper()
    private val interfaceTypeRenderer = InterfaceTypeRenderer(typeNameMapper, typeRegistry)
    private val delegateTypeRenderer = DelegateTypeRenderer()
    private val runtimePropertyRenderer = RuntimePropertyRenderer(typeNameMapper)
    private val runtimeMethodRenderer = RuntimeMethodRenderer(typeNameMapper)
    private val runtimeCompanionRenderer = RuntimeCompanionRenderer()
    private val runtimeProjectionRenderer = RuntimeProjectionRenderer()
    private val valueTypeRenderer = ValueTypeRenderer(typeNameMapper)
    private val runtimeTypeRenderer = RuntimeTypeRenderer(
        typeNameMapper,
        runtimePropertyRenderer,
        runtimeMethodRenderer,
        runtimeCompanionRenderer,
        runtimeProjectionRenderer,
    )

    fun emit(namespace: WinMdNamespace, type: WinMdType): GeneratedFile {
        val fileSpec = renderTypeFile(namespace, type)
        val content = try {
            fileSpec.toString()
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("Failed to emit ${namespace.name}.${type.name}: ${error.message}", error)
        }
        return GeneratedFile(
            relativePath = namespace.name.replace('.', '/') + "/${type.name}.kt",
            content = content,
        )
    }

    private fun renderTypeFile(namespace: WinMdNamespace, type: WinMdType): FileSpec {
        return FileSpec.builder(namespace.name.lowercase(), type.name)
            .addType(renderType(type))
            .build()
    }

    private fun renderType(type: WinMdType) = when (type.kind) {
        WinMdTypeKind.Interface -> interfaceTypeRenderer.render(type)
        WinMdTypeKind.Delegate -> delegateTypeRenderer.render(type)
        WinMdTypeKind.RuntimeClass -> runtimeTypeRenderer.render(type)
        WinMdTypeKind.Struct,
        WinMdTypeKind.Enum,
        -> valueTypeRenderer.render(type)
    }
}
