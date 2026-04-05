package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.TypeName
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdModel

internal class AsyncMethodRuleRegistry(
    private val typeNameMapper: TypeNameMapper,
    private val asyncMethodProjectionPlanner: AsyncMethodProjectionPlanner,
    private val projectedObjectArgumentLowering: ProjectedObjectArgumentLowering,
) {
    constructor(
        typeNameMapper: TypeNameMapper,
        asyncMethodProjectionPlanner: AsyncMethodProjectionPlanner,
    ) : this(
        typeNameMapper,
        asyncMethodProjectionPlanner,
        defaultProjectedObjectArgumentLowering(),
    )

    fun plan(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String> = emptySet(),
    ): AsyncMethodRulePlan? {
        if (!isKotlinIdentifier(method.name) || method.vtableIndex == null) {
            return null
        }
        val invocation = asyncInvocation(method, currentNamespace) ?: return null
        val rawReturnType = typeNameMapper.mapTypeName(method.returnType, currentNamespace, genericParameters)
        val awaitReturnType = asyncMethodProjectionPlanner.awaitReturnType(
            method.returnType,
            currentNamespace,
            genericParameters,
        ) ?: return null
        val progressLambdaType = asyncMethodProjectionPlanner.progressLambdaType(
            method.returnType,
            currentNamespace,
            genericParameters,
        )
        if (requiresProjectedAsyncResult(method.returnType, genericParameters) &&
            asyncMethodProjectionPlanner.asyncResultDescriptorExpression(method.returnType, currentNamespace, genericParameters) == null
        ) {
            return null
        }
        if (requiresProjectedAsyncProgress(method.returnType, genericParameters) &&
            asyncMethodProjectionPlanner.asyncProgressDescriptorExpression(method.returnType, currentNamespace, genericParameters) == null
        ) {
            return null
        }
        val rawTaskCallFactory = when {
            method.returnType == "Windows.Foundation.IAsyncAction" ->
                AsyncRawTaskCallFactory { invocation ->
                    AsyncTaskCallCatalog.asyncAction(rawReturnType, invocation, PoetSymbols.platformComInteropClass)
                }
            method.returnType.startsWith("Windows.Foundation.IAsyncOperation<") -> {
                val resultType = asyncMethodProjectionPlanner.asyncResultDescriptorExpression(
                    method.returnType,
                    currentNamespace,
                    genericParameters,
                ) ?: return null
                AsyncRawTaskCallFactory { invocation ->
                    AsyncTaskCallCatalog.asyncOperation(
                        rawReturnType,
                        invocation,
                        resultType,
                        PoetSymbols.platformComInteropClass,
                    )
                }
            }
            method.returnType.startsWith("Windows.Foundation.IAsyncActionWithProgress<") -> {
                val progressPlan = asyncMethodProjectionPlanner.asyncProgressPlan(
                    method.returnType,
                    currentNamespace,
                    genericParameters,
                ) ?: return null
                AsyncRawTaskCallFactory { invocation ->
                    AsyncTaskCallCatalog.asyncActionWithProgress(
                        rawReturnType,
                        invocation,
                        progressPlan,
                        PoetSymbols.platformComInteropClass,
                    )
                }
            }
            method.returnType.startsWith("Windows.Foundation.IAsyncOperationWithProgress<") -> {
                val progressPlan = asyncMethodProjectionPlanner.asyncOperationWithProgressPlan(
                    method.returnType,
                    currentNamespace,
                    genericParameters,
                ) ?: return null
                val resultType = asyncMethodProjectionPlanner.asyncResultDescriptorExpression(
                    method.returnType,
                    currentNamespace,
                    genericParameters,
                ) ?: return null
                AsyncRawTaskCallFactory { invocation ->
                    AsyncTaskCallCatalog.asyncOperationWithProgress(
                        rawReturnType,
                        invocation,
                        resultType,
                        progressPlan,
                        PoetSymbols.platformComInteropClass,
                    )
                }
            }
            else -> return null
        }
        return AsyncMethodRulePlan(
            rawReturnType = rawReturnType,
            awaitReturnType = awaitReturnType,
            progressLambdaType = progressLambdaType,
            invocation = invocation,
            rawTaskCallFactory = rawTaskCallFactory,
        )
    }

    private fun asyncInvocation(method: WinMdMethod, currentNamespace: String): String? {
        val vtableIndex = method.vtableIndex ?: return null
        val parameterNames = method.parameters.map { it.name.replaceFirstChar(Char::lowercase) }
        val parameterTypes = method.parameters.map { it.type }
        val parameterCategories = methodParameterCategories(parameterTypes) { typeName ->
            projectedObjectArgumentLowering.supportsInputType(typeName, currentNamespace)
        } ?: return null
        return when (parameterCategories.size) {
            0 -> "%T.invokeObjectMethod(pointer, $vtableIndex).getOrThrow()"
            1 -> asyncUnaryInvocation(
                parameterCategories.single(),
                parameterNames.single(),
                parameterTypes.single(),
                currentNamespace,
                vtableIndex,
            )
            2 -> asyncTwoArgumentInvocation(
                parameterCategories[0],
                parameterCategories[1],
                parameterNames[0],
                parameterNames[1],
                parameterTypes[0],
                parameterTypes[1],
                currentNamespace,
                vtableIndex,
            )
            else -> asyncGenericInvocation(parameterCategories, parameterNames, parameterTypes, currentNamespace, vtableIndex)
        }
    }

    private fun requiresProjectedAsyncResult(returnType: String, genericParameters: Set<String>): Boolean {
        val resultTypeName = when {
            returnType.startsWith("Windows.Foundation.IAsyncOperation<") ->
                returnType.substringAfter('<').substringBeforeLast('>')
            returnType.startsWith("Windows.Foundation.IAsyncOperationWithProgress<") ->
                returnType.substringAfter('<').substringBeforeLast('>').substringBefore(',')
            else -> null
        } ?: return false
        return containsGenericParameter(resultTypeName, genericParameters) && resultTypeName !in scalarAsyncResultTypeNames
    }

    private fun requiresProjectedAsyncProgress(returnType: String, genericParameters: Set<String>): Boolean {
        val progressTypeName = when {
            returnType.startsWith("Windows.Foundation.IAsyncActionWithProgress<") ->
                returnType.substringAfter('<').substringBeforeLast('>')
            returnType.startsWith("Windows.Foundation.IAsyncOperationWithProgress<") ->
                returnType.substringAfter('<').substringBeforeLast('>').substringAfter(',')
            else -> null
        } ?: return false
        return containsGenericParameter(progressTypeName, genericParameters) && progressTypeName !in scalarAsyncProgressTypeNames
    }

    private fun asyncUnaryInvocation(
        category: MethodParameterCategory,
        parameterName: String,
        parameterType: String,
        currentNamespace: String,
        vtableIndex: Int,
    ): String? {
        val loweredArgument = asyncArgumentExpression(category, parameterName, parameterType, currentNamespace)
        return when (category) {
            MethodParameterCategory.STRING ->
                "%T.invokeObjectMethodWithStringArg(pointer, $vtableIndex, $loweredArgument).getOrThrow()"
            MethodParameterCategory.OBJECT ->
                "%T.invokeObjectMethodWithObjectArg(pointer, $vtableIndex, $loweredArgument).getOrThrow()"
            MethodParameterCategory.INT32 ->
                "%T.invokeObjectMethodWithInt32Arg(pointer, $vtableIndex, $loweredArgument).getOrThrow()"
            MethodParameterCategory.UINT32 ->
                "%T.invokeObjectMethodWithUInt32Arg(pointer, $vtableIndex, $loweredArgument).getOrThrow()"
            MethodParameterCategory.BOOLEAN ->
                "%T.invokeObjectMethodWithBooleanArg(pointer, $vtableIndex, $loweredArgument).getOrThrow()"
            MethodParameterCategory.INT64,
            MethodParameterCategory.EVENT_REGISTRATION_TOKEN ->
                "%T.invokeObjectMethodWithInt64Arg(pointer, $vtableIndex, $loweredArgument).getOrThrow()"
        }
    }

    private fun asyncTwoArgumentInvocation(
        first: MethodParameterCategory,
        second: MethodParameterCategory,
        firstName: String,
        secondName: String,
        firstType: String,
        secondType: String,
        currentNamespace: String,
        vtableIndex: Int,
    ): String? {
        val firstToken = first.toAbiToken().callNamePart()
        val secondToken = second.toAbiToken().callNamePart()
        val helperNamePart = if (firstToken == secondToken) {
            "Two${firstToken}Args"
        } else {
            "${firstToken}And${secondToken}Args"
        }
        val firstArgument = asyncArgumentExpression(first, firstName, firstType, currentNamespace)
        val secondArgument = asyncArgumentExpression(second, secondName, secondType, currentNamespace)
        return "dev.winrt.kom.requireObject(%T.invokeMethodWith${helperNamePart}(pointer, $vtableIndex, dev.winrt.kom.ComMethodResultKind.OBJECT, $firstArgument, $secondArgument).getOrThrow())"
    }

    private fun asyncGenericInvocation(
        categories: List<MethodParameterCategory>,
        parameterNames: List<String>,
        parameterTypes: List<String>,
        currentNamespace: String,
        vtableIndex: Int,
    ): String {
        val arguments = categories.indices.map { index ->
            asyncArgumentExpression(
                categories[index],
                parameterNames[index],
                parameterTypes[index],
                currentNamespace,
            )
        }
        return "dev.winrt.kom.requireObject(%T.invokeMethodWithResultKind(pointer, $vtableIndex, dev.winrt.kom.ComMethodResultKind.OBJECT, ${arguments.joinToString(", ")}).getOrThrow())"
    }

    private fun asyncArgumentExpression(
        category: MethodParameterCategory,
        parameterName: String,
        parameterType: String,
        currentNamespace: String,
    ): String {
        return when (category) {
            MethodParameterCategory.STRING -> parameterName
            MethodParameterCategory.INT32,
            MethodParameterCategory.UINT32,
            MethodParameterCategory.BOOLEAN,
            MethodParameterCategory.INT64,
            MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> "$parameterName.value"
            MethodParameterCategory.OBJECT ->
                projectedObjectArgumentLowering.expression(parameterName, parameterType, currentNamespace).toString()
        }
    }
}

