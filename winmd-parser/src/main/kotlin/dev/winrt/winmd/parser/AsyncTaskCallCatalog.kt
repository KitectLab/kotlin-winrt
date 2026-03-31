package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.CodeBlock

internal object AsyncTaskCallCatalog {
    fun asyncAction(returnType: TypeName, invocationFormat: String, platformComInteropClass: Any): AsyncTaskCallPlan {
        return AsyncTaskCallPlan(
            statementFormat = "return %T($invocationFormat)",
            args = arrayOf(returnType, platformComInteropClass),
        )
    }

    fun asyncOperation(
        returnType: TypeName,
        invocationFormat: String,
        resultType: CodeBlock,
        platformComInteropClass: Any,
    ): AsyncTaskCallPlan {
        return AsyncTaskCallPlan(
            statementFormat = "return %T($invocationFormat, %L)",
            args = arrayOf(returnType, platformComInteropClass, resultType),
        )
    }

    fun asyncActionWithProgress(
        returnType: TypeName,
        invocationFormat: String,
        progressPlan: AsyncProgressPlan,
        platformComInteropClass: Any,
    ): AsyncTaskCallPlan {
        return AsyncTaskCallPlan(
            statementFormat = "return %T($invocationFormat, %S, %T.%L, %L)",
            args = arrayOf(
                returnType,
                platformComInteropClass,
                progressPlan.progressSignature,
                PoetSymbols.winRtDelegateValueKindClass,
                progressPlan.valueKind,
                progressPlan.decodeLambda,
            ),
        )
    }

    fun asyncOperationWithProgress(
        returnType: TypeName,
        invocationFormat: String,
        resultType: CodeBlock,
        progressPlan: AsyncOperationWithProgressPlan,
        platformComInteropClass: Any,
    ): AsyncTaskCallPlan {
        return AsyncTaskCallPlan(
            statementFormat = "return %T($invocationFormat, %L, %S, %T.%L, %L)",
            args = arrayOf(
                returnType,
                platformComInteropClass,
                resultType,
                progressPlan.progressSignature,
                PoetSymbols.winRtDelegateValueKindClass,
                progressPlan.valueKind,
                progressPlan.decodeLambda,
            ),
        )
    }
}

internal data class AsyncTaskCallPlan(
    val statementFormat: String,
    val args: Array<Any>,
)
