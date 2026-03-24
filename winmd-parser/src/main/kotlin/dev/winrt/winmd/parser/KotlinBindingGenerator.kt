package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdProperty
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind

data class GeneratedFile(
    val relativePath: String,
    val content: String,
)

class KotlinBindingGenerator {
    fun generate(model: WinMdModel): List<GeneratedFile> {
        return model.namespaces.map { namespace ->
            GeneratedFile(
                relativePath = namespace.name.replace('.', '/') + "/Bindings.kt",
                content = renderNamespace(namespace),
            )
        }
    }

    private fun renderNamespace(namespace: WinMdNamespace): String {
        val packageName = namespace.name.lowercase()
        val declarations = namespace.types.joinToString("\n\n") { renderType(it) }

        return buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import dev.winrt.core.WinRtInterfaceProjection")
            appendLine("import dev.winrt.core.WinRtInterfaceMetadata")
            appendLine("import dev.winrt.core.WinRtRuntimeClassMetadata")
            appendLine("import dev.winrt.core.Inspectable")
            appendLine("import dev.winrt.core.projectInterface")
            appendLine("import dev.winrt.core.RuntimeClassId")
            appendLine("import dev.winrt.core.RuntimeProperty")
            appendLine("import dev.winrt.core.WinRtRuntime")
            appendLine("import dev.winrt.core.guidOf")
            appendLine("import dev.winrt.kom.ComPtr")
            appendLine()
            appendLine(declarations)
        }
    }

    private fun renderType(type: WinMdType): String {
        return when (type.kind) {
            WinMdTypeKind.Interface -> renderInterface(type)
            WinMdTypeKind.RuntimeClass -> renderRuntimeClass(type)
            WinMdTypeKind.Struct -> "data class ${type.name}(val raw: String = \"\")"
            WinMdTypeKind.Enum -> "enum class ${type.name} { Value }"
        }
    }

    private fun renderInterface(type: WinMdType): String {
        val methods = type.methods.joinToString("\n") { method ->
            "    fun ${method.name.replaceFirstChar(Char::lowercase)}(): ${method.returnType}"
        }
        return buildString {
            appendLine("open class ${type.name}(pointer: ComPtr) : WinRtInterfaceProjection(pointer) {")
            if (methods.isNotBlank()) {
                appendLine(methods)
            }
            appendLine()
            appendLine("    companion object : WinRtInterfaceMetadata {")
            appendLine("        override val qualifiedName: String = \"${type.namespace}.${type.name}\"")
            appendLine("        override val iid = guidOf(\"${type.guid ?: "00000000-0000-0000-0000-000000000000"}\")")
            appendLine("        fun from(Inspectable: Inspectable): ${type.name} = Inspectable.projectInterface(this, ::${type.name})")
            appendLine("    }")
            append("}")
        }
    }

    private fun renderRuntimeClass(type: WinMdType): String {
        val propertyDeclarations = type.properties.joinToString("\n") { property ->
            renderProperty(property)
        }
        val methods = type.methods.joinToString("\n") { method ->
            val functionName = method.name.replaceFirstChar(Char::lowercase)
            val returnExpression = when (method.returnType) {
                "Unit" -> "Unit"
                "String" -> "\"\""
                "Int" -> "0"
                "Boolean" -> "false"
                else -> "error(\"Stub method not implemented: $functionName\")"
            }
            "    fun $functionName(): ${method.returnType} = $returnExpression"
        }

        return buildString {
            appendLine("open class ${type.name}(pointer: ComPtr) : Inspectable(pointer) {")
            if (propertyDeclarations.isNotBlank()) {
                appendLine(propertyDeclarations)
            }
            if (methods.isNotBlank()) {
                appendLine(methods)
            }
            appendLine("    companion object : WinRtRuntimeClassMetadata {")
            appendLine("        override val qualifiedName: String = \"${type.namespace}.${type.name}\"")
            appendLine("        override val classId = RuntimeClassId(\"${type.namespace}\", \"${type.name}\")")
            appendLine("        override val defaultInterfaceName: String? = ${type.defaultInterface?.let { "\"$it\"" } ?: "null"}")
            appendLine("        fun activate(): ${type.name} = WinRtRuntime.activate(classId, ::${type.name})")
            appendLine("    }")
            type.defaultInterface?.let { defaultInterface ->
                val simpleName = defaultInterface.substringAfterLast('.')
                val accessorName = simpleName.replaceFirstChar(Char::lowercase)
                appendLine()
                appendLine("    fun as${simpleName}(): ${simpleName} = ${simpleName}.from(this)")
            }
            append("}")
        }
    }

    private fun renderProperty(property: WinMdProperty): String {
        val keyword = if (property.mutable) "var" else "val"
        val setter = if (property.mutable) {
            """
                set(value) {
                    backing_${property.name}.set(value)
                }
            """.trimIndent()
        } else {
            ""
        }

        return buildString {
            appendLine("    private val backing_${property.name} = RuntimeProperty<${property.type}>(\"\" as ${property.type})")
            appendLine("    $keyword ${property.name.replaceFirstChar(Char::lowercase)}: ${property.type}")
            appendLine("        get() = backing_${property.name}.get()")
            if (setter.isNotBlank()) {
                appendLine("        $setter")
            }
        }
    }
}
