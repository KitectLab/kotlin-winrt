package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.FileSpec
import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind

class KotlinBindingGenerator {
    private val typeNameMapper = TypeNameMapper()
    private val interfaceTypeRenderer = InterfaceTypeRenderer(typeNameMapper)
    private val runtimePropertyRenderer = RuntimePropertyRenderer(typeNameMapper)
    private val runtimeMethodRenderer = RuntimeMethodRenderer(typeNameMapper)
    private val runtimeCompanionRenderer = RuntimeCompanionRenderer()
    private val runtimeProjectionRenderer = RuntimeProjectionRenderer()
    private val valueTypeRenderer = ValueTypeRenderer(typeNameMapper)
    private val runtimeTypeRenderer = RuntimeTypeRenderer(
        runtimePropertyRenderer,
        runtimeMethodRenderer,
        runtimeCompanionRenderer,
        runtimeProjectionRenderer,
    )

    fun generate(model: WinMdModel): List<GeneratedFile> {
        return model.namespaces.flatMap { namespace ->
            namespace.types.map { type ->
                GeneratedFile(
                    relativePath = namespace.name.replace('.', '/') + "/${type.name}.kt",
                    content = renderTypeFile(namespace, type).toString(),
                )
            }
        }
    }

    private fun renderTypeFile(namespace: WinMdNamespace, type: WinMdType): FileSpec {
        return FileSpec.builder(namespace.name.lowercase(), type.name)
            .addType(renderType(type))
            .build()
    }

    private fun renderType(type: WinMdType) = when (type.kind) {
        WinMdTypeKind.Interface -> interfaceTypeRenderer.render(type)
        WinMdTypeKind.RuntimeClass -> runtimeTypeRenderer.render(type)
        WinMdTypeKind.Struct,
        WinMdTypeKind.Enum,
        -> valueTypeRenderer.render(type)
    }
}
