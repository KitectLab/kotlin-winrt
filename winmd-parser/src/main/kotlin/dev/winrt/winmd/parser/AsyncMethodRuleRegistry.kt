package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.TypeName
import dev.winrt.winmd.plugin.WinMdMethod

internal class AsyncMethodRuleRegistry(
    private val typeNameMapper: TypeNameMapper,
    private val asyncMethodProjectionPlanner: AsyncMethodProjectionPlanner,
) {
    fun plan(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String> = emptySet(),
    ): AsyncMethodRulePlan? {
        if (!isKotlinIdentifier(method.name) || method.vtableIndex == null) {
            return null
        }
        val invocation = asyncInvocation(method) ?: return null
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

    private fun asyncInvocation(method: WinMdMethod): String? {
        val vtableIndex = method.vtableIndex ?: return null
        val parameterNames = method.parameters.map { it.name.replaceFirstChar(Char::lowercase) }
        val parameterTypes = method.parameters.map { it.type }
        val parameterCategories = methodParameterCategories(parameterTypes, ::supportsAsyncObjectInput) ?: return null
        return when (parameterCategories.size) {
            0 -> "%T.invokeObjectMethod(pointer, $vtableIndex).getOrThrow()"
            1 -> asyncUnaryInvocation(parameterCategories.single(), parameterNames.single(), vtableIndex)
            2 -> asyncTwoArgumentInvocation(
                parameterCategories[0],
                parameterCategories[1],
                parameterNames[0],
                parameterNames[1],
                vtableIndex,
            )
            else -> asyncGenericInvocation(parameterCategories, parameterNames, vtableIndex)
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
        return resultTypeName in genericParameters && resultTypeName !in scalarAsyncResultTypeNames
    }

    private fun requiresProjectedAsyncProgress(returnType: String, genericParameters: Set<String>): Boolean {
        val progressTypeName = when {
            returnType.startsWith("Windows.Foundation.IAsyncActionWithProgress<") ->
                returnType.substringAfter('<').substringBeforeLast('>')
            returnType.startsWith("Windows.Foundation.IAsyncOperationWithProgress<") ->
                returnType.substringAfter('<').substringBeforeLast('>').substringAfter(',')
            else -> null
        } ?: return false
        return progressTypeName in genericParameters && progressTypeName !in scalarAsyncProgressTypeNames
    }

    private fun asyncUnaryInvocation(
        category: MethodParameterCategory,
        parameterName: String,
        vtableIndex: Int,
    ): String? {
        return when (category) {
            MethodParameterCategory.STRING ->
                "%T.invokeObjectMethodWithStringArg(pointer, $vtableIndex, $parameterName).getOrThrow()"
            MethodParameterCategory.OBJECT ->
                "%T.invokeObjectMethodWithObjectArg(pointer, $vtableIndex, $parameterName.pointer).getOrThrow()"
            MethodParameterCategory.INT32 ->
                "%T.invokeObjectMethodWithInt32Arg(pointer, $vtableIndex, $parameterName).getOrThrow()"
            MethodParameterCategory.UINT32 ->
                "%T.invokeObjectMethodWithUInt32Arg(pointer, $vtableIndex, $parameterName).getOrThrow()"
            MethodParameterCategory.BOOLEAN ->
                "%T.invokeObjectMethodWithBooleanArg(pointer, $vtableIndex, $parameterName).getOrThrow()"
            MethodParameterCategory.INT64,
            MethodParameterCategory.EVENT_REGISTRATION_TOKEN ->
                "%T.invokeObjectMethodWithInt64Arg(pointer, $vtableIndex, $parameterName).getOrThrow()"
        }
    }

    private fun asyncTwoArgumentInvocation(
        first: MethodParameterCategory,
        second: MethodParameterCategory,
        firstName: String,
        secondName: String,
        vtableIndex: Int,
    ): String? {
        val firstToken = first.toAbiToken().callNamePart()
        val secondToken = second.toAbiToken().callNamePart()
        val firstArgument = asyncArgumentExpression(first, firstName)
        val secondArgument = asyncArgumentExpression(second, secondName)
        return "dev.winrt.kom.requireObject(%T.invokeMethodWith${firstToken}And${secondToken}Args(pointer, $vtableIndex, dev.winrt.kom.ComMethodResultKind.OBJECT, $firstArgument, $secondArgument).getOrThrow())"
    }

    private fun asyncGenericInvocation(
        categories: List<MethodParameterCategory>,
        parameterNames: List<String>,
        vtableIndex: Int,
    ): String {
        val arguments = categories.zip(parameterNames) { category, name -> asyncArgumentExpression(category, name) }
        return "dev.winrt.kom.requireObject(%T.invokeMethodWithResultKind(pointer, $vtableIndex, dev.winrt.kom.ComMethodResultKind.OBJECT, ${arguments.joinToString(", ")}).getOrThrow())"
    }

    private fun asyncArgumentExpression(category: MethodParameterCategory, parameterName: String): String {
        return when (category) {
            MethodParameterCategory.STRING -> parameterName
            MethodParameterCategory.INT32,
            MethodParameterCategory.UINT32,
            MethodParameterCategory.BOOLEAN,
            MethodParameterCategory.INT64,
            MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> parameterName
            MethodParameterCategory.OBJECT -> "$parameterName.pointer"
        }
    }

private fun supportsAsyncObjectInput(typeName: String): Boolean {
    return (typeName == "Object" || typeName.contains('.')) &&
        !typeName.contains('<') &&
        !typeName.contains('`') &&
        !typeName.endsWith("[]")
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
