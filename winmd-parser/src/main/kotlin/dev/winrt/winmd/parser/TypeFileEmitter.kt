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
    private val winRtProjectionTypeMapper = WinRtProjectionTypeMapper()
    private val projectedObjectArgumentLowering = ProjectedObjectArgumentLowering(
        typeRegistry,
        winRtSignatureMapper,
        winRtProjectionTypeMapper,
    )
    private val asyncMethodProjectionPlanner = AsyncMethodProjectionPlanner(typeNameMapper, winRtSignatureMapper)
    private val asyncMethodRuleRegistry = AsyncMethodRuleRegistry(
        typeNameMapper,
        asyncMethodProjectionPlanner,
        projectedObjectArgumentLowering,
    )
    private val interfaceTypeRenderer = InterfaceTypeRenderer(
        typeNameMapper,
        delegateLambdaPlanResolver,
        eventSlotDelegatePlanResolver,
        typeRegistry,
        asyncMethodProjectionPlanner,
        asyncMethodRuleRegistry,
        winRtSignatureMapper,
        winRtProjectionTypeMapper,
        projectedObjectArgumentLowering,
    )
    private val delegateTypeRenderer = DelegateTypeRenderer(typeNameMapper)
    private val runtimePropertyRenderer = RuntimePropertyRenderer(typeNameMapper, typeRegistry)
    private val runtimeMethodRenderer = RuntimeMethodRenderer(
        typeNameMapper,
        delegateLambdaPlanResolver,
        typeRegistry,
        asyncMethodRuleRegistry,
        projectedObjectArgumentLowering,
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
        supportsInterfaceMethod = interfaceTypeRenderer::supportsForwardedMethod,
        projectedObjectArgumentLowering = projectedObjectArgumentLowering,
    )
    private val valueTypeRenderer = ValueTypeRenderer(typeNameMapper, typeRegistry)
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

    fun emit(namespace: WinMdNamespace, type: WinMdType): GeneratedFile? {
        val fileSpec = renderTypeFile(namespace, type) ?: return null
        val content = try {
            normalizeRenderedIdentifiers(fileSpec.toString())
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("Failed to emit ${namespace.name}.${type.name}: ${error.message}", error)
        }
        return GeneratedFile(
            relativePath = namespace.name.replace('.', '/') + "/${type.name}.kt",
            content = content,
        )
    }

    private fun renderTypeFile(namespace: WinMdNamespace, type: WinMdType): FileSpec? {
        val renderedTypes = renderTypes(type)
        if (renderedTypes.isEmpty()) {
            return null
        }
        val builder = FileSpec.builder(namespace.name.lowercase(), type.name)
        if (type.kind == WinMdTypeKind.Delegate) {
            delegateTypeRenderer.renderLambdaAlias(type)?.let(builder::addTypeAlias)
        }
        renderedTypes.forEach(builder::addType)
        return builder.build()
    }

    private fun renderTypes(type: WinMdType): List<TypeSpec> = when (type.kind) {
        WinMdTypeKind.Interface -> interfaceTypeRenderer.render(type)
        WinMdTypeKind.Delegate -> listOf(delegateTypeRenderer.render(type))
        WinMdTypeKind.RuntimeClass -> listOf(runtimeTypeRenderer.render(type))
        WinMdTypeKind.Struct,
        WinMdTypeKind.Enum,
        -> {
            if (type.kind == WinMdTypeKind.Struct && isHResultType("${type.namespace}.${type.name}")) {
                emptyList()
            } else {
                listOf(valueTypeRenderer.render(type))
            }
        }
    }
}
