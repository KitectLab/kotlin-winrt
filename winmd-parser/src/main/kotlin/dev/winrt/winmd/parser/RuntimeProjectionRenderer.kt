package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec

internal class RuntimeProjectionRenderer {
    fun renderDefaultInterfaceProjection(defaultInterface: String): FunSpec? {
        return renderInterfaceProjection(defaultInterface)
    }

    fun renderImplementedInterfaceProjection(interfaceName: String): FunSpec? {
        return renderInterfaceProjection(interfaceName)
    }

    private fun renderInterfaceProjection(interfaceName: String): FunSpec? {
        if (
            interfaceName.contains('<') ||
            interfaceName.contains('`') ||
            interfaceName.endsWith("[]")
        ) {
            return null
        }
        val simpleName = interfaceName.substringAfterLast('.')
            .substringBefore('<')
            .substringBefore('`')
            .removeSuffix("[]")
        if (!isKotlinIdentifier(simpleName)) {
            return null
        }
        return FunSpec.builder("as$simpleName")
            .returns(ClassName(interfaceName.substringBeforeLast('.').lowercase(), simpleName))
            .addStatement("return %L.from(this)", simpleName)
            .build()
    }
}
