package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind

internal class TypeFileEmitter(
    typeRegistry: TypeRegistry,
) {
    private val typeNameMapper = TypeNameMapper()
    private val delegateLambdaPlanResolver = DelegateLambdaPlanResolver(typeNameMapper)
    private val eventSlotDelegatePlanResolver = EventSlotDelegatePlanResolver(typeNameMapper, typeRegistry)
    private val winRtSignatureMapper = WinRtSignatureMapper(typeRegistry)
    private val asyncMethodProjectionPlanner = AsyncMethodProjectionPlanner(typeNameMapper, winRtSignatureMapper)
    private val asyncMethodRuleRegistry = AsyncMethodRuleRegistry(typeNameMapper, asyncMethodProjectionPlanner)
    private val winRtProjectionTypeMapper = WinRtProjectionTypeMapper()
    private val interfaceTypeRenderer = InterfaceTypeRenderer(
        typeNameMapper,
        delegateLambdaPlanResolver,
        eventSlotDelegatePlanResolver,
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
        eventSlotDelegatePlanResolver = eventSlotDelegatePlanResolver,
        winRtSignatureMapper = winRtSignatureMapper,
        asyncMethodRuleRegistry = asyncMethodRuleRegistry,
        winRtProjectionTypeMapper = winRtProjectionTypeMapper,
        kotlinCollectionProjectionMapper = KotlinCollectionProjectionMapper(),
    )
    private val valueTypeRenderer = ValueTypeRenderer(typeNameMapper)
    private val runtimeTypeRenderer = RuntimeTypeRenderer(
        typeNameMapper,
        typeRegistry,
        delegateLambdaPlanResolver,
        eventSlotDelegatePlanResolver,
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
        renderTypes(type).forEach(builder::addType)
        return builder.build()
    }

    private fun renderTypes(type: WinMdType): List<TypeSpec> = when (type.kind) {
        WinMdTypeKind.Interface -> interfaceTypeRenderer.render(type)
        WinMdTypeKind.Delegate -> listOf(delegateTypeRenderer.render(type))
        WinMdTypeKind.RuntimeClass -> listOf(runtimeTypeRenderer.render(type))
        WinMdTypeKind.Struct,
        WinMdTypeKind.Enum,
        -> listOf(valueTypeRenderer.render(type))
    }
}
