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
            (method.returnType == "UInt32" && method.parameters.isEmpty() && method.vtableIndex != null)
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
        if (method.returnType == "UInt32" && method.parameters.isEmpty() && method.vtableIndex != null) {
            val vtableIndex = method.vtableIndex!!
            return builder
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("return %T(0u)", PoetSymbols.uint32Class)
                .endControlFlow()
                .addStatement("return %T(%T.invokeUInt32Method(pointer, %L).getOrThrow())", PoetSymbols.uint32Class, PoetSymbols.platformComInteropClass, vtableIndex)
                .build()
        }

        return null
    }
}
