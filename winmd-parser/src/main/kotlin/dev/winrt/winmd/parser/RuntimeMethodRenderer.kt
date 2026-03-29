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
        return runtimeMethodPlan(method) != null
    }

    fun renderRuntimeMethod(method: WinMdMethod, currentNamespace: String): FunSpec? {
        val plan = runtimeMethodPlan(method) ?: return null
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
        val parameterBindings = bindParameters(builder, method, currentNamespace)
        return builder
            .beginControlFlow("if (pointer.isNull)")
            .addPlannedStatement(plan.nullPointerReturn(method))
            .endControlFlow()
            .addStatement(
                plan.returnStatement,
                *plan.statementArgs(method, currentNamespace, parameterBindings),
            )
            .build()
    }

    private fun bindParameters(
        builder: FunSpec.Builder,
        method: WinMdMethod,
        currentNamespace: String,
    ): List<RuntimeMethodParameterBinding> {
        return method.parameters.map { parameter ->
            val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
            val parameterType = typeNameMapper.mapTypeName(parameter.type, currentNamespace)
            builder.addParameter(parameterName, parameterType)
            RuntimeMethodParameterBinding(
                type = parameter.type,
                name = parameterName,
            )
        }
    }

    private fun runtimeMethodPlan(method: WinMdMethod): RuntimeMethodPlan? {
        if (!isKotlinIdentifier(method.name) || method.vtableIndex == null) {
            return null
        }
        val parameterTypes = method.parameters.map { it.type }
        val signatureKey = methodSignatureKey(method.returnType, parameterTypes, ::supportsRuntimeObjectType)
        return when {
            scalarRuntimeReturnPlan(method.returnType)?.supports(parameterTypes) == true ->
                scalarRuntimePlan(scalarRuntimeReturnPlan(method.returnType)!!)
            signatureKey != null -> runtimeMethodPlanForKey(signatureKey)
            else -> null
        }
    }

    private fun runtimeMethodPlanForKey(signatureKey: MethodSignatureKey): RuntimeMethodPlan? {
        return when (signatureKey) {
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.EMPTY) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return") },
                returnStatement = "%T.invokeUnitMethod(pointer, %L).getOrThrow()",
                statementArgs = { method, _, _ ->
                    arrayOf(PoetSymbols.platformComInteropClass, method.vtableIndex!!)
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.INT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return") },
                returnStatement = "%T.invokeUnitMethodWithInt32Arg(pointer, %L, %N.value).getOrThrow()",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.platformComInteropClass, method.vtableIndex!!, parameterBindings.single().name)
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.INT64) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return") },
                returnStatement = "%T.invokeUnitMethodWithInt64Arg(pointer, %L, %N.value).getOrThrow()",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.platformComInteropClass, method.vtableIndex!!, parameterBindings.single().name)
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.STRING) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return") },
                returnStatement = "%T.invokeUnitMethodWithStringArg(pointer, %L, %N).getOrThrow()",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.platformComInteropClass, method.vtableIndex!!, parameterBindings.single().name)
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.OBJECT) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return") },
                returnStatement = "%T.invokeObjectSetter(pointer, %L, %N.pointer).getOrThrow()",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.platformComInteropClass, method.vtableIndex!!, parameterBindings.single().name)
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.EMPTY) -> RuntimeMethodPlan(
                nullPointerReturn = { method -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}")) },
                returnStatement = "return %T(%T.invokeObjectMethod(pointer, %L).getOrThrow())",
                statementArgs = { method, currentNamespace, _ ->
                    arrayOf(
                        typeNameMapper.mapTypeName(method.returnType, currentNamespace),
                        PoetSymbols.platformComInteropClass,
                        method.vtableIndex!!,
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.STRING) -> RuntimeMethodPlan(
                nullPointerReturn = { method -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}")) },
                returnStatement = "return %T(%T.invokeObjectMethodWithStringArg(pointer, %L, %N).getOrThrow())",
                statementArgs = { method, currentNamespace, parameterBindings ->
                    arrayOf(
                        typeNameMapper.mapTypeName(method.returnType, currentNamespace),
                        PoetSymbols.platformComInteropClass,
                        method.vtableIndex!!,
                        parameterBindings.single().name,
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.UINT32) -> RuntimeMethodPlan(
                nullPointerReturn = { method -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}")) },
                returnStatement = "return %T(%T.invokeObjectMethodWithUInt32Arg(pointer, %L, %N.value).getOrThrow())",
                statementArgs = { method, currentNamespace, parameterBindings ->
                    arrayOf(
                        typeNameMapper.mapTypeName(method.returnType, currentNamespace),
                        PoetSymbols.platformComInteropClass,
                        method.vtableIndex!!,
                        parameterBindings.single().name,
                    )
                },
            )
            else -> null
        }
    }

    private fun scalarRuntimePlan(plan: ScalarRuntimeReturnPlan): RuntimeMethodPlan {
        return RuntimeMethodPlan(
            nullPointerReturn = { PlannedStatement("return %L", arrayOf(plan.nullReturn)) },
            returnStatement = "return %L",
            statementArgs = { method, _, parameterBindings ->
                arrayOf(
                    plan.returnExpression(
                        method.vtableIndex!!,
                        parameterBindings.map { it.name },
                        parameterBindings.map { it.type },
                    ),
                )
            },
        )
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
        fun supports(parameterTypes: List<String>): Boolean =
            parameterTypes in supportedParameterTypes
    }

    private data class RuntimeMethodPlan(
        val nullPointerReturn: (WinMdMethod) -> PlannedStatement,
        val returnStatement: String,
        val statementArgs: (WinMdMethod, String, List<RuntimeMethodParameterBinding>) -> Array<Any>,
    )

    private data class RuntimeMethodParameterBinding(
        val type: String,
        val name: String,
    )

    private data class PlannedStatement(
        val format: String,
        val args: Array<Any> = emptyArray(),
    )

    private fun FunSpec.Builder.addPlannedStatement(statement: PlannedStatement): FunSpec.Builder {
        return addStatement(statement.format, *statement.args)
    }
}
