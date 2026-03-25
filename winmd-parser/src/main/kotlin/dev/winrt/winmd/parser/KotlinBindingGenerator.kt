package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdField
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdEnumMember
import dev.winrt.winmd.plugin.WinMdProperty
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind

data class GeneratedFile(
    val relativePath: String,
    val content: String,
)

class KotlinBindingGenerator {
    fun generate(model: WinMdModel): List<GeneratedFile> {
        return model.namespaces.flatMap { namespace ->
            namespace.types.map { type ->
                GeneratedFile(
                    relativePath = namespace.name.replace('.', '/') + "/${type.name}.kt",
                    content = renderTypeFile(namespace, type),
                )
            }
        }
    }

    private fun renderTypeFile(namespace: WinMdNamespace, type: WinMdType): String {
        val packageName = namespace.name.lowercase()

        return buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import dev.winrt.core.DateTime")
            appendLine("import dev.winrt.core.EventRegistrationToken")
            appendLine("import dev.winrt.core.Float32")
            appendLine("import dev.winrt.core.Float64")
            appendLine("import dev.winrt.core.WinRtInterfaceProjection")
            appendLine("import dev.winrt.core.WinRtInterfaceMetadata")
            appendLine("import dev.winrt.core.WinRtRuntimeClassMetadata")
            appendLine("import dev.winrt.core.GuidValue")
            appendLine("import dev.winrt.core.Inspectable")
            appendLine("import dev.winrt.core.IReference")
            appendLine("import dev.winrt.core.Int32")
            appendLine("import dev.winrt.core.projectInterface")
            appendLine("import dev.winrt.core.RuntimeClassId")
            appendLine("import dev.winrt.core.RuntimeProperty")
            appendLine("import dev.winrt.core.TimeSpan")
            appendLine("import dev.winrt.core.UInt32")
            appendLine("import dev.winrt.core.WinRtRuntime")
            appendLine("import dev.winrt.core.WinRtStrings")
            appendLine("import dev.winrt.core.WinRtBoolean")
            appendLine("import dev.winrt.core.guidOf")
            appendLine("import dev.winrt.kom.ComPtr")
            appendLine("import dev.winrt.kom.PlatformComInterop")
            appendLine()
            appendLine(renderType(type))
        }
    }

    private fun renderType(type: WinMdType): String {
        return when (type.kind) {
            WinMdTypeKind.Interface -> renderInterface(type)
            WinMdTypeKind.RuntimeClass -> renderRuntimeClass(type)
            WinMdTypeKind.Struct -> renderStruct(type)
            WinMdTypeKind.Enum -> renderEnum(type)
        }
    }

    private fun renderInterface(type: WinMdType): String {
        val methods = type.methods.joinToString("\n") { method ->
            val functionName = method.name.replaceFirstChar(Char::lowercase)
            if (method.returnType == "String" && method.parameters.isEmpty() && method.vtableIndex != null) {
                """
                    fun $functionName(): String {
                        val value = PlatformComInterop.invokeHStringMethod(pointer, ${method.vtableIndex}).getOrThrow()
                        return try {
                            WinRtStrings.toKotlin(value)
                        } finally {
                            WinRtStrings.release(value)
                        }
                    }
                """.trimIndent().prependIndent("    ")
            } else {
                "    fun $functionName(): ${method.returnType}"
            }
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
            val kotlinType = mapType(method.returnType)
            if (method.returnType == "UInt32" && method.parameters.isEmpty() && method.vtableIndex != null) {
                return@joinToString buildString {
                    appendLine("    fun $functionName(): $kotlinType {")
                    appendLine("        if (pointer.isNull) return UInt32(0u)")
                    appendLine("        return UInt32(PlatformComInterop.invokeUInt32Method(pointer, ${method.vtableIndex}).getOrThrow())")
                    append("    }")
                }
            }
            val returnExpression = when (kotlinType) {
                "Unit" -> "Unit"
                "String" -> "\"\""
                "Int" -> "0"
                "Boolean" -> "false"
                "Int32" -> "Int32(0)"
                "UInt32" -> "UInt32(0u)"
                "WinRtBoolean" -> "WinRtBoolean.FALSE"
                "DateTime" -> "DateTime(0)"
                "TimeSpan" -> "TimeSpan(0)"
                "EventRegistrationToken" -> "EventRegistrationToken(0)"
                "GuidValue" -> "GuidValue(\"\")"
                else -> "error(\"Stub method not implemented: $functionName\")"
            }
            "    fun $functionName(): $kotlinType = $returnExpression"
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
        val propertyName = property.name.replaceFirstChar(Char::lowercase)
        val keyword = if (property.mutable) "var" else "val"
        val kotlinType = mapType(property.type)
        if (property.type == "Boolean" && property.getterVtableIndex != null) {
            return buildString {
                appendLine("    private val backing_${property.name} = RuntimeProperty<$kotlinType>(${defaultValueFor(kotlinType)})")
                appendLine("    $keyword $propertyName: $kotlinType")
                appendLine("        get() {")
                appendLine("            if (pointer.isNull) return backing_${property.name}.get()")
                appendLine("            return WinRtBoolean(PlatformComInterop.invokeBooleanGetter(pointer, ${property.getterVtableIndex}).getOrThrow())")
                appendLine("        }")
            }
        }
        if (property.type == "Guid" && property.getterVtableIndex != null) {
            return buildString {
                appendLine("    private val backing_${property.name} = RuntimeProperty<$kotlinType>(${defaultValueFor(kotlinType)})")
                appendLine("    $keyword $propertyName: $kotlinType")
                appendLine("        get() {")
                appendLine("            if (pointer.isNull) return backing_${property.name}.get()")
                appendLine("            return GuidValue(PlatformComInterop.invokeGuidGetter(pointer, ${property.getterVtableIndex}).getOrThrow().toString())")
                appendLine("        }")
            }
        }
        if (property.type == "DateTime" && property.getterVtableIndex != null) {
            return buildString {
                appendLine("    private val backing_${property.name} = RuntimeProperty<$kotlinType>(${defaultValueFor(kotlinType)})")
                appendLine("    $keyword $propertyName: $kotlinType")
                appendLine("        get() {")
                appendLine("            if (pointer.isNull) return backing_${property.name}.get()")
                appendLine("            return DateTime(PlatformComInterop.invokeInt64Getter(pointer, ${property.getterVtableIndex}).getOrThrow())")
                appendLine("        }")
            }
        }
        if (property.type == "TimeSpan" && property.getterVtableIndex != null) {
            return buildString {
                appendLine("    private val backing_${property.name} = RuntimeProperty<$kotlinType>(${defaultValueFor(kotlinType)})")
                appendLine("    $keyword $propertyName: $kotlinType")
                appendLine("        get() {")
                appendLine("            if (pointer.isNull) return backing_${property.name}.get()")
                appendLine("            return TimeSpan(PlatformComInterop.invokeInt64Getter(pointer, ${property.getterVtableIndex}).getOrThrow())")
                appendLine("        }")
            }
        }
        if (property.type == "String" && property.getterVtableIndex != null) {
            return buildString {
                appendLine("    private val backing_${property.name} = RuntimeProperty<$kotlinType>(${defaultValueFor(kotlinType)})")
                appendLine("    $keyword $propertyName: $kotlinType")
                appendLine("        get() {")
                appendLine("            if (pointer.isNull) return backing_${property.name}.get()")
                appendLine("            val value = PlatformComInterop.invokeHStringMethod(pointer, ${property.getterVtableIndex}).getOrThrow()")
                appendLine("            return try {")
                appendLine("                WinRtStrings.toKotlin(value)")
                appendLine("            } finally {")
                appendLine("                WinRtStrings.release(value)")
                appendLine("            }")
                appendLine("        }")
                if (property.mutable && property.setterVtableIndex != null) {
                    appendLine("        set(value) {")
                    appendLine("            if (pointer.isNull) {")
                    appendLine("                backing_${property.name}.set(value)")
                    appendLine("                return")
                    appendLine("            }")
                    appendLine("            PlatformComInterop.invokeStringSetter(pointer, ${property.setterVtableIndex}, value).getOrThrow()")
                    appendLine("        }")
                }
            }
        }

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
            appendLine("    private val backing_${property.name} = RuntimeProperty<$kotlinType>(${defaultValueFor(kotlinType)})")
            appendLine("    $keyword $propertyName: $kotlinType")
            appendLine("        get() = backing_${property.name}.get()")
            if (setter.isNotBlank()) {
                appendLine("        $setter")
            }
        }
    }

    private fun renderStruct(type: WinMdType): String {
        val fields = type.fields.joinToString(",\n") { field ->
            "    val ${field.name.replaceFirstChar(Char::lowercase)}: ${mapType(field.type)}"
        }

        return buildString {
            appendLine("data class ${type.name}(")
            if (fields.isNotBlank()) {
                appendLine(fields)
            }
            append(")")
        }
    }

    private fun renderEnum(type: WinMdType): String {
        val members = type.enumMembers.joinToString(",\n") { member ->
            "    ${member.name}(${member.value})"
        }

        return buildString {
            appendLine("enum class ${type.name}(val value: Int) {")
            if (members.isNotBlank()) {
                appendLine(members)
            }
            append("}")
        }
    }

    private fun mapType(typeName: String): String {
        return when {
            typeName == "String" -> "String"
            typeName == "Unit" -> "Unit"
            typeName == "Boolean" -> "WinRtBoolean"
            typeName == "Int" -> "Int"
            typeName == "Int32" -> "Int32"
            typeName == "UInt32" -> "UInt32"
            typeName == "Float32" -> "Float32"
            typeName == "Float64" -> "Float64"
            typeName == "Guid" -> "GuidValue"
            typeName == "DateTime" -> "DateTime"
            typeName == "TimeSpan" -> "TimeSpan"
            typeName == "EventRegistrationToken" -> "EventRegistrationToken"
            typeName.startsWith("IReference<") -> {
                val inner = typeName.removePrefix("IReference<").removeSuffix(">")
                "IReference<${mapType(inner)}>"
            }
            else -> typeName.substringAfterLast('.')
        }
    }

    private fun defaultValueFor(typeName: String): String {
        return when {
            typeName == "String" -> "\"\""
            typeName == "Int" -> "0"
            typeName == "Unit" -> "Unit"
            typeName == "WinRtBoolean" -> "WinRtBoolean.FALSE"
            typeName == "Int32" -> "Int32(0)"
            typeName == "UInt32" -> "UInt32(0u)"
            typeName == "Float32" -> "Float32(0f)"
            typeName == "Float64" -> "Float64(0.0)"
            typeName == "DateTime" -> "DateTime(0)"
            typeName == "TimeSpan" -> "TimeSpan(0)"
            typeName == "EventRegistrationToken" -> "EventRegistrationToken(0)"
            typeName == "GuidValue" -> "GuidValue(\"\")"
            typeName.startsWith("IReference<") -> "IReference(${defaultValueFor(typeName.removePrefix("IReference<").removeSuffix(">"))})"
            else -> "error(\"No default value for $typeName\")"
        }
    }
}
