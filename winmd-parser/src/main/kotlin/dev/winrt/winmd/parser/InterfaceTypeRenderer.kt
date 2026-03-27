package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdProperty
import dev.winrt.winmd.plugin.WinMdType

internal class InterfaceTypeRenderer(
    private val typeNameMapper: TypeNameMapper,
    private val typeRegistry: TypeRegistry,
) {
    fun render(type: WinMdType): TypeSpec {
        val typeClass = ClassName(type.namespace.lowercase(), type.name)
        return TypeSpec.classBuilder(type.name)
            .addModifiers(KModifier.OPEN)
            .primaryConstructor(pointerConstructor())
            .superclass(PoetSymbols.winRtInterfaceProjectionClass)
            .addSuperclassConstructorParameter("pointer")
            .addProperties(type.properties.mapNotNull { renderProperty(it, type.namespace) })
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

    private fun renderProperty(property: WinMdProperty, currentNamespace: String): PropertySpec? {
        if (!supportsInterfaceProperty(property, currentNamespace)) {
            return null
        }
        val propertyName = property.name.replaceFirstChar(Char::lowercase)
        val propertyType = typeNameMapper.mapTypeName(property.type, currentNamespace)
        val getterVtableIndex = property.getterVtableIndex!!
        val getterBuilder = FunSpec.getterBuilder()
        when {
            typeRegistry.isEnumType(property.type, currentNamespace) ->
                getterBuilder.addStatement(
                    "return %T.fromValue(%T.invokeUInt32Method(pointer, %L).getOrThrow().toInt())",
                    propertyType,
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                )
            property.type == "Int32" ->
                getterBuilder.addStatement(
                    "return %T(%T.invokeInt32Method(pointer, %L).getOrThrow())",
                    PoetSymbols.int32Class,
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                )
            else -> return null
        }
        val propertyBuilder = PropertySpec.builder(propertyName, propertyType)
            .getter(getterBuilder.build())
        if (property.mutable && property.type == "Int32" && property.setterVtableIndex != null) {
            val setterVtableIndex = property.setterVtableIndex!!
            propertyBuilder.mutable()
            propertyBuilder.setter(
                FunSpec.setterBuilder()
                    .addParameter("value", propertyType)
                    .addStatement(
                        "%T.invokeInt32Setter(pointer, %L, value.value).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        setterVtableIndex,
                    )
                    .build(),
            )
        }
        return propertyBuilder.build()
    }

    private fun renderMethod(method: WinMdMethod, currentNamespace: String): FunSpec? {
        if (!isKotlinIdentifier(method.name)) {
            return null
        }
        if (!supportsInterfaceMethod(method, currentNamespace)) {
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
            method.returnType == "String" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "Int32" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "val value = %T.invokeHStringMethodWithInt32Arg(pointer, %L, %N.value).getOrThrow()",
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
            method.returnType == "String" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "val value = %T.invokeHStringMethodWithUInt32Arg(pointer, %L, %N.value).getOrThrow()",
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
            method.returnType == "Float64" && method.parameters.isEmpty() && method.vtableIndex != null -> {
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeFloat64Method(pointer, %L).getOrThrow())",
                        PoetSymbols.float64Class,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                    )
                    .build()
            }
            method.returnType == "Float64" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeFloat64MethodWithStringArg(pointer, %L, %N).getOrThrow())",
                        PoetSymbols.float64Class,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .build()
            }
            method.returnType == "Float64" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeFloat64MethodWithUInt32Arg(pointer, %L, %N.value).getOrThrow())",
                        PoetSymbols.float64Class,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .build()
            }
            method.returnType == "Boolean" && method.parameters.isEmpty() && method.vtableIndex != null -> {
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeBooleanGetter(pointer, %L).getOrThrow())",
                        PoetSymbols.winRtBooleanClass,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                    )
                    .build()
            }
            method.returnType == "Boolean" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeBooleanMethodWithUInt32Arg(pointer, %L, %N.value).getOrThrow())",
                        PoetSymbols.winRtBooleanClass,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .build()
            }
            method.returnType == "Boolean" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeBooleanMethodWithStringArg(pointer, %L, %N).getOrThrow())",
                        PoetSymbols.winRtBooleanClass,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .build()
            }
            method.returnType == "Int32" && method.parameters.isEmpty() && method.vtableIndex != null -> {
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeInt32Method(pointer, %L).getOrThrow())",
                        PoetSymbols.int32Class,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                    )
                    .build()
            }
            typeRegistry.isEnumType(method.returnType, currentNamespace) &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null -> {
                val vtableIndex = method.vtableIndex!!
                val returnType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
                builder
                    .addStatement(
                        "return %T.fromValue(%T.invokeUInt32Method(pointer, %L).getOrThrow().toInt())",
                        returnType,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                    )
                    .build()
            }
            supportsInterfaceObjectType(method.returnType) &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null -> {
                val vtableIndex = method.vtableIndex!!
                val returnType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
                builder
                    .addStatement(
                        "return %T(%T.invokeObjectMethod(pointer, %L).getOrThrow())",
                        returnType,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                    )
                    .build()
            }
            supportsInterfaceObjectType(method.returnType) &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                val returnType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
                builder
                    .addStatement(
                        "return %T(%T.invokeObjectMethodWithStringArg(pointer, %L, %N).getOrThrow())",
                        returnType,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .build()
            }
            supportsInterfaceObjectType(method.returnType) &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                val returnType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
                builder
                    .addStatement(
                        "return %T(%T.invokeObjectMethodWithUInt32Arg(pointer, %L, %N.value).getOrThrow())",
                        returnType,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .build()
            }
            method.returnType == "Unit" &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null -> {
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "%T.invokeUnitMethod(pointer, %L).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                    )
                    .build()
            }
            method.returnType == "Unit" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "%T.invokeStringSetter(pointer, %L, %N).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .build()
            }
            method.returnType == "Unit" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "Int32" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "%T.invokeUnitMethodWithInt32Arg(pointer, %L, %N.value).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .build()
            }
            method.returnType == "Unit" &&
                method.parameters.size == 1 &&
                supportsInterfaceObjectInput(method.parameters[0].type) &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "%T.invokeObjectSetter(pointer, %L, %N.pointer).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
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

    private fun supportsInterfaceMethod(method: WinMdMethod, currentNamespace: String): Boolean {
        return (method.returnType == "String" && method.parameters.isEmpty() && method.vtableIndex != null) ||
            (method.returnType == "String" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null) ||
            (method.returnType == "String" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "Int32" &&
                method.vtableIndex != null) ||
            (method.returnType == "String" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null) ||
            (method.returnType == "Float64" && method.parameters.isEmpty() && method.vtableIndex != null) ||
            (method.returnType == "Float64" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null) ||
            (method.returnType == "Float64" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null) ||
            (method.returnType == "Boolean" && method.parameters.isEmpty() && method.vtableIndex != null) ||
            (method.returnType == "Boolean" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null) ||
            (method.returnType == "Boolean" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null) ||
            (method.returnType == "Int32" && method.parameters.isEmpty() && method.vtableIndex != null) ||
            (typeRegistry.isEnumType(method.returnType, currentNamespace) &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null) ||
            (supportsInterfaceObjectType(method.returnType) &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null) ||
            (supportsInterfaceObjectType(method.returnType) &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null) ||
            (supportsInterfaceObjectType(method.returnType) &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null) ||
            (method.returnType == "Unit" &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null) ||
            (method.returnType == "Unit" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null) ||
            (method.returnType == "Unit" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "Int32" &&
                method.vtableIndex != null) ||
            (method.returnType == "Unit" &&
                method.parameters.size == 1 &&
                supportsInterfaceObjectInput(method.parameters[0].type) &&
                method.vtableIndex != null)
    }

    private fun supportsInterfaceObjectInput(type: String): Boolean {
        return (type == "Object" || type.contains('.')) &&
            !type.contains('`') &&
            !type.contains('<') &&
            !type.endsWith("[]")
    }

    private fun supportsInterfaceObjectType(type: String): Boolean {
        return (type == "Object" || type.contains('.')) &&
            !type.contains('`') &&
            !type.contains('<') &&
            !type.endsWith("[]")
    }

    private fun supportsInterfaceProperty(property: WinMdProperty, currentNamespace: String): Boolean {
        return property.getterVtableIndex != null &&
            (
                typeRegistry.isEnumType(property.type, currentNamespace) ||
                    property.type == "Int32"
                )
    }
}
