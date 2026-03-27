package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import dev.winrt.winmd.plugin.WinMdActivationKind
import dev.winrt.winmd.plugin.WinMdType

internal class RuntimeTypeRenderer(
    private val runtimePropertyRenderer: RuntimePropertyRenderer,
    private val runtimeMethodRenderer: RuntimeMethodRenderer,
    private val runtimeCompanionRenderer: RuntimeCompanionRenderer,
    private val runtimeProjectionRenderer: RuntimeProjectionRenderer,
) {
    fun render(type: WinMdType): TypeSpec {
        require(type.kind == dev.winrt.winmd.plugin.WinMdTypeKind.RuntimeClass) {
            "Unsupported type kind for runtime renderer: ${type.kind}"
        }
        return renderRuntimeClass(type)
    }

    private fun renderRuntimeClass(type: WinMdType): TypeSpec {
        val builder = TypeSpec.classBuilder(type.name)
            .addModifiers(KModifier.OPEN)
            .primaryConstructor(pointerConstructor())
            .superclass(PoetSymbols.inspectableClass)
            .addSuperclassConstructorParameter("pointer")

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
        builder.addType(runtimeCompanionRenderer.render(type))
        type.defaultInterface?.let { defaultInterface ->
            runtimeProjectionRenderer.renderDefaultInterfaceProjection(defaultInterface)
                ?.let(builder::addFunction)
        }
        return builder.build()
    }
}
