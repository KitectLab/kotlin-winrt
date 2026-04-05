package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock

internal object HStringSupport {
    fun fromCall(hstringCall: CodeBlock): CodeBlock {
        return scopedCall(
            hstringCall,
            CodeBlock.of("value.toKotlinString()"),
        )
    }

    fun nullableFromCall(hstringCall: CodeBlock): CodeBlock {
        return scopedCall(
            hstringCall,
            CodeBlock.of("value.takeUnless { it.isNull }?.toKotlinString()"),
        )
    }

    fun toKotlinString(pointerExpression: String, vtableIndex: Int): CodeBlock {
        return fromCall(AbiCallCatalog.hstringMethod(vtableIndex, pointerExpression))
    }

    private fun scopedCall(hstringCall: CodeBlock, resultExpression: CodeBlock): CodeBlock {
        return CodeBlock.builder()
            .add("run {\n")
            .indent()
            .add("val value = %L\n", hstringCall)
            .add("try {\n")
            .indent()
            .add("%L\n", resultExpression)
            .unindent()
            .add("} finally {\n")
            .indent()
            .add("value.close()\n")
            .unindent()
            .add("}\n")
            .unindent()
            .add("}")
            .build()
    }
}
