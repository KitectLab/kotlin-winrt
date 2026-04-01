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
        val invocationShape = methodSignatureShape(
            parameterTypes = method.parameters.map { it.type },
            supportsObjectType = ::supportsAsyncObjectInput,
        )?.takeIf {
            it == MethodSignatureShape.EMPTY ||
                it == MethodSignatureShape.STRING ||
                it == MethodSignatureShape.OBJECT
        } ?: return null
        val parameterName = when (invocationShape) {
            MethodSignatureShape.STRING -> method.parameters.single().name.replaceFirstChar(Char::lowercase)
            MethodSignatureShape.OBJECT -> method.parameters.single().name.replaceFirstChar(Char::lowercase)
            else -> null
        }
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
            invocation = when (invocationShape) {
                MethodSignatureShape.EMPTY ->
                    "%T.invokeObjectMethod(pointer, ${method.vtableIndex}).getOrThrow()"
                MethodSignatureShape.STRING ->
                    "%T.invokeObjectMethodWithStringArg(pointer, ${method.vtableIndex}, $parameterName).getOrThrow()"
                MethodSignatureShape.OBJECT ->
                    "%T.invokeObjectMethodWithObjectArg(pointer, ${method.vtableIndex}, $parameterName.pointer).getOrThrow()"
                else -> error("Unsupported async invocation shape: $invocationShape")
            },
            rawTaskCallFactory = rawTaskCallFactory,
        )
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
