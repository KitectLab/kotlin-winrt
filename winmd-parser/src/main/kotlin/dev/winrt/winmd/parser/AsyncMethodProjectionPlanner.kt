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
        val signature = when {
            returnType.startsWith("Windows.Foundation.IAsyncOperation<") ->
                asyncOperationResultSignature(returnType, currentNamespace)
            returnType.startsWith("Windows.Foundation.IAsyncOperationWithProgress<") ->
                asyncOperationWithProgressPlan(returnType, currentNamespace)?.resultSignature
            else -> null
        } ?: return null
        return CodeBlock.of("%T.signature(%S)", PoetSymbols.asyncResultTypesClass, signature)
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

    fun asyncProgressDescriptorExpression(returnType: String, currentNamespace: String): CodeBlock? {
        val progressPlan = when {
            returnType.startsWith("Windows.Foundation.IAsyncActionWithProgress<") ->
                asyncProgressPlan(returnType, currentNamespace)
            returnType.startsWith("Windows.Foundation.IAsyncOperationWithProgress<") ->
                asyncOperationWithProgressPlan(returnType, currentNamespace)?.let {
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

    fun asyncProgressPlan(returnType: String, currentNamespace: String): AsyncProgressPlan? {
        if (!returnType.startsWith("Windows.Foundation.IAsyncActionWithProgress<") || !returnType.endsWith(">")) {
            return null
        }
        val progressType = returnType.substringAfter('<').substringBeforeLast('>')
        return scalarAsyncProgressPlan(progressType, currentNamespace)
    }

    fun asyncOperationWithProgressPlan(returnType: String, currentNamespace: String): AsyncOperationWithProgressPlan? {
        if (!returnType.startsWith("Windows.Foundation.IAsyncOperationWithProgress<") || !returnType.endsWith(">")) {
            return null
        }
        val arguments = splitGenericArguments(returnType.substringAfter('<').substringBeforeLast('>'))
        if (arguments.size != 2) return null
        val resultSignature = winRtSignatureMapper.signatureFor(arguments[0], currentNamespace)
        val progressPlan = scalarAsyncProgressPlan(arguments[1], currentNamespace) ?: return null
        return AsyncOperationWithProgressPlan(
            resultSignature = resultSignature,
            progressSignature = progressPlan.progressSignature,
            valueKind = progressPlan.valueKind,
            decodeLambda = progressPlan.decodeLambda,
        )
    }

    private fun scalarAsyncProgressPlan(typeName: String, currentNamespace: String): AsyncProgressPlan? {
        val signature = winRtSignatureMapper.signatureFor(typeName, currentNamespace)
        return when (typeName) {
            "String" -> AsyncProgressPlan(signature, "STRING", "{ it as String }")
            "Boolean" -> AsyncProgressPlan(signature, "BOOLEAN", "{ it as Boolean }")
            "Int32" -> AsyncProgressPlan(signature, "INT32", "{ it as Int }")
            "UInt32" -> AsyncProgressPlan(signature, "UINT32", "{ it as UInt32 }")
            "Int64" -> AsyncProgressPlan(signature, "INT64", "{ it as Long }")
            "UInt64" -> AsyncProgressPlan(signature, "UINT64", "{ it as ULong }")
            "Float32" -> AsyncProgressPlan(signature, "FLOAT32", "{ it as Float }")
            "Float64" -> AsyncProgressPlan(signature, "FLOAT64", "{ it as Double }")
            else -> null
        }
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
}

internal data class AsyncProgressPlan(
    val progressSignature: String,
    val valueKind: String,
    val decodeLambda: String,
)

internal data class AsyncOperationWithProgressPlan(
    val resultSignature: String,
    val progressSignature: String,
    val valueKind: String,
    val decodeLambda: String,
)
