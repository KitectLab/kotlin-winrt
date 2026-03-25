package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import dev.winrt.winmd.plugin.WinMdType

internal class RuntimeTypeRenderer(
    private val runtimePropertyRenderer: RuntimePropertyRenderer,
    private val runtimeMethodRenderer: RuntimeMethodRenderer,
    private val runtimeCompanionRenderer: RuntimeCompanionRenderer,
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

        type.properties.forEach { property ->
            builder.addProperty(runtimePropertyRenderer.renderBackingProperty(property, type.namespace))
            builder.addProperty(runtimePropertyRenderer.renderRuntimeProperty(property, type.namespace))
        }
        type.methods.forEach { builder.addFunction(runtimeMethodRenderer.renderRuntimeMethod(it, type.namespace)) }
        builder.addType(runtimeCompanionRenderer.render(type))
        type.defaultInterface?.let { defaultInterface ->
            val simpleName = defaultInterface.substringAfterLast('.')
            builder.addFunction(
                FunSpec.builder("as$simpleName")
                    .returns(ClassName(defaultInterface.substringBeforeLast('.').lowercase(), simpleName))
                    .addStatement("return %L.from(this)", simpleName)
                    .build(),
            )
        }
        return builder.build()
    }
}
