package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.stripValueTypeNameMarker

internal const val WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET = 116444736000000000L

internal class TypeNameMapper {

    fun mapTypeName(
        typeName: String,
        currentNamespace: String,
        genericParameters: Set<String> = emptySet(),
    ): TypeName {
        val normalizedTypeName = stripValueTypeNameMarker(typeName)
        return when {
            normalizedTypeName in genericParameters -> TypeVariableName(normalizedTypeName)
            normalizedTypeName == "String" || normalizedTypeName == "System.String" -> String::class.asTypeName()
            normalizedTypeName == "Unit" -> Unit::class.asTypeName()
            normalizedTypeName == "Object" || normalizedTypeName == "System.Object" -> PoetSymbols.inspectableClass
            normalizedTypeName == "Boolean" || normalizedTypeName == "System.Boolean" || normalizedTypeName == "Windows.Foundation.WinRtBoolean" -> PoetSymbols.winRtBooleanClass
            normalizedTypeName == "UInt8" || normalizedTypeName == "System.Byte" || normalizedTypeName == "Windows.Foundation.UInt8" -> UByte::class.asTypeName()
            normalizedTypeName == "Int16" || normalizedTypeName == "System.Int16" || normalizedTypeName == "Windows.Foundation.Int16" -> Short::class.asTypeName()
            normalizedTypeName == "UInt16" || normalizedTypeName == "System.UInt16" || normalizedTypeName == "Windows.Foundation.UInt16" -> UShort::class.asTypeName()
            normalizedTypeName == "Char16" || normalizedTypeName == "System.Char" || normalizedTypeName == "Windows.Foundation.Char16" -> Char::class.asTypeName()
            normalizedTypeName == "Int" -> Int::class.asTypeName()
            normalizedTypeName == "Int32" || normalizedTypeName == "System.Int32" || normalizedTypeName == "Windows.Foundation.Int32" -> PoetSymbols.int32Class
            normalizedTypeName == "UInt32" || normalizedTypeName == "System.UInt32" || normalizedTypeName == "Windows.Foundation.UInt32" -> PoetSymbols.uint32Class
            normalizedTypeName == "Int64" || normalizedTypeName == "System.Int64" || normalizedTypeName == "Windows.Foundation.Int64" -> PoetSymbols.int64Class
            normalizedTypeName == "UInt64" || normalizedTypeName == "System.UInt64" || normalizedTypeName == "Windows.Foundation.UInt64" -> PoetSymbols.uint64Class
            normalizedTypeName == "Float32" || normalizedTypeName == "System.Single" || normalizedTypeName == "Windows.Foundation.Float32" -> PoetSymbols.float32Class
            normalizedTypeName == "Float64" || normalizedTypeName == "System.Double" || normalizedTypeName == "Windows.Foundation.Float64" -> PoetSymbols.float64Class
            normalizedTypeName == "Guid" || normalizedTypeName == "System.Guid" || normalizedTypeName == "Windows.Foundation.Guid" -> PoetSymbols.guidValueClass
            normalizedTypeName == "DateTime" || normalizedTypeName == "Windows.Foundation.DateTime" -> PoetSymbols.dateTimeClass
            normalizedTypeName == "TimeSpan" || normalizedTypeName == "Windows.Foundation.TimeSpan" -> PoetSymbols.timeSpanClass
            normalizedTypeName == "EventRegistrationToken" || normalizedTypeName == "Windows.Foundation.EventRegistrationToken" -> PoetSymbols.eventRegistrationTokenClass
            normalizedTypeName.endsWith("[]") -> arrayClass.parameterizedBy(
                mapTypeName(normalizedTypeName.removeSuffix("[]"), currentNamespace, genericParameters),
            )
            '<' in normalizedTypeName && normalizedTypeName.endsWith(">") -> mapGenericTypeName(normalizedTypeName, currentNamespace, genericParameters)
            '.' in normalizedTypeName -> normalizeQualifiedType(normalizedTypeName)
            else -> ClassName(currentNamespace.lowercase(), normalizedTypeName)
        }
    }

