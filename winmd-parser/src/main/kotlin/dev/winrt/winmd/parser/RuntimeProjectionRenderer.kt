package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec

internal class RuntimeProjectionRenderer {
    fun renderDefaultInterfaceProjection(defaultInterface: String): FunSpec? {
        if (
            defaultInterface.contains('<') ||
            defaultInterface.contains('`') ||
            defaultInterface.endsWith("[]")
        ) {
            return null
        }
        val simpleName = defaultInterface.substringAfterLast('.')
            .substringBefore('<')
            .substringBefore('`')
            .removeSuffix("[]")
        if (!isKotlinIdentifier(simpleName)) {
            return null
        }
        return FunSpec.builder("as$simpleName")
            .returns(ClassName(defaultInterface.substringBeforeLast('.').lowercase(), simpleName))
            .addStatement("return %L.from(this)", simpleName)
            .build()
    }
}
