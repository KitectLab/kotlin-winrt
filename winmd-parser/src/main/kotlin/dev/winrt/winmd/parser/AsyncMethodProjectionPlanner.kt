package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.asTypeName

internal class AsyncMethodProjectionPlanner(
    private val typeNameMapper: TypeNameMapper,
    private val winRtSignatureMapper: WinRtSignatureMapper,
) {
    fun awaitReturnType(
        returnType: String,
        currentNamespace: String,
        genericParameters: Set<String> = emptySet(),
    ): TypeName? {
        return when {
            returnType == "Windows.Foundation.IAsyncAction" -> Unit::class.asTypeName()
            returnType.startsWith("Windows.Foundation.IAsyncOperation<") ->
                (typeNameMapper.mapTypeName(returnType, currentNamespace, genericParameters) as? com.squareup.kotlinpoet.ParameterizedTypeName)
                    ?.typeArguments
                    ?.singleOrNull()
            returnType.startsWith("Windows.Foundation.IAsyncActionWithProgress<") -> Unit::class.asTypeName()
            returnType.startsWith("Windows.Foundation.IAsyncOperationWithProgress<") ->
                (typeNameMapper.mapTypeName(returnType, currentNamespace, genericParameters) as? com.squareup.kotlinpoet.ParameterizedTypeName)
                    ?.typeArguments
                    ?.firstOrNull()
            else -> null
        }
    }

    fun isAsyncTaskReturn(returnType: String, currentNamespace: String): Boolean {
        return when {
            returnType == "Windows.Foundation.IAsyncAction" -> true
            returnType.startsWith("Windows.Foundation.IAsyncOperation<") ->
                asyncOperationResultSignature(returnType, currentNamespace) != null
            returnType.startsWith("Windows.Foundation.IAsyncActionWithProgress<") ->
                asyncProgressPlan(returnType, currentNamespace) != null
            returnType.startsWith("Windows.Foundation.IAsyncOperationWithProgress<") ->
                asyncOperationWithProgressPlan(returnType, currentNamespace) != null
            else -> false
        }
    }

    fun asyncOperationResultSignature(returnType: String, currentNamespace: String): String? {
        if (!returnType.startsWith("Windows.Foundation.IAsyncOperation<") || !returnType.endsWith(">")) {
            return null
        }
        val argument = returnType.substringAfter('<').substringBeforeLast('>')
        return winRtSignatureMapper.signatureFor(argument, currentNamespace)
    }

    fun asyncResultTypeName(
        returnType: String,
        currentNamespace: String,
        genericParameters: Set<String> = emptySet(),
    ): TypeName? = awaitReturnType(returnType, currentNamespace, genericParameters)

    fun asyncResultDescriptorExpression(returnType: String, currentNamespace: String): CodeBlock? {
        return asyncResultDescriptorExpression(returnType, currentNamespace, emptySet())
    }

    fun asyncResultDescriptorExpression(
        returnType: String,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): CodeBlock? {
        val resultTypeName = when {
            returnType.startsWith("Windows.Foundation.IAsyncOperation<") ->
                returnType.substringAfter('<').substringBeforeLast('>')
            returnType.startsWith("Windows.Foundation.IAsyncOperationWithProgress<") ->
                splitGenericArguments(returnType.substringAfter('<').substringBeforeLast('>')).firstOrNull()
            else -> null
        } ?: return null
        if (containsGenericParameter(resultTypeName, genericParameters) && resultTypeName !in scalarAsyncResultTypeNames) {
            return null
        }
        val signature = winRtSignatureMapper.signatureFor(resultTypeName, currentNamespace)
        if (resultTypeName in genericParameters || resultTypeName in scalarAsyncResultTypeNames) {
            return CodeBlock.of("%T.signature(%S)", PoetSymbols.asyncResultTypesClass, signature)
        }
        return CodeBlock.of(
            "%T.projected(%S) { %T(it) }",
            PoetSymbols.asyncResultTypesClass,
            signature,
            typeNameMapper.mapTypeName(resultTypeName, currentNamespace, genericParameters),
        )
    }

    fun progressLambdaType(
        returnType: String,
        currentNamespace: String,
        genericParameters: Set<String> = emptySet(),
    ): TypeName? {
        val progressType = when {
            returnType.startsWith("Windows.Foundation.IAsyncActionWithProgress<") ->
                returnType.substringAfter('<').substringBeforeLast('>')
            returnType.startsWith("Windows.Foundation.IAsyncOperationWithProgress<") ->
                splitGenericArguments(returnType.substringAfter('<').substringBeforeLast('>')).getOrNull(1)
            else -> null
        } ?: return null
        return LambdaTypeName.get(
            parameters = arrayOf(typeNameMapper.mapTypeName(progressType, currentNamespace, genericParameters)),
            returnType = Unit::class.asTypeName(),
        )
    }

    fun asyncProgressTypeName(
        returnType: String,
        currentNamespace: String,
        genericParameters: Set<String> = emptySet(),
    ): TypeName? {
        val progressLambda = progressLambdaType(returnType, currentNamespace, genericParameters) as? LambdaTypeName ?: return null
        return progressLambda.parameters.singleOrNull()?.type
    }

    fun asyncProgressDescriptorExpression(
        returnType: String,
        currentNamespace: String,
        genericParameters: Set<String> = emptySet(),
    ): CodeBlock? {
        val progressType = when {
            returnType.startsWith("Windows.Foundation.IAsyncActionWithProgress<") ->
                returnType.substringAfter('<').substringBeforeLast('>')
            returnType.startsWith("Windows.Foundation.IAsyncOperationWithProgress<") ->
                splitGenericArguments(returnType.substringAfter('<').substringBeforeLast('>')).getOrNull(1)
            else -> null
        } ?: return null
        if (containsGenericParameter(progressType, genericParameters) && progressType !in scalarAsyncProgressPlan.keys) {
            return null
        }
        val progressPlan = when {
            returnType.startsWith("Windows.Foundation.IAsyncActionWithProgress<") ->
                asyncProgressPlan(returnType, currentNamespace, genericParameters)
            returnType.startsWith("Windows.Foundation.IAsyncOperationWithProgress<") ->
                asyncOperationWithProgressPlan(returnType, currentNamespace, genericParameters)?.let {
                    AsyncProgressPlan(it.progressSignature, it.valueKind, it.decodeLambda)
                }
            else -> null
        } ?: return null
        return CodeBlock.of(
            "%T.signature(%S, %T.%L)",
            PoetSymbols.asyncProgressTypesClass,
            progressPlan.progressSignature,
            PoetSymbols.winRtDelegateValueKindClass,
            progressPlan.valueKind,
        )
    }

    fun asyncProgressPlan(
        returnType: String,
        currentNamespace: String,
        genericParameters: Set<String> = emptySet(),
    ): AsyncProgressPlan? {
        if (!returnType.startsWith("Windows.Foundation.IAsyncActionWithProgress<") || !returnType.endsWith(">")) {
            return null
        }
        val progressType = returnType.substringAfter('<').substringBeforeLast('>')
        return asyncProgressPlanForType(progressType, currentNamespace, genericParameters)
    }

    fun asyncOperationWithProgressPlan(
        returnType: String,
        currentNamespace: String,
        genericParameters: Set<String> = emptySet(),
    ): AsyncOperationWithProgressPlan? {
        if (!returnType.startsWith("Windows.Foundation.IAsyncOperationWithProgress<") || !returnType.endsWith(">")) {
            return null
        }
        val arguments = splitGenericArguments(returnType.substringAfter('<').substringBeforeLast('>'))
        if (arguments.size != 2) return null
        val resultSignature = winRtSignatureMapper.signatureFor(arguments[0], currentNamespace)
        val progressPlan = asyncProgressPlanForType(arguments[1], currentNamespace, genericParameters) ?: return null
        return AsyncOperationWithProgressPlan(
            resultSignature = resultSignature,
            progressSignature = progressPlan.progressSignature,
            valueKind = progressPlan.valueKind,
            decodeLambda = progressPlan.decodeLambda,
        )
    }

    private fun asyncProgressPlanForType(
        typeName: String,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): AsyncProgressPlan? {
        val signature = winRtSignatureMapper.signatureFor(typeName, currentNamespace)
        scalarAsyncProgressPlan[typeName]?.let { (valueKind, decodeLambda) ->
            return AsyncProgressPlan(signature, valueKind, decodeLambda)
        }
        if (typeName in genericParameters) return null
        return AsyncProgressPlan(
            progressSignature = signature,
            valueKind = "OBJECT",
            decodeLambda = CodeBlock.of(
                "{ %T(it as %T) }",
                typeNameMapper.mapTypeName(typeName, currentNamespace, genericParameters),
                PoetSymbols.comPtrClass,
            ),
        )
    }

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
}

