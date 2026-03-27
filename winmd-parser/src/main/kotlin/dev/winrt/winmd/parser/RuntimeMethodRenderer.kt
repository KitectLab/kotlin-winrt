package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.FunSpec
import dev.winrt.winmd.plugin.WinMdMethod

internal class RuntimeMethodRenderer(
    private val typeNameMapper: TypeNameMapper,
) {
    fun canRenderRuntimeMethod(method: WinMdMethod): Boolean {
        if (!isKotlinIdentifier(method.name)) {
            return false
        }
        return (method.returnType == "Unit" && method.parameters.isEmpty() && method.vtableIndex != null) ||
            (method.returnType == "Unit" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "Int32" &&
                method.vtableIndex != null) ||
            (method.returnType == "Unit" &&
                method.parameters.size == 1 &&
                supportsRuntimeObjectType(method.parameters[0].type) &&
                method.vtableIndex != null) ||
            (method.returnType == "UInt32" && method.parameters.isEmpty() && method.vtableIndex != null) ||
            (method.returnType == "Int32" && method.parameters.isEmpty() && method.vtableIndex != null) ||
            (supportsRuntimeObjectType(method.returnType) && method.parameters.isEmpty() && method.vtableIndex != null) ||
            (supportsRuntimeObjectType(method.returnType) &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null)
    }

    fun renderRuntimeMethod(method: WinMdMethod, currentNamespace: String): FunSpec? {
        if (!canRenderRuntimeMethod(method)) {
            return null
        }
        val functionName = method.name.replaceFirstChar(Char::lowercase)
        val kotlinType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
        val builder = FunSpec.builder(functionName).returns(kotlinType)

        if (method.returnType == "Unit" && method.parameters.isEmpty() && method.vtableIndex != null) {
            val vtableIndex = method.vtableIndex!!
            return builder
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("return")
                .endControlFlow()
                .addStatement("%T.invokeUnitMethod(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)
                .build()
        }
        if (method.returnType == "Unit" &&
            method.parameters.size == 1 &&
            method.parameters[0].type == "Int32" &&
            method.vtableIndex != null
        ) {
            val vtableIndex = method.vtableIndex!!
            val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
            return builder
                .addParameter(argumentName, PoetSymbols.int32Class)
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("return")
                .endControlFlow()
                .addStatement("%T.invokeUnitMethodWithInt32Arg(pointer, %L, %N.value).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)
                .build()
        }
        if (method.returnType == "Unit" &&
            method.parameters.size == 1 &&
            supportsRuntimeObjectType(method.parameters[0].type) &&
            method.vtableIndex != null
        ) {
            val vtableIndex = method.vtableIndex!!
            val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
            return builder
                .addParameter(argumentName, typeNameMapper.mapTypeName(method.parameters[0].type, currentNamespace))
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("return")
                .endControlFlow()
                .addStatement("%T.invokeObjectSetter(pointer, %L, %N.pointer).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)
                .build()
        }
        if (method.returnType == "UInt32" && method.parameters.isEmpty() && method.vtableIndex != null) {
            val vtableIndex = method.vtableIndex!!
            return builder
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("return %T(0u)", PoetSymbols.uint32Class)
                .endControlFlow()
                .addStatement("return %T(%T.invokeUInt32Method(pointer, %L).getOrThrow())", PoetSymbols.uint32Class, PoetSymbols.platformComInteropClass, vtableIndex)
                .build()
        }
        if (method.returnType == "Int32" && method.parameters.isEmpty() && method.vtableIndex != null) {
            val vtableIndex = method.vtableIndex!!
            return builder
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("return %T(0)", PoetSymbols.int32Class)
                .endControlFlow()
                .addStatement("return %T(%T.invokeInt32Method(pointer, %L).getOrThrow())", PoetSymbols.int32Class, PoetSymbols.platformComInteropClass, vtableIndex)
                .build()
        }
        if (supportsRuntimeObjectType(method.returnType) &&
            method.parameters.isEmpty() &&
            method.vtableIndex != null
        ) {
            val vtableIndex = method.vtableIndex!!
            val returnType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
            return builder
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("error(%S)", "Null runtime object pointer: ${method.name}")
                .endControlFlow()
                .addStatement("return %T(%T.invokeObjectMethod(pointer, %L).getOrThrow())", returnType, PoetSymbols.platformComInteropClass, vtableIndex)
                .build()
        }
        if (supportsRuntimeObjectType(method.returnType) &&
            method.parameters.size == 1 &&
            method.parameters[0].type == "UInt32" &&
            method.vtableIndex != null
        ) {
            val vtableIndex = method.vtableIndex!!
            val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
            val returnType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
            return builder
                .addParameter(argumentName, PoetSymbols.uint32Class)
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("error(%S)", "Null runtime object pointer: ${method.name}")
                .endControlFlow()
                .addStatement(
                    "return %T(%T.invokeObjectMethodWithUInt32Arg(pointer, %L, %N.value).getOrThrow())",
                    returnType,
                    PoetSymbols.platformComInteropClass,
                    vtableIndex,
                    argumentName,
                )
                .build()
        }

        return null
    }

    private fun supportsRuntimeObjectType(type: String): Boolean {
        return (type == "Object" || type.contains('.')) &&
            !type.contains('`') &&
            !type.contains('<') &&
            !type.endsWith("[]")
    }
}
