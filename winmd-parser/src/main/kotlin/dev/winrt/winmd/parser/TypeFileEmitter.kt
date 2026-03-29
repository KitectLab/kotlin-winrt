package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.FileSpec
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind

internal class TypeFileEmitter(
    typeRegistry: TypeRegistry,
) {
    private val typeNameMapper = TypeNameMapper()
    private val delegateLambdaPlanResolver = DelegateLambdaPlanResolver(typeNameMapper)
    private val winRtSignatureMapper = WinRtSignatureMapper(typeRegistry)
    private val asyncMethodProjectionPlanner = AsyncMethodProjectionPlanner(typeNameMapper, winRtSignatureMapper)
    private val asyncMethodRuleRegistry = AsyncMethodRuleRegistry(typeNameMapper, asyncMethodProjectionPlanner)
    private val winRtProjectionTypeMapper = WinRtProjectionTypeMapper()
    private val interfaceTypeRenderer = InterfaceTypeRenderer(
        typeNameMapper,
        delegateLambdaPlanResolver,
        typeRegistry,
        asyncMethodProjectionPlanner,
        asyncMethodRuleRegistry,
        winRtProjectionTypeMapper,
    )
    private val delegateTypeRenderer = DelegateTypeRenderer(typeNameMapper)
    private val runtimePropertyRenderer = RuntimePropertyRenderer(typeNameMapper)
    private val runtimeMethodRenderer = RuntimeMethodRenderer(
        typeNameMapper,
        delegateLambdaPlanResolver,
        typeRegistry,
        asyncMethodRuleRegistry,
    )
    private val runtimeCompanionRenderer = RuntimeCompanionRenderer(
        typeRegistry = typeRegistry,
        typeNameMapper = typeNameMapper,
        delegateLambdaPlanResolver = delegateLambdaPlanResolver,
        winRtSignatureMapper = winRtSignatureMapper,
        asyncMethodRuleRegistry = asyncMethodRuleRegistry,
        winRtProjectionTypeMapper = winRtProjectionTypeMapper,
        kotlinCollectionProjectionMapper = KotlinCollectionProjectionMapper(),
    )
    private val valueTypeRenderer = ValueTypeRenderer(typeNameMapper)
    private val runtimeTypeRenderer = RuntimeTypeRenderer(
        typeNameMapper,
        typeRegistry,
        runtimePropertyRenderer,
        runtimeMethodRenderer,
        runtimeCompanionRenderer,
        winRtSignatureMapper,
        winRtProjectionTypeMapper,
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
        val builder = FileSpec.builder(namespace.name.lowercase(), type.name)
        if (type.kind == WinMdTypeKind.Delegate) {
            delegateTypeRenderer.renderLambdaAlias(type)?.let(builder::addTypeAlias)
        }
        return builder
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