private val scalarAsyncResultTypeNames = setOf(
    "String",
    "Boolean",
    "Int32",
    "UInt32",
    "Int64",
    "UInt64",
    "Float32",
    "Float64",
)

private val scalarAsyncProgressTypeNames = setOf(
    "String",
    "Boolean",
    "Int32",
    "UInt32",
    "Int64",
    "UInt64",
    "Float32",
    "Float64",
)

private fun containsGenericParameter(typeName: String, genericParameters: Set<String>): Boolean {
    if (typeName in genericParameters) {
        return true
    }
    val genericStart = typeName.indexOf('<')
    if (genericStart < 0 || !typeName.endsWith('>')) {
        return false
    }
    return splitGenericArguments(typeName.substring(genericStart + 1, typeName.length - 1))
        .any { containsGenericParameter(it, genericParameters) }
}

private fun splitGenericArguments(source: String): List<String> {
    if (source.isBlank()) {
        return emptyList()
    }
    val arguments = mutableListOf<String>()
    var depth = 0
    var start = 0
    source.forEachIndexed { index, char ->
        when (char) {
            '<' -> depth++
            '>' -> depth--
            ',' -> if (depth == 0) {
                arguments += source.substring(start, index).trim()
                start = index + 1
            }
        }
    }
    arguments += source.substring(start).trim()
    return arguments
}

private fun defaultProjectedObjectArgumentLowering(): ProjectedObjectArgumentLowering {
    val emptyTypeRegistry = TypeRegistry(WinMdModel(files = emptyList(), namespaces = emptyList()))
    return ProjectedObjectArgumentLowering(
        emptyTypeRegistry,
        WinRtSignatureMapper(emptyTypeRegistry),
        WinRtProjectionTypeMapper(),
    )
}

internal data class AsyncMethodRulePlan(
    val rawReturnType: TypeName,
    val awaitReturnType: TypeName,
    val progressLambdaType: TypeName?,
    val invocation: String,
    val rawTaskCallFactory: AsyncRawTaskCallFactory,
)

internal fun interface AsyncRawTaskCallFactory {
    fun create(invocationFormat: String): AsyncTaskCallPlan
}
