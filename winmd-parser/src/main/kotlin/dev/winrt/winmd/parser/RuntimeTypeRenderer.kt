package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdActivationKind
import dev.winrt.winmd.plugin.WinMdType

internal class RuntimeTypeRenderer(
    private val typeNameMapper: TypeNameMapper,
    private val runtimePropertyRenderer: RuntimePropertyRenderer,
    private val runtimeMethodRenderer: RuntimeMethodRenderer,
) {
    fun render(type: WinMdType): TypeSpec {
        return when (type.kind) {
            dev.winrt.winmd.plugin.WinMdTypeKind.RuntimeClass -> renderRuntimeClass(type)
            dev.winrt.winmd.plugin.WinMdTypeKind.Struct -> renderStruct(type)
            dev.winrt.winmd.plugin.WinMdTypeKind.Enum -> renderEnum(type)
            else -> error("Unsupported type kind for runtime renderer: ${type.kind}")
        }
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
        builder.addType(renderRuntimeClassCompanion(type))
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

    private fun renderRuntimeClassCompanion(type: WinMdType): TypeSpec {
        val typeClass = ClassName(type.namespace.lowercase(), type.name)
        return TypeSpec.companionObjectBuilder()
            .addSuperinterface(PoetSymbols.winRtRuntimeClassMetadataClass)
            .addProperty(overrideStringProperty("qualifiedName", "${type.namespace}.${type.name}"))
            .addProperty(
                PropertySpec.builder("classId", PoetSymbols.runtimeClassIdClass)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("%T(%S, %S)", PoetSymbols.runtimeClassIdClass, type.namespace, type.name)
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("defaultInterfaceName", String::class.asTypeName().copy(nullable = true))
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer(type.defaultInterface?.let { CodeBlock.of("%S", it) } ?: CodeBlock.of("null"))
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("activationKind", PoetSymbols.winRtActivationKindClass)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("%T.%L", PoetSymbols.winRtActivationKindClass, activationKindLiteral(type.activationKind))
                    .build(),
            )
            .addFunction(
                FunSpec.builder(type.activationFunctionName)
                    .returns(typeClass)
                    .addStatement("return %T.activate(this, ::%L)", PoetSymbols.winRtRuntimeClass, type.name)
                    .build(),
            )
            .build()
    }

    private fun renderStruct(type: WinMdType): TypeSpec {
        return TypeSpec.classBuilder(type.name)
            .addModifiers(KModifier.DATA)
            .primaryConstructor(
                FunSpec.constructorBuilder().apply {
                    type.fields.forEach { field ->
                        addParameter(field.name.replaceFirstChar(Char::lowercase), typeNameMapper.mapTypeName(field.type, type.namespace))
                    }
                }.build(),
            )
            .apply {
                type.fields.forEach { field ->
                    addProperty(
                        PropertySpec.builder(field.name.replaceFirstChar(Char::lowercase), typeNameMapper.mapTypeName(field.type, type.namespace))
                            .initializer(field.name.replaceFirstChar(Char::lowercase))
                            .build(),
                    )
                }
            }
            .build()
    }

    private fun renderEnum(type: WinMdType): TypeSpec {
        return TypeSpec.enumBuilder(type.name)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("value", Int::class)
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("value", Int::class)
                    .initializer("value")
                    .build(),
            )
            .apply {
                type.enumMembers.forEach { member ->
                    addEnumConstant(
                        member.name,
                        TypeSpec.anonymousClassBuilder()
                            .addSuperclassConstructorParameter("%L", member.value)
                            .build(),
                    )
                }
            }
            .build()
    }

    private fun activationKindLiteral(kind: WinMdActivationKind): String {
        return when (kind) {
            WinMdActivationKind.Factory -> "Factory"
        }
    }
}
