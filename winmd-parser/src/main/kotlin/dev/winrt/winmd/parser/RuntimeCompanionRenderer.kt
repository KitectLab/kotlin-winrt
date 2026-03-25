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

internal class RuntimeCompanionRenderer {
    fun render(type: WinMdType): TypeSpec {
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

    private fun activationKindLiteral(kind: WinMdActivationKind): String {
        return when (kind) {
            WinMdActivationKind.Factory -> "Factory"
        }
    }
}
