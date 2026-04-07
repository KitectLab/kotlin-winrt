package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter
import dev.winrt.winmd.plugin.WinMdTypeKind

internal class RuntimeMethodRenderer(
    private val typeNameMapper: TypeNameMapper,
    private val delegateLambdaPlanResolver: DelegateLambdaPlanResolver,
    private val typeRegistry: TypeRegistry,
    private val asyncMethodRuleRegistry: AsyncMethodRuleRegistry,
    private val projectedObjectArgumentLowering: ProjectedObjectArgumentLowering,
    private val winRtSignatureMapper: WinRtSignatureMapper = WinRtSignatureMapper(typeRegistry),
    private val winRtProjectionTypeMapper: WinRtProjectionTypeMapper = WinRtProjectionTypeMapper(),
    private val valueTypeProjectionSupport: ValueTypeProjectionSupport = ValueTypeProjectionSupport(typeNameMapper, typeRegistry),
) {
    constructor(
        typeNameMapper: TypeNameMapper,
        delegateLambdaPlanResolver: DelegateLambdaPlanResolver,
        typeRegistry: TypeRegistry,
        asyncMethodRuleRegistry: AsyncMethodRuleRegistry,
    ) : this(
        typeNameMapper,
        delegateLambdaPlanResolver,
        typeRegistry,
        asyncMethodRuleRegistry,
        ProjectedObjectArgumentLowering(typeRegistry, WinRtSignatureMapper(typeRegistry), WinRtProjectionTypeMapper()),
    )

    fun renderRuntimeMethods(method: WinMdMethod, currentNamespace: String): List<FunSpec> {
        return listOfNotNull(
            renderRuntimeMethod(method, currentNamespace),
            renderAsyncAwaitMethod(method, currentNamespace),
        )
    }

    fun canRenderRuntimeMethod(method: WinMdMethod, currentNamespace: String): Boolean {
        return supportsProjectedIndexOfRuntimeMethod(method, currentNamespace) ||
            asyncMethodRuleRegistry.plan(method, currentNamespace) != null ||
            (typeRegistry.isEnumType(method.returnType, currentNamespace) &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null) ||
            runtimeMethodPlan(method, currentNamespace) != null
    }

    fun renderRuntimeMethod(method: WinMdMethod, currentNamespace: String): FunSpec? {
        renderAsyncTaskMethod(method, currentNamespace)?.let { return it }
        renderProjectedIndexOfRuntimeMethod(method, currentNamespace)?.let { return it }
        if (typeRegistry.isEnumType(method.returnType, currentNamespace) && method.parameters.isEmpty() && method.vtableIndex != null) {
            val functionName = projectedMethodName(method)
            val returnType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
            val underlyingType = enumUnderlyingTypeOrDefault(typeRegistry, method.returnType, currentNamespace)
            return FunSpec.builder(functionName)
                .returns(returnType)
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("error(%S)", "Null runtime object pointer: ${method.name}")
                .endControlFlow()
                .addStatement(
                    "return %T.fromValue(%L)",
                    returnType,
                    enumGetterAbiCall(underlyingType, method.vtableIndex!!),
                )
                .build()
        }
        val plan = runtimeMethodPlan(method, currentNamespace) ?: return null
        val functionName = projectedMethodName(method)
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
        val functionName = projectedMethodName(method)
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
        val baseFunctionName = projectedMethodName(method)
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

    private fun supportsProjectedIndexOfRuntimeMethod(
        method: WinMdMethod,
        currentNamespace: String,
    ): Boolean {
        if (!method.isIndexOfOutUInt32Method() || method.vtableIndex == null) {
            return false
        }
        val valueParameter = method.parameters.first()
        val valueBinding = RuntimeMethodParameterBinding(
            type = valueParameter.type,
            name = valueParameter.name.replaceFirstChar(Char::lowercase),
        )
        return lowerRuntimeArrayMethodArgument(
            method = method,
            parameter = valueParameter,
            parameterBindings = listOf(valueBinding),
            currentNamespace = currentNamespace,
        ) != null
    }

    private fun renderProjectedIndexOfRuntimeMethod(
        method: WinMdMethod,
        currentNamespace: String,
    ): FunSpec? {
        if (!supportsProjectedIndexOfRuntimeMethod(method, currentNamespace)) {
            return null
        }
        val valueParameter = method.parameters.first()
        val valueBinding = RuntimeMethodParameterBinding(
            type = valueParameter.type,
            name = valueParameter.name.replaceFirstChar(Char::lowercase),
        )
        val abiArgument = lowerRuntimeArrayMethodArgument(
            method = method,
            parameter = valueParameter,
            parameterBindings = listOf(valueBinding),
            currentNamespace = currentNamespace,
        ) ?: return null
        return FunSpec.builder(projectedMethodName(method))
            .returns(PoetSymbols.uint32Class.copy(nullable = true))
            .addParameter(
                valueBinding.name,
                typeNameMapper.mapTypeName(valueParameter.type, currentNamespace),
            )
            .beginControlFlow("if (pointer.isNull)")
            .addStatement("return null")
            .endControlFlow()
            .addStatement(
                "val (found, index) = %T.invokeIndexOfMethod(pointer, %L, %L).getOrThrow()",
                PoetSymbols.platformComInteropClass,
                method.vtableIndex!!,
                abiArgument,
            )
            .addStatement("return if (found) %T(index) else null", PoetSymbols.uint32Class)
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

    private fun runtimeMethodPlan(method: WinMdMethod, currentNamespace: String): RuntimeMethodPlan? {
        if (!isKotlinIdentifier(method.name) || method.vtableIndex == null) {
            return null
        }
        if (method.isIndexOfOutUInt32Method()) {
            return null
        }
        plannedInt32FillArrayRuntimeMethod(method, currentNamespace)?.let { return it }
        plannedStandardReceiveArrayRuntimeMethod(method)?.let { return it }
        plannedStructReceiveArrayRuntimeMethod(method, currentNamespace)?.let { return it }
        plannedRuntimeClassReceiveArrayRuntimeMethod(method, currentNamespace)?.let { return it }
        plannedStructPassArrayRuntimeMethod(method, currentNamespace)?.let { return it }
        plannedStandardPassArrayRuntimeMethod(method, currentNamespace)?.let { return it }
        valueAwareRuntimeMethodPlan(method, currentNamespace)?.let { return it }
        val parameterTypes = method.parameters.map { it.type }
        val signatureKey = methodSignatureKey(
            returnType = method.returnType,
            parameterTypes = parameterTypes.map { typeRegistry.signatureParameterType(it, currentNamespace) },
            supportsParameterObjectType = { type -> supportsRuntimeObjectType(type, currentNamespace) },
        ) ?: return null
        MethodRuleRegistry.sharedMethodPlan(signatureKey) ?: return null
        return runtimeMethodPlanForKey(signatureKey)
    }

    private fun plannedInt32FillArrayRuntimeMethod(
        method: WinMdMethod,
        currentNamespace: String,
    ): RuntimeMethodPlan? {
        if (!method.isInt32FillArrayMethod()) {
            return null
        }
        val fillArrayParameter = method.int32FillArrayParameter() ?: return null
        return when {
            method.returnType == "Unit" -> RuntimeMethodPlan(
                nullPointerReturn = { PlannedStatement("return") },
                returnStatement = "%L",
                statementArgs = { method, currentNamespace, parameterBindings ->
                    val abiArguments = runtimeInt32FillArrayAbiArguments(method, currentNamespace, parameterBindings, fillArrayParameter)
                        ?: error("Unsupported Int32 fill-array runtime method: ${method.name}")
                    arrayOf(
                        int32FillArrayWrappedCall(
                            fillArrayParameter,
                            runtimeVarargAbiCall("invokeUnitMethodWithArgs", method.vtableIndex!!, abiArguments),
                            returnsValue = false,
                        ),
                    )
                },
            )
            supportsRuntimeObjectReturnType(method.returnType, currentNamespace) -> RuntimeMethodPlan(
                nullPointerReturn = { method ->
                    PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}"))
                },
                returnStatement = "return %L",
                statementArgs = { method, currentNamespace, parameterBindings ->
                    val abiArguments = runtimeInt32FillArrayAbiArguments(method, currentNamespace, parameterBindings, fillArrayParameter)
                        ?: error("Unsupported Int32 fill-array runtime method: ${method.name}")
                    arrayOf(
                        int32FillArrayWrappedCall(
                            fillArrayParameter,
                            runtimeObjectReturnCode(
                                method = method,
                                currentNamespace = currentNamespace,
                                abiCall = runtimeVarargAbiCall("invokeObjectMethodWithArgs", method.vtableIndex!!, abiArguments),
                            ),
                            returnsValue = true,
                        ),
                    )
                },
            )
            supportsFillArrayResultKind(method.returnType) -> RuntimeMethodPlan(
                nullPointerReturn = { method -> twoArgumentNullReturn(method.returnType, method.name) },
                returnStatement = "return %L",
                statementArgs = { method, currentNamespace, parameterBindings ->
                    val abiArguments = runtimeInt32FillArrayAbiArguments(method, currentNamespace, parameterBindings, fillArrayParameter)
                        ?: error("Unsupported Int32 fill-array runtime method: ${method.name}")
                    arrayOf(
                        int32FillArrayWrappedCall(
                            fillArrayParameter,
                            twoArgumentReturnCode(
                                method.returnType,
                                runtimeVarargResultKindAbiCall(
                                    vtableIndex = method.vtableIndex!!,
                                    returnType = method.returnType,
                                    abiArguments = abiArguments,
                                ),
                            ),
                            returnsValue = true,
                        ),
                    )
                },
            )
            else -> null
        }
    }

    private fun plannedStandardReceiveArrayRuntimeMethod(method: WinMdMethod): RuntimeMethodPlan? =
        standardReceiveArrayMethodDescriptors.firstNotNullOfOrNull { descriptor ->
            plannedReceiveArrayRuntimeMethod(method, descriptor)
        }

    private fun plannedReceiveArrayRuntimeMethod(
        method: WinMdMethod,
        descriptor: ReceiveArrayMethodDescriptor,
    ): RuntimeMethodPlan? {
        if (!descriptor.matches(method)) {
            return null
        }
        return RuntimeMethodPlan(
            nullPointerReturn = { failingMethod ->
                PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${failingMethod.name}"))
            },
            returnStatement = "return %L",
            statementArgs = { currentMethod, currentNamespace, parameterBindings ->
                val abiArguments = descriptor.abiArguments(currentMethod.parameters) { parameter ->
                    lowerRuntimeArrayMethodArgument(currentMethod, parameter, parameterBindings, currentNamespace)
                } ?: error("Unsupported ${descriptor.label} receive-array runtime method: ${currentMethod.name}")
                arrayOf(descriptor.returnExpression(currentMethod.vtableIndex!!, abiArguments))
            },
        )
    }

    private fun plannedStandardPassArrayRuntimeMethod(
        method: WinMdMethod,
        currentNamespace: String,
    ): RuntimeMethodPlan? =
        standardPassArrayMethodDescriptors.firstNotNullOfOrNull { descriptor ->
            plannedPassArrayRuntimeMethod(method, currentNamespace, descriptor)
        }

    private fun plannedPassArrayRuntimeMethod(
        method: WinMdMethod,
        currentNamespace: String,
        descriptor: PassArrayMethodDescriptor,
    ): RuntimeMethodPlan? {
        if (!descriptor.matches(method) { typeName -> supportsRuntimeObjectReturnType(typeName, currentNamespace) }) {
            return null
        }
        return RuntimeMethodPlan(
            nullPointerReturn = { failingMethod ->
                if (failingMethod.returnType == "Unit") PlannedStatement("return")
                else PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${failingMethod.name}"))
            },
            returnStatement = if (method.returnType == "Unit") "%L" else "return %L",
            statementArgs = { currentMethod, namespace, parameterBindings ->
                val abiArguments = descriptor.abiArguments(currentMethod.parameters) { parameter ->
                    lowerRuntimeArrayMethodArgument(currentMethod, parameter, parameterBindings, namespace)
                } ?: error("Unsupported ${descriptor.label} pass-array runtime method: ${currentMethod.name}")
                arrayOf(
                    if (currentMethod.returnType == "Unit") {
                        runtimeVarargAbiCall("invokeUnitMethodWithArgs", currentMethod.vtableIndex!!, abiArguments)
                    } else {
                        runtimeObjectReturnCode(
                            method = currentMethod,
                            currentNamespace = namespace,
                            abiCall = runtimeVarargAbiCall("invokeObjectMethodWithArgs", currentMethod.vtableIndex!!, abiArguments),
                        )
                    },
                )
            },
        )
    }

    private fun plannedRuntimeClassReceiveArrayRuntimeMethod(
        method: WinMdMethod,
        currentNamespace: String,
    ): RuntimeMethodPlan? {
        val elementType = method.runtimeClassReceiveArrayElementType(currentNamespace, typeRegistry) ?: return null
        return RuntimeMethodPlan(
            nullPointerReturn = { method -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}")) },
            returnStatement = "return %L",
            statementArgs = { method, namespace, parameterBindings ->
                val abiArguments = runtimeClassReceiveArrayAbiArguments(
                    parameters = method.parameters,
                    currentNamespace = namespace,
                    typeRegistry = typeRegistry,
                    expectedElementType = elementType,
                ) { parameter ->
                    lowerRuntimeArrayMethodArgument(method, parameter, parameterBindings, namespace)
                        ?: return@runtimeClassReceiveArrayAbiArguments null
                } ?: error("Unsupported runtime-class receive-array runtime method: ${method.name}")
                val runtimeClassType = typeNameMapper.mapTypeName(elementType, namespace)
                arrayOf(runtimeClassReceiveArrayReturnExpression(method.vtableIndex!!, runtimeClassType, abiArguments))
            },
        )
    }

    private fun plannedStructReceiveArrayRuntimeMethod(
        method: WinMdMethod,
        currentNamespace: String,
    ): RuntimeMethodPlan? {
        val elementType = method.supportedStructReceiveArrayElementType(currentNamespace, typeRegistry) ?: return null
        return RuntimeMethodPlan(
            nullPointerReturn = { method -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}")) },
            returnStatement = "return %L",
            statementArgs = { method, namespace, parameterBindings ->
                val abiArguments = structReceiveArrayAbiArguments(
                    parameters = method.parameters,
                    currentNamespace = namespace,
                    typeRegistry = typeRegistry,
                    expectedElementType = elementType,
                ) { parameter ->
                    lowerRuntimeArrayMethodArgument(method, parameter, parameterBindings, namespace)
                        ?: return@structReceiveArrayAbiArguments null
                } ?: error("Unsupported struct receive-array runtime method: ${method.name}")
                val structType = typeNameMapper.mapTypeName(elementType, namespace)
                arrayOf(structReceiveArrayReturnExpression(method.vtableIndex!!, structType, abiArguments))
            },
        )
    }

    private fun plannedStructPassArrayRuntimeMethod(
        method: WinMdMethod,
        currentNamespace: String,
    ): RuntimeMethodPlan? {
        val elementType = method.supportedStructPassArrayElementType(currentNamespace, typeRegistry) ?: return null
        if (method.returnType != "Unit" && !supportsRuntimeObjectReturnType(method.returnType, currentNamespace)) {
            return null
        }
        return RuntimeMethodPlan(
            nullPointerReturn = { method ->
                if (method.returnType == "Unit") PlannedStatement("return")
                else PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: ${method.name}"))
            },
            returnStatement = if (method.returnType == "Unit") "%L" else "return %L",
            statementArgs = { method, namespace, parameterBindings ->
                val abiArguments = structPassArrayAbiArguments(
                    parameters = method.parameters,
                    currentNamespace = namespace,
                    typeRegistry = typeRegistry,
                    expectedElementType = elementType,
                ) { parameter ->
                    lowerRuntimeArrayMethodArgument(method, parameter, parameterBindings, namespace)
                        ?: return@structPassArrayAbiArguments null
                } ?: error("Unsupported struct pass-array runtime method: ${method.name}")
                arrayOf(
                    if (method.returnType == "Unit") runtimeVarargAbiCall("invokeUnitMethodWithArgs", method.vtableIndex!!, abiArguments)
                    else runtimeObjectReturnCode(method = method, currentNamespace = namespace, abiCall = runtimeVarargAbiCall("invokeObjectMethodWithArgs", method.vtableIndex!!, abiArguments)),
                )
            },
        )
    }

    private fun valueAwareRuntimeMethodPlan(method: WinMdMethod, currentNamespace: String): RuntimeMethodPlan? {
        val argumentExpressions = method.parameters.map { parameter ->
            valueTypeProjectionSupport.lowerGenericAbiArgument(
                type = parameter.type,
                currentNamespace = currentNamespace,
                argumentName = parameter.name.replaceFirstChar(Char::lowercase),
                supportsObjectType = { typeName -> supportsRuntimeObjectType(typeName, currentNamespace) },
                lowerObjectArgument = { argumentName, typeName ->
                    projectedObjectArgumentLowering.expression(argumentName, typeName, currentNamespace)
                },
            ) ?: return null
        }
        val projection = valueTypeProjectionSupport.methodProjection(
            returnType = method.returnType,
            parameterTypes = method.parameters.map(WinMdParameter::type),
            currentNamespace = currentNamespace,
            supportsObjectReturnType = { typeName -> supportsRuntimeObjectReturnType(typeName, currentNamespace) },
        ) ?: return null
        val rendered = projection.renderRuntimeCall(method, currentNamespace, argumentExpressions) { abiCall ->
            runtimeObjectReturnCode(
                method = method,
                currentNamespace = currentNamespace,
                abiCall = abiCall,
            )
        }
        return RuntimeMethodPlan(
            nullPointerReturn = {
                PlannedStatement(
                    rendered.nullPointerReturn.statement,
                    rendered.nullPointerReturn.args.toTypedArray(),
                )
            },
            returnStatement = rendered.statement,
            statementArgs = { _, _, _ -> rendered.args.toTypedArray() },
        )
    }

    private fun runtimeMethodPlanForKey(
        signatureKey: MethodSignatureKey,
    ): RuntimeMethodPlan? {
        val parameterCategories = signatureKey.shape.toParameterCategories() ?: return null
        return when {
            parameterCategories.size <= 1 -> plannedUnaryRuntimeMethod(signatureKey)
            signatureKey.returnKind == MethodReturnKind.UNIT -> plannedTwoArgumentUnitRuntimeMethod(signatureKey)
            parameterCategories.size == 2 -> plannedTwoArgumentRuntimeMethod(signatureKey)
            else -> null
        }
    }

    private fun plannedTwoArgumentUnitRuntimeMethod(
        signatureKey: MethodSignatureKey,
    ): RuntimeMethodPlan? {
        val parameterCategories = signatureKey.shape.toParameterCategories()
            ?.takeIf(List<MethodParameterCategory>::isSupportedTwoArgumentUnitCategories)
            ?: return null
        return RuntimeMethodPlan(
            nullPointerReturn = { PlannedStatement("return") },
            returnStatement = "%L",
            statementArgs = { method, currentNamespace, parameterBindings ->
                val argumentExpressions = abiArgumentExpressions(
                    parameters = parameterBindings,
                    parameterCategories = parameterCategories,
                    argumentName = RuntimeMethodParameterBinding::name,
                    parameterType = RuntimeMethodParameterBinding::type,
                    lowerObjectArgument = { binding ->
                        runtimeObjectArgumentExpression(binding, currentNamespace)
                    },
                )
                arrayOf(
                    AbiCallCatalog.unitMethodWithTwoArguments(
                        method.vtableIndex!!,
                        parameterCategories,
                        argumentExpressions[0],
                        argumentExpressions[1],
                    ),
                )
            },
        )
    }

    private fun plannedUnaryRuntimeMethod(signatureKey: MethodSignatureKey): RuntimeMethodPlan? {
        val parameterCategories = signatureKey.shape.toParameterCategories() ?: return null
        if (parameterCategories.size > 1) return null
        val parameterCategory = parameterCategories.singleOrNull()
        return when (signatureKey.returnKind) {
            MethodReturnKind.STRING,
            MethodReturnKind.FLOAT32,
            MethodReturnKind.FLOAT64,
            MethodReturnKind.DATE_TIME,
            MethodReturnKind.TIME_SPAN,
            MethodReturnKind.BOOLEAN,
            MethodReturnKind.INT32,
            MethodReturnKind.UINT32,
            MethodReturnKind.INT64,
            MethodReturnKind.UINT64,
            MethodReturnKind.EVENT_REGISTRATION_TOKEN,
            MethodReturnKind.GUID,
            MethodReturnKind.OBJECT,
            MethodReturnKind.UNIT -> plannedUnaryRuntimeMethodForReturnKind(signatureKey.returnKind, parameterCategory)
        }
    }

    private fun plannedUnaryRuntimeMethodForReturnKind(
        returnKind: MethodReturnKind,
        parameterCategory: MethodParameterCategory?,
    ): RuntimeMethodPlan =
        RuntimeMethodPlan(
            nullPointerReturn = { method -> unaryRuntimeNullReturn(returnKind, method.returnType, method.name) },
            returnStatement = unaryRuntimeStatement(returnKind),
            statementArgs = { method, currentNamespace, parameterBindings ->
                val abiCall = unaryRuntimeAbiCall(
                    method.vtableIndex!!,
                    returnKind,
                    parameterCategory,
                    parameterBindings.singleOrNull(),
                    currentNamespace,
                )
                unaryRuntimeArgs(method, currentNamespace, returnKind, abiCall)
            },
        )

    private fun unaryRuntimeNullReturn(
        returnKind: MethodReturnKind,
        returnType: String,
        methodName: String,
    ): PlannedStatement = when (returnKind) {
        MethodReturnKind.OBJECT -> PlannedStatement("error(%S)", arrayOf<Any>("Null runtime object pointer: $methodName"))
        MethodReturnKind.UNIT -> PlannedStatement("return")
        else -> twoArgumentNullReturn(returnType, methodName)
    }

    private fun unaryRuntimeStatement(returnKind: MethodReturnKind): String =
        if (returnKind == MethodReturnKind.UNIT) "%L" else "return %L"

    private fun unaryRuntimeArgs(
        method: WinMdMethod,
        currentNamespace: String,
        returnKind: MethodReturnKind,
        abiCall: CodeBlock,
    ): Array<Any> = when (returnKind) {
        MethodReturnKind.OBJECT -> arrayOf(runtimeObjectReturnCode(method, currentNamespace, abiCall))
        MethodReturnKind.UNIT -> arrayOf(abiCall)
        else -> arrayOf(twoArgumentReturnCode(method.returnType, abiCall))
    }

    private fun unaryRuntimeAbiCall(
        vtableIndex: Int,
        returnKind: MethodReturnKind,
        parameterCategory: MethodParameterCategory?,
        parameterBinding: RuntimeMethodParameterBinding?,
        currentNamespace: String,
    ): CodeBlock {
        if (parameterCategory == null) {
            return zeroArgumentUnaryAbiCall(vtableIndex, returnKind)
        }
        val binding = requireNotNull(parameterBinding)
        val argumentName = binding.name
        val loweredArgument = abiArgumentExpression(
            argumentName = binding.name,
            parameterType = binding.type,
            category = parameterCategory,
        ) { runtimeObjectArgumentExpression(binding, currentNamespace) }
        return if (returnKind == MethodReturnKind.STRING && parameterCategory == MethodParameterCategory.BOOLEAN) {
            AbiCallCatalog.hstringMethodWithUInt32(vtableIndex, "if ($loweredArgument) 1u else 0u")
        } else {
            defaultUnaryAbiCall(
                vtableIndex = vtableIndex,
                returnKind = returnKind,
                parameterCategory = parameterCategory,
                argumentName = argumentName,
                loweredArgument = loweredArgument,
                unsupportedMessage = "Unsupported unary runtime return kind: $returnKind",
            )
        }
    }

    private fun lowerRuntimeArrayMethodArgument(
        method: WinMdMethod,
        parameter: WinMdParameter,
        parameterBindings: List<RuntimeMethodParameterBinding>,
        currentNamespace: String,
    ): CodeBlock? {
        val parameterIndex = method.parameters.indexOf(parameter)
        val binding = parameterBindings[parameterIndex]
        valueTypeProjectionSupport.lowerGenericAbiArgument(
            type = parameter.type,
            currentNamespace = currentNamespace,
            argumentName = binding.name,
            supportsObjectType = { typeName -> supportsRuntimeObjectType(typeName, currentNamespace) },
            lowerObjectArgument = { argumentName, typeName ->
                projectedObjectArgumentLowering.expression(argumentName, typeName, currentNamespace)
            },
        )?.let { return CodeBlock.of("%L", it) }
        val parameterCategory = methodParameterCategory(
            typeRegistry.signatureParameterType(parameter.type, currentNamespace),
        ) { typeName -> supportsRuntimeObjectType(typeName, currentNamespace) } ?: return null
        return CodeBlock.of(
            "%L",
            abiArgumentExpression(
                argumentName = binding.name,
                parameterType = binding.type,
                category = parameterCategory,
            ) { runtimeObjectArgumentExpression(binding, currentNamespace) },
        )
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
                method.parameters.map { parameter -> typeRegistry.signatureParameterType(parameter.type, currentNamespace) },
                { typeName -> supportsRuntimeObjectType(typeName, currentNamespace) },
            ) ?: error("Unsupported two-argument return shape: ${signatureKey.shape}")
            val argumentExpressions = abiArgumentExpressions(
                parameters = parameterBindings,
                parameterCategories = parameterCategories,
                argumentName = RuntimeMethodParameterBinding::name,
                parameterType = RuntimeMethodParameterBinding::type,
                lowerObjectArgument = { binding ->
                    runtimeObjectArgumentExpression(binding, currentNamespace)
                },
            )
            val abiCall = AbiCallCatalog.resultMethodWithTwoArguments(
                method.vtableIndex!!,
                if (signatureKey.returnKind == MethodReturnKind.OBJECT) "OBJECT" else resultKindName(method.returnType),
                if (signatureKey.returnKind == MethodReturnKind.OBJECT) PoetSymbols.requireObjectMember else resultExtractor(method.returnType),
                parameterCategories,
                argumentExpressions[0],
                argumentExpressions[1],
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

        val functionName = projectedMethodName(method)
        val delegateClass = typeNameMapper.mapTypeName(method.parameters.single().type, currentNamespace)
        val plan = delegateLambdaPlanResolver.resolve(
            invokeMethod = invokeMethod,
            currentNamespace = currentNamespace,
            supportsObjectType = { typeName -> supportsRuntimeObjectType(typeName, currentNamespace) },
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

    private fun supportsRuntimeObjectType(type: String, currentNamespace: String): Boolean {
        return projectedObjectArgumentLowering.supportsInputType(type, currentNamespace)
    }

    private fun supportsRuntimeObjectReturnType(type: String, currentNamespace: String): Boolean {
        return !typeRegistry.isStructType(type, currentNamespace) &&
            !supportsIReferenceValueProjection(type, currentNamespace, typeRegistry) &&
            projectedObjectArgumentLowering.supportsInputType(type, currentNamespace)
    }

    private fun runtimeObjectArgumentExpression(
        binding: RuntimeMethodParameterBinding,
        currentNamespace: String,
    ): CodeBlock {
        return projectedObjectArgumentLowering.expression(binding.name, binding.type, currentNamespace)
    }

    private fun runtimeObjectReturnCode(method: WinMdMethod, currentNamespace: String, abiCall: CodeBlock): CodeBlock {
        typeRegistry.closedGenericInterfaceProjectionCall(
            typeName = method.returnType,
            currentNamespace = currentNamespace,
            abiCall = abiCall,
            typeNameMapper = typeNameMapper,
            winRtSignatureMapper = winRtSignatureMapper,
            winRtProjectionTypeMapper = winRtProjectionTypeMapper,
        )?.let { return it }
        val mappedType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
        return if (typeRegistry.findType(method.returnType, currentNamespace)?.kind == WinMdTypeKind.Interface) {
            CodeBlock.of("%T.from(%T(%L))", mappedType, PoetSymbols.inspectableClass, abiCall)
        } else {
            CodeBlock.of("%T(%L)", mappedType, abiCall)
        }
    }

    private fun runtimeInt32FillArrayAbiArguments(
        method: WinMdMethod,
        currentNamespace: String,
        parameterBindings: List<RuntimeMethodParameterBinding>,
        fillArrayParameter: WinMdParameter,
    ): List<CodeBlock>? {
        return int32FillArrayAbiArguments(
            parameters = method.parameters,
            lowerNonArrayArgument = { parameter ->
                val parameterIndex = method.parameters.indexOf(parameter)
                val binding = parameterBindings[parameterIndex]
                val parameterCategory = methodParameterCategory(
                    typeRegistry.signatureParameterType(parameter.type, currentNamespace),
                ) { typeName -> supportsRuntimeObjectType(typeName, currentNamespace) } ?: return@int32FillArrayAbiArguments null
                CodeBlock.of(
                    "%L",
                    abiArgumentExpression(
                        argumentName = binding.name,
                        parameterType = binding.type,
                        category = parameterCategory,
                    ) { runtimeObjectArgumentExpression(binding, currentNamespace) },
                )
            },
            lowerArrayArgument = { _ ->
                CodeBlock.of("%N", int32FillArrayBufferName(fillArrayParameter))
            },
        )
    }

    private fun runtimeVarargAbiCall(
        methodName: String,
        vtableIndex: Int,
        abiArguments: List<CodeBlock>,
    ): CodeBlock {
        return CodeBlock.builder()
            .add("%T.%L(pointer, %L", PoetSymbols.platformComInteropClass, methodName, vtableIndex)
            .apply {
                abiArguments.forEach { argument ->
                    add(", %L", argument)
                }
            }
            .add(").getOrThrow()")
            .build()
    }

    private fun runtimeVarargResultKindAbiCall(
        vtableIndex: Int,
        returnType: String,
        abiArguments: List<CodeBlock>,
    ): CodeBlock {
        return CodeBlock.builder()
            .add(
                "%T.invokeMethodWithResultKind(pointer, %L, %T.%L",
                PoetSymbols.platformComInteropClass,
                vtableIndex,
                PoetSymbols.comMethodResultKindClass,
                resultKindName(returnType),
            )
            .apply {
                abiArguments.forEach { argument ->
                    add(", %L", argument)
                }
            }
            .add(").getOrThrow().%M()", resultExtractor(returnType))
            .build()
    }

    private fun twoArgumentNullReturn(returnType: String, methodName: String): PlannedStatement {
        return when (canonicalWinRtSpecialType(returnType)) {
            "String" -> PlannedStatement("return %S", arrayOf(""))
            "Float32" -> PlannedStatement("return %T(0f)", arrayOf(PoetSymbols.float32Class))
            "Float64" -> PlannedStatement("return %T(0.0)", arrayOf(PoetSymbols.float64Class))
            "DateTime" -> PlannedStatement("return %T.fromEpochSeconds(0)", arrayOf(PoetSymbols.dateTimeClass))
            "TimeSpan" -> PlannedStatement("return %T.parse(%S)", arrayOf(PoetSymbols.timeSpanClass, "0s"))
            "Boolean" -> PlannedStatement("return %T.FALSE", arrayOf(PoetSymbols.winRtBooleanClass))
            "EventRegistrationToken" -> PlannedStatement("return %T(0)", arrayOf(PoetSymbols.eventRegistrationTokenClass))
            "HResult" -> PlannedStatement("return null")
            "Int32" -> PlannedStatement("return %T(0)", arrayOf(PoetSymbols.int32Class))
            "UInt32" -> PlannedStatement("return %T(0u)", arrayOf(PoetSymbols.uint32Class))
            "Int64" -> PlannedStatement("return %T(0L)", arrayOf(PoetSymbols.int64Class))
            "UInt64" -> PlannedStatement("return %T(0uL)", arrayOf(PoetSymbols.uint64Class))
            "Guid" -> PlannedStatement("return %T.parse(%S)", arrayOf(PoetSymbols.guidValueClass, "00000000000000000000000000000000"))
            else -> error("Unsupported two-argument null return type for $methodName: $returnType")
        }
    }


    private fun int32NullReturn(returnType: String): PlannedStatement {
        return if (isHResultType(returnType)) {
            PlannedStatement("return null")
        } else {
            PlannedStatement("return %T(0)", arrayOf(PoetSymbols.int32Class))
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
