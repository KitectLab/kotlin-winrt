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
            signatureKey != null && MethodRuleRegistry.sharedMethodRuleFamily(signatureKey) != null -> runtimeMethodPlanForKey(signatureKey)
            signatureKey == MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.EMPTY) -> runtimeMethodPlanForKey(signatureKey)
            signatureKey == MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.EMPTY) -> runtimeMethodPlanForKey(signatureKey)
            signatureKey == MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.EMPTY) -> runtimeMethodPlanForKey(signatureKey)
            signatureKey == MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.EMPTY) -> runtimeMethodPlanForKey(signatureKey)
            signatureKey == MethodSignatureKey(MethodReturnKind.GUID, MethodSignatureShape.EMPTY) -> runtimeMethodPlanForKey(signatureKey)
            else -> null
        }
    }

    private fun runtimeMethodPlanForKey(signatureKey: MethodSignatureKey): RuntimeMethodPlan? {
        return when (signatureKey) {
            MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.EMPTY) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %S", arrayOf("")) },
                returnStatement = "return %L",
                statementArgs = { method, _, _ ->
                    arrayOf(HStringSupport.fromCall(AbiCallCatalog.hstringMethod(method.vtableIndex!!)))
                },
            )
            MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.STRING) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %S", arrayOf("")) },
                returnStatement = "return %L",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(HStringSupport.fromCall(AbiCallCatalog.hstringMethodWithString(method.vtableIndex!!, parameterBindings.single().name)))
                },
            )
            MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.INT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %S", arrayOf("")) },
                returnStatement = "return %L",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(HStringSupport.fromCall(AbiCallCatalog.hstringMethodWithInt32(method.vtableIndex!!, "${parameterBindings.single().name}.value")))
                },
            )
            MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.UINT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %S", arrayOf("")) },
                returnStatement = "return %L",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(HStringSupport.fromCall(AbiCallCatalog.hstringMethodWithUInt32(method.vtableIndex!!, "${parameterBindings.single().name}.value")))
                },
            )
            MethodSignatureKey(MethodReturnKind.FLOAT32, MethodSignatureShape.EMPTY) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0f)", arrayOf(PoetSymbols.float32Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, _ ->
                    arrayOf(PoetSymbols.float32Class, AbiCallCatalog.float32Method(method.vtableIndex!!))
                },
            )
            MethodSignatureKey(MethodReturnKind.FLOAT64, MethodSignatureShape.EMPTY) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0.0)", arrayOf(PoetSymbols.float64Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, _ ->
                    arrayOf(PoetSymbols.float64Class, AbiCallCatalog.float64Method(method.vtableIndex!!))
                },
            )
            MethodSignatureKey(MethodReturnKind.FLOAT64, MethodSignatureShape.STRING) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0.0)", arrayOf(PoetSymbols.float64Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.float64Class, AbiCallCatalog.float64MethodWithString(method.vtableIndex!!, parameterBindings.single().name))
                },
            )
            MethodSignatureKey(MethodReturnKind.FLOAT64, MethodSignatureShape.UINT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0.0)", arrayOf(PoetSymbols.float64Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.float64Class, AbiCallCatalog.float64MethodWithUInt32(method.vtableIndex!!, parameterBindings.single().name))
                },
            )
            MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.EMPTY) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T.FALSE", arrayOf(PoetSymbols.winRtBooleanClass)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, _ ->
                    arrayOf(PoetSymbols.winRtBooleanClass, AbiCallCatalog.booleanMethod(method.vtableIndex!!))
                },
            )
            MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.STRING) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T.FALSE", arrayOf(PoetSymbols.winRtBooleanClass)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.winRtBooleanClass, AbiCallCatalog.booleanMethodWithString(method.vtableIndex!!, parameterBindings.single().name))
                },
            )
            MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.UINT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T.FALSE", arrayOf(PoetSymbols.winRtBooleanClass)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.winRtBooleanClass, AbiCallCatalog.booleanMethodWithUInt32(method.vtableIndex!!, parameterBindings.single().name))
                },
            )
            MethodSignatureKey(MethodReturnKind.EVENT_REGISTRATION_TOKEN, MethodSignatureShape.EMPTY) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0)", arrayOf(PoetSymbols.eventRegistrationTokenClass)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, _ ->
                    arrayOf(PoetSymbols.eventRegistrationTokenClass, AbiCallCatalog.int64Getter(method.vtableIndex!!))
                },
            )
            MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.EMPTY) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0)", arrayOf(PoetSymbols.int32Class)) },
                returnStatement = "return %T(%T.invokeInt32Method(pointer, %L).getOrThrow())",
                statementArgs = { method, _, _ ->
                    arrayOf(PoetSymbols.int32Class, PoetSymbols.platformComInteropClass, method.vtableIndex!!)
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.EMPTY) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0u)", arrayOf(PoetSymbols.uint32Class)) },
                returnStatement = "return %T(%T.invokeUInt32Method(pointer, %L).getOrThrow())",
                statementArgs = { method, _, _ ->
                    arrayOf(PoetSymbols.uint32Class, PoetSymbols.platformComInteropClass, method.vtableIndex!!)
                },
            )
            MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.EMPTY) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0L)", arrayOf(PoetSymbols.int64Class)) },
                returnStatement = "return %T(%T.invokeInt64Getter(pointer, %L).getOrThrow())",
                statementArgs = { method, _, _ ->
                    arrayOf(PoetSymbols.int64Class, PoetSymbols.platformComInteropClass, method.vtableIndex!!)
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.EMPTY) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0uL)", arrayOf(PoetSymbols.uint64Class)) },
                returnStatement = "return %T(%T.invokeInt64Getter(pointer, %L).getOrThrow().toULong())",
                statementArgs = { method, _, _ ->
                    arrayOf(PoetSymbols.uint64Class, PoetSymbols.platformComInteropClass, method.vtableIndex!!)
                },
            )
            MethodSignatureKey(MethodReturnKind.GUID, MethodSignatureShape.EMPTY) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(%S)", arrayOf(PoetSymbols.guidValueClass, "")) },
                returnStatement = "return %T(%T.invokeGuidGetter(pointer, %L).getOrThrow().toString())",
                statementArgs = { method, _, _ ->
                    arrayOf(PoetSymbols.guidValueClass, PoetSymbols.platformComInteropClass, method.vtableIndex!!)
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.EMPTY) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return") },
                returnStatement = "%L",
                statementArgs = { method, _, _ ->
                    arrayOf(AbiCallCatalog.unitMethod(method.vtableIndex!!))
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.INT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return") },
                returnStatement = "%L",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(AbiCallCatalog.unitMethodWithInt32(method.vtableIndex!!, parameterBindings.single().name))
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.INT64) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return") },
                returnStatement = "%L",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(AbiCallCatalog.unitMethodWithInt64(method.vtableIndex!!, parameterBindings.single().name))
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.EVENT_REGISTRATION_TOKEN) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return") },
                returnStatement = "%L",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(AbiCallCatalog.unitMethodWithInt64(method.vtableIndex!!, parameterBindings.single().name))
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.STRING) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return") },
                returnStatement = "%L",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(AbiCallCatalog.unitMethodWithString(method.vtableIndex!!, parameterBindings.single().name))
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.OBJECT) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return") },
                returnStatement = "%L",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(AbiCallCatalog.objectSetter(method.vtableIndex!!, parameterBindings.single().name))
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.EMPTY) -> RuntimeMethodPlan(
                nullPointerReturn = { method -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}")) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, currentNamespace, _ ->
                    arrayOf(
                        typeNameMapper.mapTypeName(method.returnType, currentNamespace),
                        AbiCallCatalog.objectMethod(method.vtableIndex!!),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.STRING) -> RuntimeMethodPlan(
                nullPointerReturn = { method -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}")) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, currentNamespace, parameterBindings ->
                    arrayOf(
                        typeNameMapper.mapTypeName(method.returnType, currentNamespace),
                        AbiCallCatalog.objectMethodWithString(method.vtableIndex!!, parameterBindings.single().name),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.UINT32) -> RuntimeMethodPlan(
                nullPointerReturn = { method -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}")) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, currentNamespace, parameterBindings ->
                    arrayOf(
                        typeNameMapper.mapTypeName(method.returnType, currentNamespace),
                        AbiCallCatalog.objectMethodWithUInt32(method.vtableIndex!!, parameterBindings.single().name),
                    )
                },
            )
            else -> null
        }
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
