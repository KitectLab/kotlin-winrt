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
            typeName.startsWith("IReference<") -> {
                val inner = typeName.removePrefix("IReference<").removeSuffix(">")
                PoetSymbols.iReferenceClass.parameterizedBy(mapTypeName(inner, currentNamespace))
            }
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
        val simpleName = typeName.substringAfterLast('.')
        val namespace = typeName.substringBeforeLast('.', missingDelimiterValue = "")
        return ClassName(namespace.lowercase(), simpleName)
    }
}
