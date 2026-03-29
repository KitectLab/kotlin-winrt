package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock

internal object HStringSupport {
    fun fromCall(hstringCall: CodeBlock): CodeBlock {
        return CodeBlock.of(
            "%L.use { it.toKotlinString() }",
            hstringCall,
        )
    }

    fun toKotlinString(pointerExpression: String, vtableIndex: Int): CodeBlock {
        return fromCall(AbiCallCatalog.hstringMethod(vtableIndex, pointerExpression))
    }
}
