package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.TypeSpec
import dev.winrt.winmd.plugin.WinMdActivationKind
import dev.winrt.winmd.plugin.WinMdType

internal class RuntimeTypeRenderer(
    private val typeNameMapper: TypeNameMapper,
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
        val superclass = type.baseClass
            ?.takeUnless { it == "System.Object" }
            ?.let { typeNameMapper.mapTypeName(it, type.namespace) }
            ?: PoetSymbols.inspectableClass
        val builder = TypeSpec.classBuilder(type.name)
            .addModifiers(KModifier.OPEN)
            .primaryConstructor(pointerConstructor())
            .superclass(superclass)
            .addSuperclassConstructorParameter("pointer")

        if (type.namespace == "Windows.Foundation.Collections" && type.name == "StringVectorView") {
            builder.addSuperinterface(
                PoetSymbols.listClass.parameterizedBy(String::class.asTypeName()),
                CodeBlock.of(
                    "%T(sizeProvider = { %T(%T.invokeUInt32Method(pointer, 7).getOrThrow()).value.toInt() }, getter = { index -> val value = %T.invokeHStringMethodWithUInt32Arg(pointer, 6, index.toUInt()).getOrThrow(); try { %T.toKotlin(value) } finally { %T.release(value) } })",
                    PoetSymbols.winRtListProjectionClass.parameterizedBy(String::class.asTypeName()),
                    PoetSymbols.uint32Class,
                    PoetSymbols.platformComInteropClass,
                    PoetSymbols.platformComInteropClass,
                    PoetSymbols.winRtStringsClass,
                    PoetSymbols.winRtStringsClass,
                ),
            )
            builder.addProperty(
                PropertySpec.builder("winRtSize", PoetSymbols.uint32Class)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement(
                                "return %T(%T.invokeUInt32Method(pointer, 7).getOrThrow())",
                                PoetSymbols.uint32Class,
                                PoetSymbols.platformComInteropClass,
                            )
                            .build(),
                    )
                    .build(),
            )
            builder.addFunction(
                FunSpec.builder("getAt")
                    .addParameter("index", PoetSymbols.uint32Class)
                    .returns(String::class)
                    .addStatement(
                        "val value = %T.invokeHStringMethodWithUInt32Arg(pointer, 6, index.value).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                    )
                    .beginControlFlow("return try")
                    .addStatement("%T.toKotlin(value)", PoetSymbols.winRtStringsClass)
                    .nextControlFlow("finally")
                    .addStatement("%T.release(value)", PoetSymbols.winRtStringsClass)
                    .endControlFlow()
                    .build(),
            )
        }

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
        type.implementedInterfaces
            .asSequence()
            .filter { it != type.defaultInterface }
            .mapNotNull(runtimeProjectionRenderer::renderImplementedInterfaceProjection)
            .distinctBy { it.name }
            .forEach(builder::addFunction)
        return builder.build()
    }
}
