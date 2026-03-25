package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.FileSpec
import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind

class KotlinBindingGenerator {
    private val typeNameMapper = TypeNameMapper()
    private val interfaceTypeRenderer = InterfaceTypeRenderer(typeNameMapper)
    private val runtimeTypeRenderer = RuntimeTypeRenderer(typeNameMapper)

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
        WinMdTypeKind.RuntimeClass,
        WinMdTypeKind.Struct,
        WinMdTypeKind.Enum,
        -> runtimeTypeRenderer.render(type)
    }
}
