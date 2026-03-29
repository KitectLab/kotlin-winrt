package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdMethod

internal class RuntimeMethodRenderer(
    private val typeNameMapper: TypeNameMapper,
    private val delegateLambdaPlanResolver: DelegateLambdaPlanResolver,
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
                method.parameters[0].type == "Int64" &&
                method.vtableIndex != null) ||
            (method.returnType == "Unit" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null) ||
            (method.returnType == "Unit" &&
                method.parameters.size == 1 &&
                supportsRuntimeObjectType(method.parameters[0].type) &&
                method.vtableIndex != null) ||
            (scalarRuntimeReturnPlan(method.returnType) != null &&
                scalarRuntimeReturnPlan(method.returnType)!!.supports(method) &&
                method.vtableIndex != null) ||
            (supportsRuntimeObjectType(method.returnType) && method.parameters.isEmpty() && method.vtableIndex != null) ||
            (supportsRuntimeObjectType(method.returnType) &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null) ||
            (supportsRuntimeObjectType(method.returnType) &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null)
    }

    fun renderRuntimeMethod(method: WinMdMethod, currentNamespace: String): FunSpec? {
        if (!canRenderRuntimeMethod(method)) {
            return null
        }
        val functionName = if (method.name == "ToString" && method.returnType == "String" && method.parameters.isEmpty()) {
            "toString"
        } else {
            method.name.replaceFirstChar(Char::lowercase)
        }
        val kotlinType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
        val builder = FunSpec.builder(functionName).returns(kotlinType)
        if (method.name == "ToString" && method.returnType == "String" && method.parameters.isEmpty()) {
            builder.addModifiers(KModifier.OVERRIDE)
        }

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
            method.parameters[0].type == "Int64" &&
            method.vtableIndex != null
        ) {
            val vtableIndex = method.vtableIndex!!
            val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
            return builder
                .addParameter(argumentName, PoetSymbols.int64Class)
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("return")
                .endControlFlow()
                .addStatement("%T.invokeUnitMethodWithInt64Arg(pointer, %L, %N.value).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)
                .build()
        }
        if (method.returnType == "Unit" &&
            method.parameters.size == 1 &&
            method.parameters[0].type == "String" &&
            method.vtableIndex != null
        ) {
            val vtableIndex = method.vtableIndex!!
            val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
            return builder
                .addParameter(argumentName, String::class.asTypeName())
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("return")
                .endControlFlow()
                .addStatement("%T.invokeUnitMethodWithStringArg(pointer, %L, %N).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex, argumentName)
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
        val scalarPlan = scalarRuntimeReturnPlan(method.returnType)
        if (scalarPlan != null && scalarPlan.supports(method) && method.vtableIndex != null) {
            val vtableIndex = method.vtableIndex!!
            method.parameters.forEach { parameter ->
                builder.addParameter(
                    parameter.name.replaceFirstChar(Char::lowercase),
                    typeNameMapper.mapTypeName(parameter.type, currentNamespace),
                )
            }
            return builder
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("return %L", scalarPlan.nullReturn)
                .endControlFlow()
                .addStatement(
                    "return %L",
                    scalarPlan.returnExpression(
                        vtableIndex,
                        method.parameters.map { it.name.replaceFirstChar(Char::lowercase) },
                        method.parameters.map { it.type },
                    ),
                )
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
            method.parameters[0].type == "String" &&
            method.vtableIndex != null
        ) {
            val vtableIndex = method.vtableIndex!!
            val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
            val returnType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
            return builder
                .addParameter(argumentName, String::class.asTypeName())
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("error(%S)", "Null runtime object pointer: ${method.name}")
                .endControlFlow()
                .addStatement(
                    "return %T(%T.invokeObjectMethodWithStringArg(pointer, %L, %N).getOrThrow())",
                    returnType,
                    PoetSymbols.platformComInteropClass,
                    vtableIndex,
                    argumentName,
                )
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
        val plan = delegateLambdaPlanResolver.resolve(
            invokeMethod = invokeMethod,
            currentNamespace = currentNamespace,
            supportsObjectType = ::supportsRuntimeObjectType,
        ) as? DelegateLambdaPlan.PlannedBridge ?: return null
        return FunSpec.builder(functionName)
            .returns(PoetSymbols.winRtDelegateHandleClass)
            .addParameter("callback", plan.lambdaType)
            .beginControlFlow("if (pointer.isNull)")
            .addStatement("error(%S)", "Null runtime object pointer: ${method.name}")
            .endControlFlow()
            .apply {
                val argumentKindsLiteral = if (plan.bridge.argumentKinds.isEmpty()) {
                    "emptyList()"
                } else {
                    plan.bridge.argumentKinds.joinToString(
                        prefix = "listOf(",
                        postfix = ")",
                    ) { kind -> "${PoetSymbols.winRtDelegateValueKindClass.canonicalName}.${kind.name}" }
                }
                val callbackInvocation = buildDelegateCallbackInvocation(plan)
                addStatement(
                    "val delegateHandle = %T.%L(%T.iid, %L) { args -> %L }",
                    PoetSymbols.winRtDelegateBridgeClass,
                    plan.bridge.factoryMethod,
                    delegateClass,
                    argumentKindsLiteral,
                    callbackInvocation,
                )
                addStatement("%N(%T(delegateHandle.pointer))", functionName, delegateClass)
                addStatement("return delegateHandle")
            }
            .build()
    }

    private fun buildDelegateCallbackInvocation(plan: DelegateLambdaPlan.PlannedBridge): com.squareup.kotlinpoet.CodeBlock {
        if (plan.lambdaType.parameters.isEmpty()) {
            return com.squareup.kotlinpoet.CodeBlock.of("callback()")
        }
        val builder = com.squareup.kotlinpoet.CodeBlock.builder().add("callback(")
        plan.lambdaType.parameters.indices.forEachIndexed { position, index ->
            if (position > 0) {
                builder.add(", ")
            }
            val parameterType = plan.lambdaType.parameters[index]
            when (plan.bridge.argumentKinds[index]) {
                DelegateArgumentKind.OBJECT -> builder.add("%L(args[%L] as %T)", parameterType, index, PoetSymbols.comPtrClass)
                else -> builder.add("args[%L] as %L", index, parameterType)
            }
        }
        return builder.add(")").build()
    }

    private fun supportsRuntimeObjectType(type: String): Boolean {
        return (type == "Object" || type.contains('.')) &&
            !type.contains('`') &&
            !type.contains('<') &&
            !type.endsWith("[]")
    }

    private fun scalarRuntimeReturnPlan(type: String): ScalarRuntimeReturnPlan? {
        return when (type) {
            "String" -> ScalarRuntimeReturnPlan(
                nullReturn = CodeBlock.of("%S", ""),
                returnExpression = { vtableIndex, argumentNames, parameterTypes ->
                    when (parameterTypes) {
                        emptyList<String>() -> HStringSupport.toKotlinString("pointer", vtableIndex)
                        listOf("String") -> HStringSupport.toKotlinStringWithStringArg("pointer", vtableIndex, argumentNames.single())
                        listOf("UInt32") -> HStringSupport.toKotlinStringWithUInt32Arg("pointer", vtableIndex, "${argumentNames.single()}.value")
                        else -> error("Unsupported String runtime method parameters: $parameterTypes")
                    }
                },
                supportedParameterTypes = setOf(
                    emptyList<String>(),
                    listOf("String"),
                    listOf("UInt32"),
                ),
            )
            "Boolean" -> ScalarRuntimeReturnPlan(
                nullReturn = CodeBlock.of("%T.FALSE", PoetSymbols.winRtBooleanClass),
                returnExpression = { vtableIndex, argumentNames, parameterTypes ->
                    when (parameterTypes) {
                        emptyList<String>() -> CodeBlock.of(
                            "%T(%T.invokeBooleanGetter(pointer, %L).getOrThrow())",
                            PoetSymbols.winRtBooleanClass,
                            PoetSymbols.platformComInteropClass,
                            vtableIndex,
                        )
                        listOf("String") -> CodeBlock.of(
                            "%T(%T.invokeBooleanMethodWithStringArg(pointer, %L, %N).getOrThrow())",
                            PoetSymbols.winRtBooleanClass,
                            PoetSymbols.platformComInteropClass,
                            vtableIndex,
                            argumentNames.single(),
                        )
                        listOf("UInt32") -> CodeBlock.of(
                            "%T(%T.invokeBooleanMethodWithUInt32Arg(pointer, %L, %N.value).getOrThrow())",
                            PoetSymbols.winRtBooleanClass,
                            PoetSymbols.platformComInteropClass,
                            vtableIndex,
                            argumentNames.single(),
                        )
                        else -> error("Unsupported Boolean runtime method parameters: $parameterTypes")
                    }
                },
                supportedParameterTypes = setOf(emptyList<String>(), listOf("String"), listOf("UInt32")),
            )
            "Int32" -> ScalarRuntimeReturnPlan(
                nullReturn = CodeBlock.of("%T(0)", PoetSymbols.int32Class),
                returnExpression = { vtableIndex, _, _ ->
                    CodeBlock.of(
                        "%T(%T.invokeInt32Method(pointer, %L).getOrThrow())",
                        PoetSymbols.int32Class,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                    )
                },
                supportedParameterTypes = setOf(emptyList<String>()),
            )
            "UInt32" -> ScalarRuntimeReturnPlan(
                nullReturn = CodeBlock.of("%T(0u)", PoetSymbols.uint32Class),
                returnExpression = { vtableIndex, _, _ ->
                    CodeBlock.of(
                        "%T(%T.invokeUInt32Method(pointer, %L).getOrThrow())",
                        PoetSymbols.uint32Class,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                    )
                },
                supportedParameterTypes = setOf(emptyList<String>()),
            )
            "Int64" -> ScalarRuntimeReturnPlan(
                nullReturn = CodeBlock.of("%T(0L)", PoetSymbols.int64Class),
                returnExpression = { vtableIndex, _, _ ->
                    CodeBlock.of(
                        "%T(%T.invokeInt64Getter(pointer, %L).getOrThrow())",
                        PoetSymbols.int64Class,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                    )
                },
                supportedParameterTypes = setOf(emptyList<String>()),
            )
            "UInt64" -> ScalarRuntimeReturnPlan(
                nullReturn = CodeBlock.of("%T(0uL)", PoetSymbols.uint64Class),
                returnExpression = { vtableIndex, _, _ ->
                    CodeBlock.of(
                        "%T(%T.invokeInt64Getter(pointer, %L).getOrThrow().toULong())",
                        PoetSymbols.uint64Class,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                    )
                },
                supportedParameterTypes = setOf(emptyList<String>()),
            )
            "Float64" -> ScalarRuntimeReturnPlan(
                nullReturn = CodeBlock.of("%T(0.0)", PoetSymbols.float64Class),
                returnExpression = { vtableIndex, argumentNames, parameterTypes ->
                    when (parameterTypes) {
                        emptyList<String>() -> CodeBlock.of(
                            "%T(%T.invokeFloat64Method(pointer, %L).getOrThrow())",
                            PoetSymbols.float64Class,
                            PoetSymbols.platformComInteropClass,
                            vtableIndex,
                        )
                        listOf("String") -> CodeBlock.of(
                            "%T(%T.invokeFloat64MethodWithStringArg(pointer, %L, %N).getOrThrow())",
                            PoetSymbols.float64Class,
                            PoetSymbols.platformComInteropClass,
                            vtableIndex,
                            argumentNames.single(),
                        )
                        listOf("UInt32") -> CodeBlock.of(
                            "%T(%T.invokeFloat64MethodWithUInt32Arg(pointer, %L, %N.value).getOrThrow())",
                            PoetSymbols.float64Class,
                            PoetSymbols.platformComInteropClass,
                            vtableIndex,
                            argumentNames.single(),
                        )
                        else -> error("Unsupported Float64 runtime method parameters: $parameterTypes")
                    }
                },
                supportedParameterTypes = setOf(emptyList<String>(), listOf("String"), listOf("UInt32")),
            )
            "Guid" -> ScalarRuntimeReturnPlan(
                nullReturn = CodeBlock.of("%T(%S)", PoetSymbols.guidValueClass, ""),
                returnExpression = { vtableIndex, _, _ ->
                    CodeBlock.of(
                        "%T(%T.invokeGuidGetter(pointer, %L).getOrThrow().toString())",
                        PoetSymbols.guidValueClass,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                    )
                },
                supportedParameterTypes = setOf(emptyList<String>()),
            )
            else -> null
        }
    }

    private data class ScalarRuntimeReturnPlan(
        val nullReturn: CodeBlock,
        val returnExpression: (Int, List<String>, List<String>) -> CodeBlock,
        val supportedParameterTypes: Set<List<String>>,
    ) {
        fun supports(method: WinMdMethod): Boolean =
            method.parameters.map { it.type } in supportedParameterTypes
    }
}
