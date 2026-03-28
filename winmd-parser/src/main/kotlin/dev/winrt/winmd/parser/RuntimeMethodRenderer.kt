package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdMethod

internal class RuntimeMethodRenderer(
    private val typeNameMapper: TypeNameMapper,
    private val typeRegistry: TypeRegistry,
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

    fun renderRuntimeLambdaOverload(method: WinMdMethod, currentNamespace: String): FunSpec? {
        if (method.parameters.size != 1 || method.vtableIndex == null || method.returnType != "Unit") {
            return null
        }
        val delegateType = typeRegistry.findType(method.parameters.single().type, currentNamespace) ?: return null
        if (delegateType.kind != dev.winrt.winmd.plugin.WinMdTypeKind.Delegate) {
            return null
        }
        val invokeMethod = delegateType.methods.singleOrNull { it.name == "Invoke" } ?: return null
        if (invokeMethod.returnType != "Unit" && invokeMethod.returnType != "Boolean") {
            return null
        }

        val functionName = method.name.replaceFirstChar(Char::lowercase)
        val delegateClass = typeNameMapper.mapTypeName(method.parameters.single().type, currentNamespace)
        return when {
            invokeMethod.parameters.isEmpty() && invokeMethod.returnType == "Unit" -> {
                FunSpec.builder(functionName)
                    .returns(PoetSymbols.winRtDelegateHandleClass)
                    .addParameter("callback", LambdaTypeName.get(returnType = Unit::class.asTypeName()))
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("error(%S)", "Null runtime object pointer: ${method.name}")
                    .endControlFlow()
                    .addStatement(
                        "val delegateHandle = %T.createNoArgUnitDelegate(%T.iid, callback)",
                        PoetSymbols.winRtDelegateBridgeClass,
                        delegateClass,
                    )
                    .addStatement("%N(%T(delegateHandle.pointer))", functionName, delegateClass)
                    .addStatement("return delegateHandle")
                    .build()
            }
            invokeMethod.parameters.isEmpty() && invokeMethod.returnType == "Boolean" -> {
                FunSpec.builder(functionName)
                    .returns(PoetSymbols.winRtDelegateHandleClass)
                    .addParameter("callback", LambdaTypeName.get(returnType = Boolean::class.asTypeName()))
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("error(%S)", "Null runtime object pointer: ${method.name}")
                    .endControlFlow()
                    .addStatement(
                        "val delegateHandle = %T.createNoArgBooleanDelegate(%T.iid, callback)",
                        PoetSymbols.winRtDelegateBridgeClass,
                        delegateClass,
                    )
                    .addStatement("%N(%T(delegateHandle.pointer))", functionName, delegateClass)
                    .addStatement("return delegateHandle")
                    .build()
            }
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "Int32" &&
                invokeMethod.returnType == "Unit" -> {
                FunSpec.builder(functionName)
                    .returns(PoetSymbols.winRtDelegateHandleClass)
                    .addParameter(
                        "callback",
                        LambdaTypeName.get(parameters = arrayOf(Int::class.asTypeName()), returnType = Unit::class.asTypeName()),
                    )
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("error(%S)", "Null runtime object pointer: ${method.name}")
                    .endControlFlow()
                    .addStatement(
                        "val delegateHandle = %T.createInt32ArgUnitDelegate(%T.iid, callback)",
                        PoetSymbols.winRtDelegateBridgeClass,
                        delegateClass,
                    )
                    .addStatement("%N(%T(delegateHandle.pointer))", functionName, delegateClass)
                    .addStatement("return delegateHandle")
                    .build()
            }
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "String" &&
                invokeMethod.returnType == "Unit" -> {
                FunSpec.builder(functionName)
                    .returns(PoetSymbols.winRtDelegateHandleClass)
                    .addParameter(
                        "callback",
                        LambdaTypeName.get(parameters = arrayOf(String::class.asTypeName()), returnType = Unit::class.asTypeName()),
                    )
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("error(%S)", "Null runtime object pointer: ${method.name}")
                    .endControlFlow()
                    .addStatement(
                        "val delegateHandle = %T.createStringArgUnitDelegate(%T.iid, callback)",
                        PoetSymbols.winRtDelegateBridgeClass,
                        delegateClass,
                    )
                    .addStatement("%N(%T(delegateHandle.pointer))", functionName, delegateClass)
                    .addStatement("return delegateHandle")
                    .build()
            }
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "UInt32" &&
                invokeMethod.returnType == "Unit" -> {
                FunSpec.builder(functionName)
                    .returns(PoetSymbols.winRtDelegateHandleClass)
                    .addParameter(
                        "callback",
                        LambdaTypeName.get(parameters = arrayOf(UInt::class.asTypeName()), returnType = Unit::class.asTypeName()),
                    )
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("error(%S)", "Null runtime object pointer: ${method.name}")
                    .endControlFlow()
                    .addStatement(
                        "val delegateHandle = %T.createUInt32ArgUnitDelegate(%T.iid, callback)",
                        PoetSymbols.winRtDelegateBridgeClass,
                        delegateClass,
                    )
                    .addStatement("%N(%T(delegateHandle.pointer))", functionName, delegateClass)
                    .addStatement("return delegateHandle")
                    .build()
            }
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "UInt64" &&
                invokeMethod.returnType == "Unit" -> {
                FunSpec.builder(functionName)
                    .returns(PoetSymbols.winRtDelegateHandleClass)
                    .addParameter(
                        "callback",
                        LambdaTypeName.get(parameters = arrayOf(ULong::class.asTypeName()), returnType = Unit::class.asTypeName()),
                    )
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("error(%S)", "Null runtime object pointer: ${method.name}")
                    .endControlFlow()
                    .addStatement(
                        "val delegateHandle = %T.createUInt64ArgUnitDelegate(%T.iid, callback)",
                        PoetSymbols.winRtDelegateBridgeClass,
                        delegateClass,
                    )
                    .addStatement("%N(%T(delegateHandle.pointer))", functionName, delegateClass)
                    .addStatement("return delegateHandle")
                    .build()
            }
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "Int64" &&
                invokeMethod.returnType == "Unit" -> {
                FunSpec.builder(functionName)
                    .returns(PoetSymbols.winRtDelegateHandleClass)
                    .addParameter(
                        "callback",
                        LambdaTypeName.get(parameters = arrayOf(Long::class.asTypeName()), returnType = Unit::class.asTypeName()),
                    )
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("error(%S)", "Null runtime object pointer: ${method.name}")
                    .endControlFlow()
                    .addStatement(
                        "val delegateHandle = %T.createInt64ArgUnitDelegate(%T.iid, callback)",
                        PoetSymbols.winRtDelegateBridgeClass,
                        delegateClass,
                    )
                    .addStatement("%N(%T(delegateHandle.pointer))", functionName, delegateClass)
                    .addStatement("return delegateHandle")
                    .build()
            }
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "Boolean" &&
                invokeMethod.returnType == "Unit" -> {
                FunSpec.builder(functionName)
                    .returns(PoetSymbols.winRtDelegateHandleClass)
                    .addParameter(
                        "callback",
                        LambdaTypeName.get(parameters = arrayOf(Boolean::class.asTypeName()), returnType = Unit::class.asTypeName()),
                    )
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("error(%S)", "Null runtime object pointer: ${method.name}")
                    .endControlFlow()
                    .addStatement(
                        "val delegateHandle = %T.createBooleanArgUnitDelegate(%T.iid, callback)",
                        PoetSymbols.winRtDelegateBridgeClass,
                        delegateClass,
                    )
                    .addStatement("%N(%T(delegateHandle.pointer))", functionName, delegateClass)
                    .addStatement("return delegateHandle")
                    .build()
            }
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "Float32" &&
                invokeMethod.returnType == "Unit" -> {
                FunSpec.builder(functionName)
                    .returns(PoetSymbols.winRtDelegateHandleClass)
                    .addParameter(
                        "callback",
                        LambdaTypeName.get(parameters = arrayOf(Float::class.asTypeName()), returnType = Unit::class.asTypeName()),
                    )
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("error(%S)", "Null runtime object pointer: ${method.name}")
                    .endControlFlow()
                    .addStatement(
                        "val delegateHandle = %T.createFloat32ArgUnitDelegate(%T.iid, callback)",
                        PoetSymbols.winRtDelegateBridgeClass,
                        delegateClass,
                    )
                    .addStatement("%N(%T(delegateHandle.pointer))", functionName, delegateClass)
                    .addStatement("return delegateHandle")
                    .build()
            }
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "Float64" &&
                invokeMethod.returnType == "Unit" -> {
                FunSpec.builder(functionName)
                    .returns(PoetSymbols.winRtDelegateHandleClass)
                    .addParameter(
                        "callback",
                        LambdaTypeName.get(parameters = arrayOf(Double::class.asTypeName()), returnType = Unit::class.asTypeName()),
                    )
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("error(%S)", "Null runtime object pointer: ${method.name}")
                    .endControlFlow()
                    .addStatement(
                        "val delegateHandle = %T.createFloat64ArgUnitDelegate(%T.iid, callback)",
                        PoetSymbols.winRtDelegateBridgeClass,
                        delegateClass,
                    )
                    .addStatement("%N(%T(delegateHandle.pointer))", functionName, delegateClass)
                    .addStatement("return delegateHandle")
                    .build()
            }
            invokeMethod.parameters.size == 1 &&
                supportsRuntimeObjectType(invokeMethod.parameters.single().type) &&
                invokeMethod.returnType == "Unit" -> {
                val callbackArgType = typeNameMapper.mapTypeName(invokeMethod.parameters.single().type, currentNamespace)
                FunSpec.builder(functionName)
                    .returns(PoetSymbols.winRtDelegateHandleClass)
                    .addParameter(
                        "callback",
                        LambdaTypeName.get(parameters = arrayOf(callbackArgType), returnType = Unit::class.asTypeName()),
                    )
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("error(%S)", "Null runtime object pointer: ${method.name}")
                    .endControlFlow()
                    .addStatement(
                        "val delegateHandle = %T.createObjectArgUnitDelegate(%T.iid) { arg -> callback(%T(arg)) }",
                        PoetSymbols.winRtDelegateBridgeClass,
                        delegateClass,
                        callbackArgType,
                    )
                    .addStatement("%N(%T(delegateHandle.pointer))", functionName, delegateClass)
                    .addStatement("return delegateHandle")
                    .build()
            }
            invokeMethod.parameters.size == 1 &&
                supportsRuntimeObjectType(invokeMethod.parameters.single().type) &&
                invokeMethod.returnType == "Boolean" -> {
                val callbackArgType = typeNameMapper.mapTypeName(invokeMethod.parameters.single().type, currentNamespace)
                FunSpec.builder(functionName)
                    .returns(PoetSymbols.winRtDelegateHandleClass)
                    .addParameter(
                        "callback",
                        LambdaTypeName.get(parameters = arrayOf(callbackArgType), returnType = Boolean::class.asTypeName()),
                    )
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("error(%S)", "Null runtime object pointer: ${method.name}")
                    .endControlFlow()
                    .addStatement(
                        "val delegateHandle = %T.createObjectArgBooleanDelegate(%T.iid) { arg -> callback(%T(arg)) }",
                        PoetSymbols.winRtDelegateBridgeClass,
                        delegateClass,
                        callbackArgType,
                    )
                    .addStatement("%N(%T(delegateHandle.pointer))", functionName, delegateClass)
                    .addStatement("return delegateHandle")
                    .build()
            }
            else -> null
        }
    }

    private fun supportsRuntimeObjectType(type: String): Boolean {
        return (type == "Object" || type.contains('.')) &&
            !type.contains('`') &&
            !type.contains('<') &&
            !type.endsWith("[]")
    }
}