    fun defaultValueFor(typeName: TypeName, functionName: String? = null): CodeBlock {
        if (typeName.isNullable) {
            return CodeBlock.of("null")
        }
        val rendered = typeName.toString()
        return when {
            rendered == "kotlin.String" -> CodeBlock.of("%S", "")
            rendered == "kotlin.UByte" -> CodeBlock.of("0u.toUByte()")
            rendered == "kotlin.Short" -> CodeBlock.of("0.toShort()")
            rendered == "kotlin.UShort" -> CodeBlock.of("0u.toUShort()")
            rendered == "kotlin.Char" -> CodeBlock.of("%S.single()", "\u0000")
            rendered == "kotlin.Int" -> CodeBlock.of("0")
            rendered == "kotlin.Unit" -> CodeBlock.of("Unit")
            rendered == PoetSymbols.inspectableClass.canonicalName -> CodeBlock.of("%T(%T.NULL)", PoetSymbols.inspectableClass, PoetSymbols.comPtrClass)
            rendered == PoetSymbols.winRtBooleanClass.canonicalName -> CodeBlock.of("%T.FALSE", PoetSymbols.winRtBooleanClass)
            rendered == PoetSymbols.int32Class.canonicalName -> CodeBlock.of("%T(0)", PoetSymbols.int32Class)
            rendered == PoetSymbols.uint32Class.canonicalName -> CodeBlock.of("%T(0u)", PoetSymbols.uint32Class)
            rendered == PoetSymbols.int64Class.canonicalName -> CodeBlock.of("%T(0L)", PoetSymbols.int64Class)
            rendered == PoetSymbols.uint64Class.canonicalName -> CodeBlock.of("%T(0uL)", PoetSymbols.uint64Class)
            rendered == PoetSymbols.float32Class.canonicalName -> CodeBlock.of("%T(0f)", PoetSymbols.float32Class)
            rendered == PoetSymbols.float64Class.canonicalName -> CodeBlock.of("%T(0.0)", PoetSymbols.float64Class)
            rendered == PoetSymbols.dateTimeClass.canonicalName -> CodeBlock.of("%T.fromEpochSeconds(0)", PoetSymbols.dateTimeClass)
            rendered == PoetSymbols.timeSpanClass.canonicalName -> CodeBlock.of("%T.parse(" + "\"0s\"" + ")", PoetSymbols.timeSpanClass)
            rendered == PoetSymbols.eventRegistrationTokenClass.canonicalName -> CodeBlock.of("%T(0)", PoetSymbols.eventRegistrationTokenClass)
            rendered == PoetSymbols.guidValueClass.canonicalName -> CodeBlock.of("%T.parse(%S)", PoetSymbols.guidValueClass, "00000000000000000000000000000000")
            rendered.startsWith("kotlin.Array<") -> CodeBlock.of("emptyArray()")
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

    private fun mapGenericTypeName(typeName: String, currentNamespace: String, genericParameters: Set<String>): TypeName {
        val genericStart = typeName.indexOf('<')
        val rawType = typeName.substring(0, genericStart)
        val argumentSource = typeName.substring(genericStart + 1, typeName.length - 1)
        val arguments = splitGenericArguments(argumentSource).map { argument ->
            mapTypeName(argument, currentNamespace, genericParameters)
        }
        val rawTypeName = when (normalizeSimpleName(rawType.substringAfterLast('.'))) {
            "IReference" -> return arguments.single().copy(nullable = true)
            "IIterable" -> PoetSymbols.iterableClass
            "IIterator" -> PoetSymbols.iteratorClass
            "IMap" -> PoetSymbols.mutableMapClass
            "IMapView" -> PoetSymbols.mapClass
            "IKeyValuePair" -> PoetSymbols.mapEntryClass
            "IObservableVector" -> PoetSymbols.mutableListClass
            "IObservableMap" -> PoetSymbols.mutableMapClass
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

    private companion object {
        val arrayClass = ClassName("kotlin", "Array")
    }
}
