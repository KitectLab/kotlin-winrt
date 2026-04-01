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
    private val asyncMethodRuleRegistry: AsyncMethodRuleRegistry,
) {
    fun renderRuntimeMethods(method: WinMdMethod, currentNamespace: String): List<FunSpec> {
        return listOfNotNull(
            renderRuntimeMethod(method, currentNamespace),
            renderAsyncAwaitMethod(method, currentNamespace),
        )
    }

    fun canRenderRuntimeMethod(method: WinMdMethod, currentNamespace: String): Boolean {
        return asyncMethodRuleRegistry.plan(method, currentNamespace) != null ||
            (typeRegistry.isEnumType(method.returnType, currentNamespace) &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null) ||
            runtimeMethodPlan(method) != null
    }

    fun renderRuntimeMethod(method: WinMdMethod, currentNamespace: String): FunSpec? {
        renderAsyncTaskMethod(method, currentNamespace)?.let { return it }
        if (typeRegistry.isEnumType(method.returnType, currentNamespace) && method.parameters.isEmpty() && method.vtableIndex != null) {
            val functionName = if (method.name == "ToString" && method.returnType == "String" && method.parameters.isEmpty()) {
                "toString"
            } else {
                method.name.replaceFirstChar(Char::lowercase)
            }
            val returnType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
            return FunSpec.builder(functionName)
                .returns(returnType)
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("error(%S)", "Null runtime object pointer: ${method.name}")
                .endControlFlow()
                .addStatement(
                    "return %T.fromValue(%T.invokeUInt32Method(pointer, %L).getOrThrow().toInt())",
                    returnType,
                    PoetSymbols.platformComInteropClass,
                    method.vtableIndex!!,
                )
                .build()
        }
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

    private fun renderAsyncTaskMethod(method: WinMdMethod, currentNamespace: String): FunSpec? {
        val asyncPlan = asyncMethodRuleRegistry.plan(method, currentNamespace) ?: return null
        val functionName = method.name.replaceFirstChar(Char::lowercase)
        val builder = FunSpec.builder(functionName).returns(asyncPlan.rawReturnType)
        method.parameters.forEach { parameter ->
            val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
            builder.addParameter(parameterName, typeNameMapper.mapTypeName(parameter.type, currentNamespace))
        }
        builder.beginControlFlow("if (pointer.isNull)")
            .addStatement("error(%S)", "Null runtime object pointer: ${method.name}")
            .endControlFlow()
        asyncPlan.rawTaskCallFactory.create(asyncPlan.invocation)
            .let { plan -> builder.addStatement(plan.statementFormat, *plan.args) }
        return builder.build()
    }

    private fun renderAsyncAwaitMethod(method: WinMdMethod, currentNamespace: String): FunSpec? {
        val asyncPlan = asyncMethodRuleRegistry.plan(method, currentNamespace) ?: return null
        val baseFunctionName = if (method.name == "ToString" && method.returnType == "String" && method.parameters.isEmpty()) {
            "toString"
        } else {
            method.name.replaceFirstChar(Char::lowercase)
        }
        val builder = FunSpec.builder("${baseFunctionName}Await")
            .addModifiers(KModifier.SUSPEND)
            .returns(asyncPlan.awaitReturnType)
        val parameterNames = method.parameters.map { parameter ->
            val parameterName = parameter.name.replaceFirstChar(Char::lowercase)
            builder.addParameter(parameterName, typeNameMapper.mapTypeName(parameter.type, currentNamespace))
            parameterName
        }
        val invocation = buildString {
            append(baseFunctionName)
            append('(')
            append(parameterNames.joinToString(", "))
            append(')')
        }
        asyncPlan.progressLambdaType?.let { progressLambdaType ->
            builder.addParameter(
                com.squareup.kotlinpoet.ParameterSpec.builder("onProgress", progressLambdaType)
                    .defaultValue("{ _ -> }")
                    .build(),
            )
            builder.addStatement("return %L.%M(onProgress = onProgress)", invocation, PoetSymbols.awaitMember)
        } ?: builder.addStatement("return %L.%M()", invocation, PoetSymbols.awaitMember)
        return builder.build()
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
        if (signatureKey.isTwoArgumentUnifiedReturnShape()) {
            return plannedTwoArgumentRuntimeMethod(signatureKey)
        }
        plannedUnaryRuntimeMethod(signatureKey)?.let { return it }
        signatureKey.shape.toParameterCategories()
            ?.takeIf { signatureKey.returnKind == MethodReturnKind.UNIT && it.isSupportedTwoArgumentUnitCategories() }
            ?.let {
                return RuntimeMethodPlan(
                    nullPointerReturn = { PlannedStatement("return") },
                    returnStatement = "%L",
                    statementArgs = { method, _, parameterBindings ->
                        arrayOf(
                            AbiCallCatalog.unitMethodWithTwoArguments(
                                method.vtableIndex!!,
                                it,
                                when (it[0]) {
                                    MethodParameterCategory.OBJECT -> "${parameterBindings[0].name}.pointer"
                                    MethodParameterCategory.INT32,
                                    MethodParameterCategory.UINT32,
                                    MethodParameterCategory.BOOLEAN,
                                    MethodParameterCategory.INT64,
                                    MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> "${parameterBindings[0].name}.value"
                                    else -> parameterBindings[0].name
                                },
                                when (it[1]) {
                                    MethodParameterCategory.OBJECT -> "${parameterBindings[1].name}.pointer"
                                    MethodParameterCategory.INT32,
                                    MethodParameterCategory.UINT32,
                                    MethodParameterCategory.BOOLEAN,
                                    MethodParameterCategory.INT64,
                                    MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> "${parameterBindings[1].name}.value"
                                    else -> parameterBindings[1].name
                                },
                            ),
                        )
                    },
                )
            }
        return when (signatureKey) {
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
                    arrayOf(PoetSymbols.winRtBooleanClass, AbiCallCatalog.booleanMethodWithUInt32(method.vtableIndex!!, "${parameterBindings.single().name}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.INT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T.FALSE", arrayOf(PoetSymbols.winRtBooleanClass)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.winRtBooleanClass, AbiCallCatalog.booleanMethodWithInt32(method.vtableIndex!!, "${parameterBindings.single().name}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.BOOLEAN) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T.FALSE", arrayOf(PoetSymbols.winRtBooleanClass)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    val argumentName = parameterBindings.single().name
                    arrayOf(PoetSymbols.winRtBooleanClass, AbiCallCatalog.booleanMethodWithBoolean(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.INT64) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T.FALSE", arrayOf(PoetSymbols.winRtBooleanClass)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.winRtBooleanClass, AbiCallCatalog.booleanMethodWithInt64(method.vtableIndex!!, "${parameterBindings.single().name}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.OBJECT) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T.FALSE", arrayOf(PoetSymbols.winRtBooleanClass)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.winRtBooleanClass, AbiCallCatalog.booleanMethodWithObject(method.vtableIndex!!, "${parameterBindings.single().name}.pointer"))
                },
            )
            MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.STRING) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0)", arrayOf(PoetSymbols.int32Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.int32Class, AbiCallCatalog.int32MethodWithString(method.vtableIndex!!, parameterBindings.single().name))
                },
            )
            MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.INT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0)", arrayOf(PoetSymbols.int32Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.int32Class, AbiCallCatalog.int32MethodWithInt32(method.vtableIndex!!, "${parameterBindings.single().name}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.UINT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0)", arrayOf(PoetSymbols.int32Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.int32Class, AbiCallCatalog.int32MethodWithUInt32(method.vtableIndex!!, "${parameterBindings.single().name}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.BOOLEAN) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0)", arrayOf(PoetSymbols.int32Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    val argumentName = parameterBindings.single().name
                    arrayOf(PoetSymbols.int32Class, AbiCallCatalog.int32MethodWithBoolean(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.INT64) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0)", arrayOf(PoetSymbols.int32Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.int32Class, AbiCallCatalog.int32MethodWithInt64(method.vtableIndex!!, "${parameterBindings.single().name}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.INT32, MethodSignatureShape.OBJECT) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0)", arrayOf(PoetSymbols.int32Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(
                        PoetSymbols.int32Class,
                        AbiCallCatalog.int32MethodWithObject(method.vtableIndex!!, "${parameterBindings.single().name}.pointer"),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.STRING) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0L)", arrayOf(PoetSymbols.int64Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.int64Class, AbiCallCatalog.int64MethodWithString(method.vtableIndex!!, parameterBindings.single().name))
                },
            )
            MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.INT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0L)", arrayOf(PoetSymbols.int64Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.int64Class, AbiCallCatalog.int64MethodWithInt32(method.vtableIndex!!, "${parameterBindings.single().name}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.UINT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0L)", arrayOf(PoetSymbols.int64Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.int64Class, AbiCallCatalog.int64MethodWithUInt32(method.vtableIndex!!, "${parameterBindings.single().name}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.BOOLEAN) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0L)", arrayOf(PoetSymbols.int64Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    val argumentName = parameterBindings.single().name
                    arrayOf(PoetSymbols.int64Class, AbiCallCatalog.int64MethodWithBoolean(method.vtableIndex!!, "if (${argumentName}.value) 1L else 0L"))
                },
            )
            MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.OBJECT) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0L)", arrayOf(PoetSymbols.int64Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(
                        PoetSymbols.int64Class,
                        AbiCallCatalog.int64MethodWithObject(method.vtableIndex!!, "${parameterBindings.single().name}.pointer"),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.STRING) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0uL)", arrayOf(PoetSymbols.uint64Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.uint64Class, AbiCallCatalog.uint64MethodWithString(method.vtableIndex!!, parameterBindings.single().name))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.INT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0uL)", arrayOf(PoetSymbols.uint64Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.uint64Class, AbiCallCatalog.uint64MethodWithInt32(method.vtableIndex!!, "${parameterBindings.single().name}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.UINT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0uL)", arrayOf(PoetSymbols.uint64Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.uint64Class, AbiCallCatalog.uint64MethodWithUInt32(method.vtableIndex!!, "${parameterBindings.single().name}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.BOOLEAN) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0uL)", arrayOf(PoetSymbols.uint64Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    val argumentName = parameterBindings.single().name
                    arrayOf(PoetSymbols.uint64Class, AbiCallCatalog.uint64MethodWithBoolean(method.vtableIndex!!, "if (${argumentName}.value) 1L else 0L"))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.OBJECT) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0uL)", arrayOf(PoetSymbols.uint64Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(
                        PoetSymbols.uint64Class,
                        AbiCallCatalog.uint64MethodWithObject(method.vtableIndex!!, "${parameterBindings.single().name}.pointer"),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.STRING) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0u)", arrayOf(PoetSymbols.uint32Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.uint32Class, AbiCallCatalog.uint32MethodWithString(method.vtableIndex!!, parameterBindings.single().name))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.INT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0u)", arrayOf(PoetSymbols.uint32Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.uint32Class, AbiCallCatalog.uint32MethodWithInt32(method.vtableIndex!!, "${parameterBindings.single().name}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.UINT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0u)", arrayOf(PoetSymbols.uint32Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.uint32Class, AbiCallCatalog.uint32MethodWithUInt32(method.vtableIndex!!, "${parameterBindings.single().name}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.BOOLEAN) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0u)", arrayOf(PoetSymbols.uint32Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    val argumentName = parameterBindings.single().name
                    arrayOf(PoetSymbols.uint32Class, AbiCallCatalog.uint32MethodWithBoolean(method.vtableIndex!!, "${argumentName}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.INT64) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0u)", arrayOf(PoetSymbols.uint32Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.uint32Class, AbiCallCatalog.uint32MethodWithInt64(method.vtableIndex!!, "${parameterBindings.single().name}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT32, MethodSignatureShape.OBJECT) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0u)", arrayOf(PoetSymbols.uint32Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(
                        PoetSymbols.uint32Class,
                        AbiCallCatalog.uint32MethodWithObject(method.vtableIndex!!, "${parameterBindings.single().name}.pointer"),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.INT64, MethodSignatureShape.OBJECT) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0L)", arrayOf(PoetSymbols.int64Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(
                        PoetSymbols.int64Class,
                        AbiCallCatalog.int64MethodWithObject(method.vtableIndex!!, "${parameterBindings.single().name}.pointer"),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.STRING) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0uL)", arrayOf(PoetSymbols.uint64Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.uint64Class, AbiCallCatalog.uint64MethodWithString(method.vtableIndex!!, parameterBindings.single().name))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.INT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0uL)", arrayOf(PoetSymbols.uint64Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.uint64Class, AbiCallCatalog.uint64MethodWithInt32(method.vtableIndex!!, "${parameterBindings.single().name}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.UINT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0uL)", arrayOf(PoetSymbols.uint64Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.uint64Class, AbiCallCatalog.uint64MethodWithUInt32(method.vtableIndex!!, "${parameterBindings.single().name}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.BOOLEAN) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0uL)", arrayOf(PoetSymbols.uint64Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    val argumentName = parameterBindings.single().name
                    arrayOf(PoetSymbols.uint64Class, AbiCallCatalog.uint64MethodWithBoolean(method.vtableIndex!!, "if (${argumentName}.value) 1L else 0L"))
                },
            )
            MethodSignatureKey(MethodReturnKind.UINT64, MethodSignatureShape.OBJECT) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(0uL)", arrayOf(PoetSymbols.uint64Class)) },
                returnStatement = "return %T(%L)",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(
                        PoetSymbols.uint64Class,
                        AbiCallCatalog.uint64MethodWithObject(method.vtableIndex!!, "${parameterBindings.single().name}.pointer"),
                    )
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
            MethodSignatureKey(MethodReturnKind.GUID, MethodSignatureShape.STRING) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(%S)", arrayOf(PoetSymbols.guidValueClass, "")) },
                returnStatement = "return %T(%L.toString())",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.guidValueClass, AbiCallCatalog.guidMethodWithString(method.vtableIndex!!, parameterBindings.single().name))
                },
            )
            MethodSignatureKey(MethodReturnKind.GUID, MethodSignatureShape.INT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(%S)", arrayOf(PoetSymbols.guidValueClass, "")) },
                returnStatement = "return %T(%L.toString())",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.guidValueClass, AbiCallCatalog.guidMethodWithInt32(method.vtableIndex!!, "${parameterBindings.single().name}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.GUID, MethodSignatureShape.UINT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(%S)", arrayOf(PoetSymbols.guidValueClass, "")) },
                returnStatement = "return %T(%L.toString())",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.guidValueClass, AbiCallCatalog.guidMethodWithUInt32(method.vtableIndex!!, "${parameterBindings.single().name}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.GUID, MethodSignatureShape.BOOLEAN) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(%S)", arrayOf(PoetSymbols.guidValueClass, "")) },
                returnStatement = "return %T(%L.toString())",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.guidValueClass, AbiCallCatalog.guidMethodWithBoolean(method.vtableIndex!!, "${parameterBindings.single().name}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.GUID, MethodSignatureShape.INT64) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(%S)", arrayOf(PoetSymbols.guidValueClass, "")) },
                returnStatement = "return %T(%L.toString())",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.guidValueClass, AbiCallCatalog.guidMethodWithInt64(method.vtableIndex!!, "${parameterBindings.single().name}.value"))
                },
            )
            MethodSignatureKey(MethodReturnKind.GUID, MethodSignatureShape.OBJECT) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return %T(%S)", arrayOf(PoetSymbols.guidValueClass, "")) },
                returnStatement = "return %T(%L.toString())",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(PoetSymbols.guidValueClass, AbiCallCatalog.guidMethodWithObject(method.vtableIndex!!, "${parameterBindings.single().name}.pointer"))
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
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.UINT32) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return") },
                returnStatement = "%L",
                statementArgs = { method, _, parameterBindings ->
                    arrayOf(AbiCallCatalog.unitMethodWithUInt32(method.vtableIndex!!, parameterBindings.single().name))
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.BOOLEAN) -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return") },
                returnStatement = "%L",
                statementArgs = { method, _, parameterBindings ->
                    val argumentName = parameterBindings.single().name
                    arrayOf(AbiCallCatalog.unitMethodWithInt32Expression(method.vtableIndex!!, "if (${argumentName}.value) 1 else 0"))
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
            else -> when (signatureKey) {
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.EMPTY) -> RuntimeMethodPlan(
                nullPointerReturn = { method -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}")) },
                returnStatement = "return %L",
                statementArgs = { method, currentNamespace, _ ->
                    arrayOf(runtimeObjectReturnCode(method, currentNamespace, AbiCallCatalog.objectMethod(method.vtableIndex!!)))
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.STRING) -> RuntimeMethodPlan(
                nullPointerReturn = { method -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}")) },
                returnStatement = "return %L",
                statementArgs = { method, currentNamespace, parameterBindings ->
                    arrayOf(
                        runtimeObjectReturnCode(
                            method,
                            currentNamespace,
                            AbiCallCatalog.objectMethodWithString(method.vtableIndex!!, parameterBindings.single().name),
                        ),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.UINT32) -> RuntimeMethodPlan(
                nullPointerReturn = { method -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}")) },
                returnStatement = "return %L",
                statementArgs = { method, currentNamespace, parameterBindings ->
                    arrayOf(
                        runtimeObjectReturnCode(
                            method,
                            currentNamespace,
                            AbiCallCatalog.objectMethodWithUInt32(method.vtableIndex!!, "${parameterBindings.single().name}.value"),
                        ),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.INT32) -> RuntimeMethodPlan(
                nullPointerReturn = { method -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}")) },
                returnStatement = "return %L",
                statementArgs = { method, currentNamespace, parameterBindings ->
                    arrayOf(
                        runtimeObjectReturnCode(
                            method,
                            currentNamespace,
                            AbiCallCatalog.objectMethodWithInt32(method.vtableIndex!!, "${parameterBindings.single().name}.value"),
                        ),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.BOOLEAN) -> RuntimeMethodPlan(
                nullPointerReturn = { method -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}")) },
                returnStatement = "return %L",
                statementArgs = { method, currentNamespace, parameterBindings ->
                    val argumentName = parameterBindings.single().name
                    arrayOf(runtimeObjectReturnCode(
                        method,
                        currentNamespace,
                        AbiCallCatalog.objectMethodWithBoolean(method.vtableIndex!!, "${argumentName}.value"),
                    ))
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.INT64) -> RuntimeMethodPlan(
                nullPointerReturn = { method -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}")) },
                returnStatement = "return %L",
                statementArgs = { method, currentNamespace, parameterBindings ->
                    arrayOf(runtimeObjectReturnCode(
                        method,
                        currentNamespace,
                        AbiCallCatalog.objectMethodWithInt64(method.vtableIndex!!, "${parameterBindings.single().name}.value"),
                    ))
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.OBJECT) -> RuntimeMethodPlan(
                nullPointerReturn = { method -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}")) },
                returnStatement = "return %L",
                statementArgs = { method, currentNamespace, parameterBindings ->
                    arrayOf(
                        runtimeObjectReturnCode(
                            method,
                            currentNamespace,
                            AbiCallCatalog.objectMethodWithObject(method.vtableIndex!!, "${parameterBindings.single().name}.pointer"),
                        ),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.OBJECT_STRING) -> RuntimeMethodPlan(
                nullPointerReturn = { method -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}")) },
                returnStatement = "return %L",
                statementArgs = { method, currentNamespace, parameterBindings ->
                    arrayOf(
                        runtimeObjectReturnCode(
                            method,
                            currentNamespace,
                            AbiCallCatalog.objectMethodWithObjectAndString(
                                method.vtableIndex!!,
                                "${parameterBindings[0].name}.pointer",
                                parameterBindings[1].name,
                            ),
                        ),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.STRING_OBJECT) -> RuntimeMethodPlan(
                nullPointerReturn = { method -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}")) },
                returnStatement = "return %L",
                statementArgs = { method, currentNamespace, parameterBindings ->
                    arrayOf(
                        runtimeObjectReturnCode(
                            method,
                            currentNamespace,
                            AbiCallCatalog.objectMethodWithStringAndObject(
                                method.vtableIndex!!,
                                parameterBindings[0].name,
                                "${parameterBindings[1].name}.pointer",
                            ),
                        ),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.TWO_OBJECT) -> RuntimeMethodPlan(
                nullPointerReturn = { method -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}")) },
                returnStatement = "return %L",
                statementArgs = { method, currentNamespace, parameterBindings ->
                    arrayOf(
                        runtimeObjectReturnCode(
                            method,
                            currentNamespace,
                            AbiCallCatalog.resultMethodWithTwoObject(
                                method.vtableIndex!!,
                                "OBJECT",
                                PoetSymbols.requireObjectMember,
                                "${parameterBindings[0].name}.pointer",
                                "${parameterBindings[1].name}.pointer",
                            ),
                        ),
                    )
                },
            )
            else -> null
        }
        }
    }

    private fun plannedUnaryRuntimeMethod(signatureKey: MethodSignatureKey): RuntimeMethodPlan? {
        val parameterCategories = signatureKey.shape.toParameterCategories() ?: return null
        if (parameterCategories.size > 1) return null
        val parameterCategory = parameterCategories.singleOrNull()
        return when (signatureKey.returnKind) {
            MethodReturnKind.STRING -> plannedUnaryHStringRuntimeMethod(parameterCategory)
            MethodReturnKind.FLOAT32 -> plannedUnaryWrappedRuntimeMethod(
                parameterCategory = parameterCategory,
                nullPointerReturn = PlannedStatement("return %T(0f)", arrayOf(PoetSymbols.float32Class)),
                wrapperType = PoetSymbols.float32Class,
                emptyCall = AbiCallCatalog::float32Method,
                unaryCall = ::float32UnaryRuntimeCall,
            )
            MethodReturnKind.FLOAT64 -> plannedUnaryWrappedRuntimeMethod(
                parameterCategory = parameterCategory,
                nullPointerReturn = PlannedStatement("return %T(0.0)", arrayOf(PoetSymbols.float64Class)),
                wrapperType = PoetSymbols.float64Class,
                emptyCall = AbiCallCatalog::float64Method,
                unaryCall = ::float64UnaryRuntimeCall,
            )
            MethodReturnKind.OBJECT -> plannedUnaryObjectRuntimeMethod(parameterCategory)
            MethodReturnKind.UNIT -> plannedUnaryUnitRuntimeMethod(parameterCategory)
            else -> null
        }
    }

    private fun plannedUnaryHStringRuntimeMethod(parameterCategory: MethodParameterCategory?): RuntimeMethodPlan =
        RuntimeMethodPlan(
            nullPointerReturn = { PlannedStatement("return %S", arrayOf("")) },
            returnStatement = "return %L",
            statementArgs = { method, _, parameterBindings ->
                val abiCall = if (parameterCategory == null) {
                    AbiCallCatalog.hstringMethod(method.vtableIndex!!)
                } else {
                    hstringUnaryRuntimeCall(method.vtableIndex!!, parameterCategory, parameterBindings.single().name)
                }
                arrayOf(HStringSupport.fromCall(abiCall))
            },
        )

    private fun plannedUnaryWrappedRuntimeMethod(
        parameterCategory: MethodParameterCategory?,
        nullPointerReturn: PlannedStatement,
        wrapperType: Any,
        emptyCall: (Int) -> CodeBlock,
        unaryCall: (Int, MethodParameterCategory, String) -> CodeBlock,
    ): RuntimeMethodPlan =
        RuntimeMethodPlan(
            nullPointerReturn = { nullPointerReturn },
            returnStatement = "return %T(%L)",
            statementArgs = { method, _, parameterBindings ->
                val abiCall = if (parameterCategory == null) {
                    emptyCall(method.vtableIndex!!)
                } else {
                    unaryCall(method.vtableIndex!!, parameterCategory, parameterBindings.single().name)
                }
                arrayOf(wrapperType, abiCall)
            },
        )

    private fun plannedUnaryObjectRuntimeMethod(parameterCategory: MethodParameterCategory?): RuntimeMethodPlan =
        RuntimeMethodPlan(
            nullPointerReturn = { method -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}")) },
            returnStatement = "return %L",
            statementArgs = { method, currentNamespace, parameterBindings ->
                val abiCall = if (parameterCategory == null) {
                    AbiCallCatalog.objectMethod(method.vtableIndex!!)
                } else {
                    objectUnaryRuntimeCall(method.vtableIndex!!, parameterCategory, parameterBindings.single().name)
                }
                arrayOf(runtimeObjectReturnCode(method, currentNamespace, abiCall))
            },
        )

    private fun plannedUnaryUnitRuntimeMethod(parameterCategory: MethodParameterCategory?): RuntimeMethodPlan =
        RuntimeMethodPlan(
            nullPointerReturn = { PlannedStatement("return") },
            returnStatement = "%L",
            statementArgs = { method, _, parameterBindings ->
                val abiCall = if (parameterCategory == null) {
                    AbiCallCatalog.unitMethod(method.vtableIndex!!)
                } else {
                    unitUnaryRuntimeCall(method.vtableIndex!!, parameterCategory, parameterBindings.single().name)
                }
                arrayOf(abiCall)
            },
        )

    private fun hstringUnaryRuntimeCall(vtableIndex: Int, category: MethodParameterCategory, argumentName: String): CodeBlock = when (category) {
        MethodParameterCategory.STRING -> AbiCallCatalog.hstringMethodWithString(vtableIndex, argumentName)
        MethodParameterCategory.INT32 -> AbiCallCatalog.hstringMethodWithInt32(vtableIndex, runtimeUnaryArgumentExpression(argumentName, category))
        MethodParameterCategory.UINT32 -> AbiCallCatalog.hstringMethodWithUInt32(vtableIndex, runtimeUnaryArgumentExpression(argumentName, category))
        MethodParameterCategory.BOOLEAN -> AbiCallCatalog.hstringMethodWithUInt32(vtableIndex, "if (${runtimeUnaryArgumentExpression(argumentName, category)}) 1u else 0u")
        MethodParameterCategory.INT64,
        MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> AbiCallCatalog.hstringMethodWithInt64(vtableIndex, runtimeUnaryArgumentExpression(argumentName, category))
        MethodParameterCategory.OBJECT -> AbiCallCatalog.hstringMethodWithObject(vtableIndex, runtimeUnaryArgumentExpression(argumentName, category))
    }

    private fun float32UnaryRuntimeCall(vtableIndex: Int, category: MethodParameterCategory, argumentName: String): CodeBlock = when (category) {
        MethodParameterCategory.STRING -> AbiCallCatalog.float32MethodWithString(vtableIndex, argumentName)
        MethodParameterCategory.INT32 -> AbiCallCatalog.float32MethodWithInt32(vtableIndex, runtimeUnaryArgumentExpression(argumentName, category))
        MethodParameterCategory.UINT32 -> AbiCallCatalog.float32MethodWithUInt32(vtableIndex, runtimeUnaryArgumentExpression(argumentName, category))
        MethodParameterCategory.BOOLEAN -> AbiCallCatalog.float32MethodWithBoolean(vtableIndex, runtimeUnaryArgumentExpression(argumentName, category))
        MethodParameterCategory.INT64,
        MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> AbiCallCatalog.float32MethodWithInt64(vtableIndex, runtimeUnaryArgumentExpression(argumentName, category))
        MethodParameterCategory.OBJECT -> AbiCallCatalog.float32MethodWithObject(vtableIndex, runtimeUnaryArgumentExpression(argumentName, category))
    }

    private fun float64UnaryRuntimeCall(vtableIndex: Int, category: MethodParameterCategory, argumentName: String): CodeBlock = when (category) {
        MethodParameterCategory.STRING -> AbiCallCatalog.float64MethodWithString(vtableIndex, argumentName)
        MethodParameterCategory.INT32 -> AbiCallCatalog.float64MethodWithInt32(vtableIndex, runtimeUnaryArgumentExpression(argumentName, category))
        MethodParameterCategory.UINT32 -> AbiCallCatalog.float64MethodWithUInt32(vtableIndex, runtimeUnaryArgumentExpression(argumentName, category))
        MethodParameterCategory.BOOLEAN -> AbiCallCatalog.float64MethodWithBoolean(vtableIndex, runtimeUnaryArgumentExpression(argumentName, category))
        MethodParameterCategory.INT64,
        MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> AbiCallCatalog.float64MethodWithInt64(vtableIndex, runtimeUnaryArgumentExpression(argumentName, category))
        MethodParameterCategory.OBJECT -> AbiCallCatalog.float64MethodWithObject(vtableIndex, runtimeUnaryArgumentExpression(argumentName, category))
    }

    private fun objectUnaryRuntimeCall(vtableIndex: Int, category: MethodParameterCategory, argumentName: String): CodeBlock = when (category) {
        MethodParameterCategory.STRING -> AbiCallCatalog.objectMethodWithString(vtableIndex, argumentName)
        MethodParameterCategory.INT32 -> AbiCallCatalog.objectMethodWithInt32(vtableIndex, runtimeUnaryArgumentExpression(argumentName, category))
        MethodParameterCategory.UINT32 -> AbiCallCatalog.objectMethodWithUInt32(vtableIndex, runtimeUnaryArgumentExpression(argumentName, category))
        MethodParameterCategory.BOOLEAN -> AbiCallCatalog.objectMethodWithBoolean(vtableIndex, runtimeUnaryArgumentExpression(argumentName, category))
        MethodParameterCategory.INT64,
        MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> AbiCallCatalog.objectMethodWithInt64(vtableIndex, runtimeUnaryArgumentExpression(argumentName, category))
        MethodParameterCategory.OBJECT -> AbiCallCatalog.objectMethodWithObject(vtableIndex, runtimeUnaryArgumentExpression(argumentName, category))
    }

    private fun unitUnaryRuntimeCall(vtableIndex: Int, category: MethodParameterCategory, argumentName: String): CodeBlock = when (category) {
        MethodParameterCategory.INT32 -> AbiCallCatalog.unitMethodWithInt32(vtableIndex, argumentName)
        MethodParameterCategory.UINT32 -> AbiCallCatalog.unitMethodWithUInt32(vtableIndex, argumentName)
        MethodParameterCategory.BOOLEAN -> AbiCallCatalog.unitMethodWithInt32Expression(vtableIndex, "if (${runtimeUnaryArgumentExpression(argumentName, category)}) 1 else 0")
        MethodParameterCategory.INT64,
        MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> AbiCallCatalog.unitMethodWithInt64(vtableIndex, argumentName)
        MethodParameterCategory.STRING -> AbiCallCatalog.unitMethodWithString(vtableIndex, argumentName)
        MethodParameterCategory.OBJECT -> AbiCallCatalog.objectSetter(vtableIndex, argumentName)
    }

    private fun runtimeUnaryArgumentExpression(argumentName: String, category: MethodParameterCategory): String = when (category) {
        MethodParameterCategory.OBJECT -> "$argumentName.pointer"
        MethodParameterCategory.INT32,
        MethodParameterCategory.UINT32,
        MethodParameterCategory.BOOLEAN,
        MethodParameterCategory.INT64,
        MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> "$argumentName.value"
        MethodParameterCategory.STRING -> argumentName
    }

    private fun plannedTwoArgumentRuntimeMethod(signatureKey: MethodSignatureKey): RuntimeMethodPlan = RuntimeMethodPlan(
        nullPointerReturn = { method ->
            if (signatureKey.returnKind == MethodReturnKind.OBJECT) {
                PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}"))
            } else {
                twoArgumentNullReturn(method.returnType, method.name)
            }
        },
        returnStatement = "return %L",
        statementArgs = { method, currentNamespace, parameterBindings ->
            val parameterCategories = methodParameterCategories(
                method.parameters.map { parameter -> parameter.type },
                ::supportsRuntimeObjectType,
            ) ?: error("Unsupported two-argument return shape: ${signatureKey.shape}")
            val abiCall = AbiCallCatalog.resultMethodWithTwoArguments(
                method.vtableIndex!!,
                if (signatureKey.returnKind == MethodReturnKind.OBJECT) "OBJECT" else resultKindName(method.returnType),
                if (signatureKey.returnKind == MethodReturnKind.OBJECT) PoetSymbols.requireObjectMember else resultExtractor(method.returnType),
                parameterCategories,
                when (parameterCategories[0]) {
                    MethodParameterCategory.OBJECT -> "${parameterBindings[0].name}.pointer"
                    MethodParameterCategory.INT32,
                    MethodParameterCategory.UINT32,
                    MethodParameterCategory.BOOLEAN,
                    MethodParameterCategory.INT64,
                    MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> "${parameterBindings[0].name}.value"
                    else -> parameterBindings[0].name
                },
                when (parameterCategories[1]) {
                    MethodParameterCategory.OBJECT -> "${parameterBindings[1].name}.pointer"
                    MethodParameterCategory.INT32,
                    MethodParameterCategory.UINT32,
                    MethodParameterCategory.BOOLEAN,
                    MethodParameterCategory.INT64,
                    MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> "${parameterBindings[1].name}.value"
                    else -> parameterBindings[1].name
                },
            )
            arrayOf(
                if (signatureKey.returnKind == MethodReturnKind.OBJECT) {
                    runtimeObjectReturnCode(method, currentNamespace, abiCall)
                } else {
                    twoArgumentReturnCode(method.returnType, abiCall)
                },
            )
        },
    )

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
                beginControlFlow("try")
                addStatement("%N(%T(delegateHandle.pointer))", functionName, delegateClass)
                nextControlFlow("catch (t: Throwable)")
                addStatement("delegateHandle.close()")
                addStatement("throw t")
                endControlFlow()
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

    private fun runtimeObjectReturnCode(method: WinMdMethod, currentNamespace: String, abiCall: CodeBlock): CodeBlock {
        val mappedType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
        return if (typeRegistry.isRuntimeProjectedInterface(method.returnType, currentNamespace)) {
            CodeBlock.of("%T.from(%T(%L))", mappedType, PoetSymbols.inspectableClass, abiCall)
        } else {
            CodeBlock.of("%T(%L)", mappedType, abiCall)
        }
    }

    private fun twoArgumentReturnCode(returnType: String, abiCall: CodeBlock): CodeBlock {
        return when (returnType) {
            "String" -> HStringSupport.fromCall(abiCall)
            "Float32" -> CodeBlock.of("%T(%L)", PoetSymbols.float32Class, abiCall)
            "Float64" -> CodeBlock.of("%T(%L)", PoetSymbols.float64Class, abiCall)
            "Boolean" -> CodeBlock.of("%T(%L)", PoetSymbols.winRtBooleanClass, abiCall)
            "Int32" -> CodeBlock.of("%T(%L)", PoetSymbols.int32Class, abiCall)
            "UInt32" -> CodeBlock.of("%T(%L)", PoetSymbols.uint32Class, abiCall)
            "Int64" -> CodeBlock.of("%T(%L)", PoetSymbols.int64Class, abiCall)
            "UInt64" -> CodeBlock.of("%T(%L)", PoetSymbols.uint64Class, abiCall)
            "Guid" -> CodeBlock.of("%T(%L.toString())", PoetSymbols.guidValueClass, abiCall)
            else -> error("Unsupported two-argument return type: $returnType")
        }
    }

    private fun twoArgumentNullReturn(returnType: String, methodName: String): PlannedStatement {
        return when (returnType) {
            "String" -> PlannedStatement("return %S", arrayOf(""))
            "Float32" -> PlannedStatement("return %T(0f)", arrayOf(PoetSymbols.float32Class))
            "Float64" -> PlannedStatement("return %T(0.0)", arrayOf(PoetSymbols.float64Class))
            "Boolean" -> PlannedStatement("return %T(false)", arrayOf(PoetSymbols.winRtBooleanClass))
            "Int32" -> PlannedStatement("return %T(0)", arrayOf(PoetSymbols.int32Class))
            "UInt32" -> PlannedStatement("return %T(0u)", arrayOf(PoetSymbols.uint32Class))
            "Int64" -> PlannedStatement("return %T(0L)", arrayOf(PoetSymbols.int64Class))
            "UInt64" -> PlannedStatement("return %T(0uL)", arrayOf(PoetSymbols.uint64Class))
            "Guid" -> PlannedStatement("return %T(%S)", arrayOf(PoetSymbols.guidValueClass, ""))
            else -> error("Unsupported two-argument null return type for $methodName: $returnType")
        }
    }

    private fun resultKindName(returnType: String): String {
        return when (returnType) {
            "String" -> "HSTRING"
            "Float32" -> "FLOAT32"
            "Float64" -> "FLOAT64"
            "Boolean" -> "BOOLEAN"
            "Int32" -> "INT32"
            "UInt32" -> "UINT32"
            "Int64" -> "INT64"
            "UInt64" -> "UINT64"
            "Guid" -> "GUID"
            else -> error("Unsupported result kind for two-argument return type: $returnType")
        }
    }

    private fun resultExtractor(returnType: String): Any {
        return when (returnType) {
            "String" -> PoetSymbols.requireHStringMember
            "Float32" -> PoetSymbols.requireFloat32Member
            "Float64" -> PoetSymbols.requireFloat64Member
            "Boolean" -> PoetSymbols.requireBooleanMember
            "Int32" -> PoetSymbols.requireInt32Member
            "UInt32" -> PoetSymbols.requireUInt32Member
            "Int64" -> PoetSymbols.requireInt64Member
            "UInt64" -> PoetSymbols.requireUInt64Member
            "Guid" -> PoetSymbols.requireGuidMember
            else -> error("Unsupported result extractor for two-argument return type: $returnType")
        }
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
