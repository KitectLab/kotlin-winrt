package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdType

internal class InterfaceTypeRenderer(
    private val typeNameMapper: TypeNameMapper,
) {
    fun render(type: WinMdType): TypeSpec {
        val typeClass = ClassName(type.namespace.lowercase(), type.name)
        return TypeSpec.classBuilder(type.name)
            .addModifiers(KModifier.OPEN)
            .primaryConstructor(pointerConstructor())
            .superclass(PoetSymbols.winRtInterfaceProjectionClass)
            .addSuperclassConstructorParameter("pointer")
            .addFunctions(type.methods.mapNotNull { renderMethod(it, type.namespace) })
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addSuperinterface(PoetSymbols.winRtInterfaceMetadataClass)
                    .addProperty(overrideStringProperty("qualifiedName", "${type.namespace}.${type.name}"))
                    .addProperty(
                        PropertySpec.builder("iid", PoetSymbols.guidClass)
                            .addModifiers(KModifier.OVERRIDE)
                            .initializer("%M(%S)", PoetSymbols.guidOfMember, type.guid ?: "00000000-0000-0000-0000-000000000000")
                            .build(),
                    )
                    .addFunction(
                        FunSpec.builder("from")
                            .returns(typeClass)
                            .addParameter("inspectable", PoetSymbols.inspectableClass)
                            .addStatement("return inspectable.%M(this, ::%L)", PoetSymbols.projectInterfaceMember, type.name)
                            .build(),
                    )
                    .build(),
            )
            .build()
    }

    private fun renderMethod(method: WinMdMethod, currentNamespace: String): FunSpec? {
        if (!isKotlinIdentifier(method.name)) {
            return null
        }
        if (!supportsInterfaceMethod(method)) {
            return null
        }
        val functionName = kotlinMethodName(method.name)
        val builder = FunSpec.builder(functionName)
            .returns(typeNameMapper.mapTypeName(method.returnType, currentNamespace))
            .addParameters(method.parameters.map { parameter ->
                ParameterSpec.builder(
                    parameter.name.replaceFirstChar(Char::lowercase),
                    typeNameMapper.mapTypeName(parameter.type, currentNamespace),
                ).build()
            })

        return when {
            method.returnType == "String" && method.parameters.isEmpty() && method.vtableIndex != null -> {
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement("val value = %T.invokeHStringMethod(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)
                    .beginControlFlow("return try")
                    .addStatement("%T.toKotlin(value)", PoetSymbols.winRtStringsClass)
                    .nextControlFlow("finally")
                    .addStatement("%T.release(value)", PoetSymbols.winRtStringsClass)
                    .endControlFlow()
                    .build()
            }
            method.returnType == "String" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "val value = %T.invokeHStringMethodWithStringArg(pointer, %L, %N).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .beginControlFlow("return try")
                    .addStatement("%T.toKotlin(value)", PoetSymbols.winRtStringsClass)
                    .nextControlFlow("finally")
                    .addStatement("%T.release(value)", PoetSymbols.winRtStringsClass)
                    .endControlFlow()
                    .build()
            }
            else -> null
        }
    }

    private fun kotlinMethodName(methodName: String): String {
        return when (methodName) {
            "ToString" -> "toStringValue"
            else -> methodName.replaceFirstChar(Char::lowercase)
        }
    }

    private fun supportsInterfaceMethod(method: WinMdMethod): Boolean {
        return (method.returnType == "String" && method.parameters.isEmpty() && method.vtableIndex != null) ||
            (method.returnType == "String" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null)
    }
}