private fun isScalarAsyncResultType(typeName: String): Boolean {
    return typeName in scalarAsyncResultTypeNames
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

private val scalarAsyncProgressPlan = mapOf(
    "String" to AsyncProgressPlanBlueprint("STRING", CodeBlock.of("{ it as String }")),
    "Boolean" to AsyncProgressPlanBlueprint("BOOLEAN", CodeBlock.of("{ it as Boolean }")),
    "Int32" to AsyncProgressPlanBlueprint("INT32", CodeBlock.of("{ it as Int }")),
    "UInt32" to AsyncProgressPlanBlueprint("UINT32", CodeBlock.of("{ it as UInt32 }")),
    "Int64" to AsyncProgressPlanBlueprint("INT64", CodeBlock.of("{ it as Long }")),
    "UInt64" to AsyncProgressPlanBlueprint("UINT64", CodeBlock.of("{ it as ULong }")),
    "Float32" to AsyncProgressPlanBlueprint("FLOAT32", CodeBlock.of("{ it as Float }")),
    "Float64" to AsyncProgressPlanBlueprint("FLOAT64", CodeBlock.of("{ it as Double }")),
)

private data class AsyncProgressPlanBlueprint(
    val valueKind: String,
    val decodeLambda: CodeBlock,
)

internal data class AsyncProgressPlan(
    val progressSignature: String,
    val valueKind: String,
    val decodeLambda: CodeBlock,
)

internal data class AsyncOperationWithProgressPlan(
    val resultSignature: String,
    val progressSignature: String,
    val valueKind: String,
    val decodeLambda: CodeBlock,
)
