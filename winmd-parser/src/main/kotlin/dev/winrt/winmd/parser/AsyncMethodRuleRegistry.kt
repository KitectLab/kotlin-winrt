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
        val parameterNames = method.parameters.map { it.name.replaceFirstChar(Char::lowercase) }
        val parameterTypes = method.parameters.map { it.type }
        return when {
            parameterTypes.isEmpty() ->
                "%T.invokeObjectMethod(pointer, ${method.vtableIndex}).getOrThrow()"
            parameterTypes == listOf("String") ->
                "%T.invokeObjectMethodWithStringArg(pointer, ${method.vtableIndex}, ${parameterNames.single()}).getOrThrow()"
            parameterTypes.size == 1 && supportsAsyncObjectInput(parameterTypes.single()) ->
                "%T.invokeObjectMethodWithObjectArg(pointer, ${method.vtableIndex}, ${parameterNames.single()}.pointer).getOrThrow()"
            parameterTypes.size == 2 && parameterTypes.all(::supportsAsyncObjectInput) ->
                "%T.invokeObjectMethodWithTwoObjectArgs(pointer, ${method.vtableIndex}, ${parameterNames[0]}.pointer, ${parameterNames[1]}.pointer).getOrThrow()"
            else -> null
        }
    }

    private fun supportsAsyncObjectInput(typeName: String): Boolean {
        return (typeName == "Object" || typeName.contains('.')) &&
            !typeName.contains('<') &&
            !typeName.contains('`') &&
            !typeName.endsWith("[]")
    }
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
