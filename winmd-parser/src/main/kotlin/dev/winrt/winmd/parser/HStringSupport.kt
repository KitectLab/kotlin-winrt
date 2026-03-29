package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock

internal object HStringSupport {
    fun toKotlinString(pointerExpression: String, vtableIndex: Int): CodeBlock {
        return hstringToKotlinString(AbiCallCatalog.hstringMethod(vtableIndex, pointerExpression))
    }

    fun toKotlinStringWithStringArg(pointerExpression: String, vtableIndex: Int, argumentName: String): CodeBlock {
        return hstringToKotlinString(AbiCallCatalog.hstringMethodWithString(vtableIndex, argumentName, pointerExpression))
    }

    fun toKotlinStringWithInt32Arg(pointerExpression: String, vtableIndex: Int, argumentName: String): CodeBlock {
        return hstringToKotlinString(AbiCallCatalog.hstringMethodWithInt32(vtableIndex, argumentName, pointerExpression))
    }

    fun toKotlinStringWithUInt32Arg(pointerExpression: String, vtableIndex: Int, argumentName: String): CodeBlock {
        return hstringToKotlinString(AbiCallCatalog.hstringMethodWithUInt32(vtableIndex, argumentName, pointerExpression))
    }

    private fun hstringToKotlinString(hstringCall: CodeBlock): CodeBlock {
        return CodeBlock.of(
            "%L.use { it.toKotlinString() }",
            hstringCall,
        )
    }
}
