package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName

internal class TypeNameMapper {
    fun mapTypeName(typeName: String, currentNamespace: String): TypeName {
        return when {
            typeName == "String" -> String::class.asTypeName()
            typeName == "Unit" -> Unit::class.asTypeName()
            typeName == "Boolean" -> PoetSymbols.winRtBooleanClass
            typeName == "Int" -> Int::class.asTypeName()
            typeName == "Int32" -> PoetSymbols.int32Class
            typeName == "UInt32" -> PoetSymbols.uint32Class
            typeName == "Float32" -> PoetSymbols.float32Class
            typeName == "Float64" -> PoetSymbols.float64Class
            typeName == "Guid" -> PoetSymbols.guidValueClass
            typeName == "DateTime" -> PoetSymbols.dateTimeClass
            typeName == "TimeSpan" -> PoetSymbols.timeSpanClass
            typeName == "EventRegistrationToken" -> PoetSymbols.eventRegistrationTokenClass
            '<' in typeName && typeName.endsWith(">") -> mapGenericTypeName(typeName, currentNamespace)
            '.' in typeName -> normalizeQualifiedType(typeName)
            else -> ClassName(currentNamespace.lowercase(), typeName)
        }
    }

    fun defaultValueFor(typeName: TypeName, functionName: String? = null): CodeBlock {
        val rendered = typeName.toString()
        return when {
            rendered == "kotlin.String" -> CodeBlock.of("%S", "")
            rendered == "kotlin.Int" -> CodeBlock.of("0")
            rendered == "kotlin.Unit" -> CodeBlock.of("Unit")
            rendered.endsWith(".WinRtBoolean") -> CodeBlock.of("%T.FALSE", PoetSymbols.winRtBooleanClass)
            rendered.endsWith(".Int32") -> CodeBlock.of("%T(0)", PoetSymbols.int32Class)
            rendered.endsWith(".UInt32") -> CodeBlock.of("%T(0u)", PoetSymbols.uint32Class)
            rendered.endsWith(".Float32") -> CodeBlock.of("%T(0f)", PoetSymbols.float32Class)
            rendered.endsWith(".Float64") -> CodeBlock.of("%T(0.0)", PoetSymbols.float64Class)
            rendered.endsWith(".DateTime") -> CodeBlock.of("%T(0)", PoetSymbols.dateTimeClass)
            rendered.endsWith(".TimeSpan") -> CodeBlock.of("%T(0)", PoetSymbols.timeSpanClass)
            rendered.endsWith(".EventRegistrationToken") -> CodeBlock.of("%T(0)", PoetSymbols.eventRegistrationTokenClass)
            rendered.endsWith(".GuidValue") -> CodeBlock.of("%T(%S)", PoetSymbols.guidValueClass, "")
            rendered.startsWith("dev.winrt.core.IReference<") -> {
                CodeBlock.of(
                    "%T(%L)",
                    PoetSymbols.iReferenceClass.parameterizedBy(String::class.asTypeName()),
                    defaultValueFor(String::class.asTypeName()),
                )
            }
            else -> CodeBlock.of("error(%S)", "Stub method not implemented: ${functionName ?: rendered}")
        }
    }

    private fun normalizeQualifiedType(typeName: String): TypeName {
        val simpleName = normalizeSimpleName(typeName.substringAfterLast('.'))
        val namespace = typeName.substringBeforeLast('.', missingDelimiterValue = "")
        return ClassName(namespace.lowercase(), simpleName)
    }

    private fun mapGenericTypeName(typeName: String, currentNamespace: String): TypeName {
        val genericStart = typeName.indexOf('<')
        val rawType = typeName.substring(0, genericStart)
        val argumentSource = typeName.substring(genericStart + 1, typeName.length - 1)
        val arguments = splitGenericArguments(argumentSource).map { argument ->
            mapTypeName(argument, currentNamespace)
        }
        val rawTypeName = when (normalizeSimpleName(rawType.substringAfterLast('.'))) {
            "IReference" -> PoetSymbols.iReferenceClass
            else -> normalizeQualifiedType(rawType) as ClassName
        }
        return rawTypeName.parameterizedBy(arguments)
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

    private fun normalizeSimpleName(simpleName: String): String {
        return simpleName.substringBefore('`')
    }
}
