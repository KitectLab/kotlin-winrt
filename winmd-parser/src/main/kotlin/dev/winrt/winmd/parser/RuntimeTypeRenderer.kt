package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import dev.winrt.winmd.plugin.WinMdActivationKind
import dev.winrt.winmd.plugin.WinMdType

internal class RuntimeTypeRenderer(
    private val typeNameMapper: TypeNameMapper,
    private val typeRegistry: TypeRegistry,
    private val runtimePropertyRenderer: RuntimePropertyRenderer,
    private val runtimeMethodRenderer: RuntimeMethodRenderer,
    private val runtimeCompanionRenderer: RuntimeCompanionRenderer,
    private val winRtSignatureMapper: WinRtSignatureMapper,
    private val winRtProjectionTypeMapper: WinRtProjectionTypeMapper,
    private val kotlinCollectionProjectionMapper: KotlinCollectionProjectionMapper = KotlinCollectionProjectionMapper(),
) {
    fun render(type: WinMdType): TypeSpec {
        require(type.kind == dev.winrt.winmd.plugin.WinMdTypeKind.RuntimeClass) {
            "Unsupported type kind for runtime renderer: ${type.kind}"
        }
        return renderRuntimeClass(type)
    }

    private fun renderRuntimeClass(type: WinMdType): TypeSpec {
        val superclass = type.baseClass
            ?.takeUnless { it == "System.Object" }
            ?.let { typeNameMapper.mapTypeName(it, type.namespace) }
            ?: PoetSymbols.inspectableClass
        val builder = TypeSpec.classBuilder(type.name)
            .addModifiers(KModifier.OPEN)
            .primaryConstructor(pointerConstructor())
            .superclass(superclass)
            .addSuperclassConstructorParameter("pointer")

        kotlinCollectionProjectionMapper.runtimeClassProjection(type)?.let { projection ->
            builder.addSuperinterface(projection.superinterface)
            projection.extraProperties.forEach(builder::addProperty)
            projection.extraFunctions.forEach(builder::addFunction)
            builder.addProperty(kotlinCollectionProjectionMapper.buildWinRtSizeProperty(projection.winRtSizeSlot))
        }
        kotlinCollectionProjectionMapper.runtimeClassInterfaceProjection(
            type = type,
            typeRegistry = typeRegistry,
            typeNameMapper = typeNameMapper,
            winRtSignatureMapper = winRtSignatureMapper,
            winRtProjectionTypeMapper = winRtProjectionTypeMapper,
        )?.let { projection ->
            builder.addSuperinterface(projection.superinterface)
            projection.extraProperties.forEach(builder::addProperty)
            projection.extraFunctions.forEach(builder::addFunction)
            builder.addProperty(kotlinCollectionProjectionMapper.buildWinRtSizeProperty(projection.winRtSizeSlot))
        }
        kotlinCollectionProjectionMapper.runtimeClassIterableProjection(
            type = type,
            typeRegistry = typeRegistry,
            typeNameMapper = typeNameMapper,
            winRtSignatureMapper = winRtSignatureMapper,
            winRtProjectionTypeMapper = winRtProjectionTypeMapper,
        )?.let { projection ->
            builder.addSuperinterface(projection.superinterface, projection.delegateFactory)
        }
        type.baseInterfaces.mapNotNull { baseInterface ->
            collectionSuperinterface(baseInterface, type.namespace, emptySet())
        }.forEach(builder::addSuperinterface)

        if (type.activationKind == WinMdActivationKind.Factory) {
            builder.addFunction(
                FunSpec.constructorBuilder()
                    .callThisConstructor(CodeBlock.of("Companion.%L().pointer", type.activationFunctionName))
                    .build(),
            )
        }

        type.properties.filter(runtimePropertyRenderer::canRenderRuntimeProperty).forEach { property ->
            builder.addProperty(runtimePropertyRenderer.renderBackingProperty(property, type.namespace))
            builder.addProperty(runtimePropertyRenderer.renderRuntimeProperty(property, type.namespace))
        }
        type.methods.filter(runtimeMethodRenderer::canRenderRuntimeMethod)
            .mapNotNull { runtimeMethodRenderer.renderRuntimeMethod(it, type.namespace) }
            .forEach(builder::addFunction)
        type.methods
            .mapNotNull { runtimeMethodRenderer.renderRuntimeLambdaOverload(it, type.namespace) }
            .forEach(builder::addFunction)
        renderDefaultInterfaceMembers(type).let { projection ->
            projection.properties.forEach(builder::addProperty)
            projection.methods.forEach(builder::addFunction)
        }
        renderBaseInterfaceMembers(type).let { projection ->
            projection.properties.forEach(builder::addProperty)
            projection.methods.forEach(builder::addFunction)
        }
        builder.addType(runtimeCompanionRenderer.render(type))
        return builder.build()
    }

    private fun renderDefaultInterfaceMembers(type: WinMdType): RuntimeProjectionMembers {
        val defaultInterface = typeRegistry.findDefaultInterfaceType(type.name, type.namespace)
            ?: return RuntimeProjectionMembers(emptyList(), emptyList())
        val methods = defaultInterface.methods
            .filter(runtimeMethodRenderer::canRenderRuntimeMethod)
            .mapNotNull { runtimeMethodRenderer.renderRuntimeMethod(it, type.namespace) }
        val properties = defaultInterface.properties
            .filter(runtimePropertyRenderer::canRenderRuntimeProperty)
            .flatMap { property ->
                listOfNotNull(
                    runtimePropertyRenderer.renderBackingProperty(property, type.namespace),
                    runtimePropertyRenderer.renderRuntimeProperty(property, type.namespace),
                )
        }
        return RuntimeProjectionMembers(methods, properties)
    }

    private fun renderBaseInterfaceMembers(type: WinMdType): RuntimeProjectionMembers {
        val methods = mutableListOf<FunSpec>()
        val properties = mutableListOf<PropertySpec>()
        typeRegistry.findImplementedInterfaceTypes(type.name, type.namespace)
            .forEach { baseInterface ->
                methods += baseInterface.methods
                    .filter(runtimeMethodRenderer::canRenderRuntimeMethod)
                    .mapNotNull { runtimeMethodRenderer.renderRuntimeMethod(it, type.namespace) }
                properties += baseInterface.properties
                    .filter(runtimePropertyRenderer::canRenderRuntimeProperty)
                    .flatMap { property ->
                        listOfNotNull(
                            runtimePropertyRenderer.renderBackingProperty(property, type.namespace),
                            runtimePropertyRenderer.renderRuntimeProperty(property, type.namespace),
                        )
                    }
        }
        return RuntimeProjectionMembers(methods, properties)
    }

    private fun collectionSuperinterface(
        baseInterface: String,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): com.squareup.kotlinpoet.TypeName? {
        val mapped = typeNameMapper.mapTypeName(baseInterface, currentNamespace, genericParameters)
        return if (mapped.toString().startsWith("kotlin.collections.")) mapped else null
    }

    private data class RuntimeProjectionMembers(
        val methods: List<FunSpec>,
        val properties: List<PropertySpec>,
    )
}
