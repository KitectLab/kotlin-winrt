package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import kotlin.time.Duration
import kotlin.time.Instant
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind

internal class ValueTypeRenderer(
    private val typeNameMapper: TypeNameMapper,
    private val typeRegistry: TypeRegistry,
) {
    fun render(type: WinMdType): TypeSpec {
        return when (type.kind) {
            WinMdTypeKind.Struct -> renderStruct(type)
            WinMdTypeKind.Enum -> renderEnum(type)
            else -> error("Unsupported value type kind: ${type.kind}")
        }
    }

    private fun renderStruct(type: WinMdType): TypeSpec {
        val declarationName = projectedDeclarationSimpleName(type.name)
        val className = ClassName(type.namespace.lowercase(), declarationName)
        return TypeSpec.classBuilder(declarationName)
            .addModifiers(KModifier.DATA)
            .primaryConstructor(
                FunSpec.constructorBuilder().apply {
                    type.fields.forEach { field ->
                        addParameter(field.name.replaceFirstChar(Char::lowercase), structFieldTypeName(field.type, type.namespace))
                    }
                }.build(),
            )
            .apply {
                type.fields.forEach { field ->
                    addProperty(
                        PropertySpec.builder(field.name.replaceFirstChar(Char::lowercase), structFieldTypeName(field.type, type.namespace))
                            .initializer(field.name.replaceFirstChar(Char::lowercase))
                            .build(),
                    )
                }
            }
            .addFunction(renderStructToAbi(type))
            .addType(renderStructCompanion(type, className))
            .build()
    }

    private fun structFieldTypeName(typeName: String, currentNamespace: String) = when (typeName) {
        "Boolean",
        "Windows.Foundation.WinRtBoolean" -> Boolean::class.asTypeName()
        "UInt8",
        "Windows.Foundation.UInt8" -> UByte::class.asTypeName()
        "Int16",
        "Windows.Foundation.Int16" -> Short::class.asTypeName()
        "UInt16",
        "Windows.Foundation.UInt16" -> UShort::class.asTypeName()
        "Char16",
        "Windows.Foundation.Char16" -> Char::class.asTypeName()
        "Int32",
        "Windows.Foundation.Int32" -> Int::class.asTypeName()
        "UInt32",
        "Windows.Foundation.UInt32" -> UInt::class.asTypeName()
        "Int64",
        "Windows.Foundation.Int64" -> Long::class.asTypeName()
        "UInt64",
        "Windows.Foundation.UInt64" -> ULong::class.asTypeName()
        "Float32",
        "Windows.Foundation.Float32" -> Float::class.asTypeName()
        "Float64",
        "Windows.Foundation.Float64" -> Double::class.asTypeName()
        else -> typeNameMapper.mapTypeName(typeName, currentNamespace)
    }

    private fun renderStructToAbi(type: WinMdType): FunSpec {
        return FunSpec.builder("toAbi")
            .addModifiers(KModifier.INTERNAL)
            .returns(PoetSymbols.comStructValueClass)
            .addStatement("val writer = %T(ABI_LAYOUT)", PoetSymbols.comStructWriterClass)
            .apply {
                type.fields.forEach { field ->
                    addCode(renderStructFieldWrite(field.type, field.name.replaceFirstChar(Char::lowercase), type.namespace))
                }
            }
            .addStatement("return writer.build()")
            .build()
    }

    private fun renderStructCompanion(type: WinMdType, className: ClassName): TypeSpec {
        return TypeSpec.companionObjectBuilder()
            .addModifiers(KModifier.INTERNAL)
            .addProperty(
                PropertySpec.builder("ABI_LAYOUT", PoetSymbols.comStructLayoutClass)
                    .addModifiers(KModifier.INTERNAL)
                    .initializer(renderStructLayoutInitializer(type))
                    .build(),
            )
            .addFunction(
                FunSpec.builder("fromAbi")
                    .addModifiers(KModifier.INTERNAL)
                    .addParameter("value", PoetSymbols.comStructValueClass)
                    .returns(className)
                    .addStatement("val reader = %T(value)", PoetSymbols.comStructReaderClass)
                    .addCode(
                        CodeBlock.builder()
                            .add("return %T(", className)
                            .apply {
                                type.fields.forEachIndexed { index, field ->
                                    if (index > 0) {
                                        add(", ")
                                    }
                                    add("%L", renderStructFieldRead(field.type, type.namespace))
                                }
                            }
                            .add(")\n")
                            .build(),
                    )
                    .build(),
            )
            .build()
    }

    private fun renderStructLayoutInitializer(type: WinMdType): CodeBlock {
        val builder = CodeBlock.builder()
            .add("%T(\n", PoetSymbols.comStructLayoutClass)
            .indent()
            .add("buildList {\n")
            .indent()
        type.fields.forEach { field ->
            appendStructLayoutEntries(builder, field.type, type.namespace)
        }
        return builder
            .unindent()
            .add("}\n")
            .unindent()
            .add(")")
            .build()
    }

    private fun appendStructLayoutEntries(builder: CodeBlock.Builder, typeName: String, currentNamespace: String) {
        val kind = primitiveStructFieldKind(typeName, currentNamespace)
        if (kind != null) {
            builder.addStatement("add(%T.%L)", PoetSymbols.comStructFieldKindClass, kind)
            return
        }
        if (typeRegistry.isEnumType(typeName, currentNamespace)) {
            appendStructLayoutEntries(
                builder,
                enumUnderlyingTypeOrDefault(typeRegistry, typeName, currentNamespace),
                currentNamespace,
            )
            return
        }
        if (typeRegistry.isStructType(typeName, currentNamespace)) {
            builder.addStatement(
                "addAll(%T.ABI_LAYOUT.fields)",
                typeNameMapper.mapTypeName(typeName, currentNamespace),
            )
            return
        }
        error("Unsupported struct field type for layout: $typeName")
    }

    private fun renderStructFieldWrite(typeName: String, propertyName: String, currentNamespace: String): CodeBlock {
        when (canonicalWinRtSpecialType(typeName)) {
            "DateTime" -> {
                return CodeBlock.of(
                    "writer.writeLong((((%L.epochSeconds * 10000000L) + (%L.nanosecondsOfSecond / 100)) + %LL))\n",
                    propertyName,
                    propertyName,
                    WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET,
                )
            }
            "TimeSpan" -> return CodeBlock.of("writer.writeLong(%L.inWholeNanoseconds / 100)\n", propertyName)
            "EventRegistrationToken" -> return CodeBlock.of("writer.writeLong(%L.value)\n", propertyName)
        }
        val kind = primitiveStructFieldKind(typeName, currentNamespace)
        if (kind != null) {
            return when (kind) {
                "BOOLEAN" -> CodeBlock.of("writer.writeBoolean(%L)\n", propertyName)
                "INT8" -> CodeBlock.of("writer.writeByte(%L)\n", propertyName)
                "UINT8" -> CodeBlock.of("writer.writeUByte(%L)\n", propertyName)
                "INT16" -> CodeBlock.of("writer.writeShort(%L)\n", propertyName)
                "UINT16" -> CodeBlock.of("writer.writeUShort(%L)\n", propertyName)
                "CHAR16" -> CodeBlock.of("writer.writeChar(%L)\n", propertyName)
                "INT32" -> CodeBlock.of("writer.writeInt(%L)\n", propertyName)
                "UINT32" -> CodeBlock.of("writer.writeUInt(%L)\n", propertyName)
                "INT64" -> CodeBlock.of("writer.writeLong(%L)\n", propertyName)
                "UINT64" -> CodeBlock.of("writer.writeULong(%L)\n", propertyName)
                "FLOAT32" -> CodeBlock.of("writer.writeFloat(%L)\n", propertyName)
                "FLOAT64" -> CodeBlock.of("writer.writeDouble(%L)\n", propertyName)
                "GUID" -> CodeBlock.of("writer.writeGuid(%L)\n", propertyName)
                else -> error("Unsupported struct field kind: $kind")
            }
        }
        if (typeRegistry.isEnumType(typeName, currentNamespace)) {
            return renderStructFieldWrite(
                enumUnderlyingTypeOrDefault(typeRegistry, typeName, currentNamespace),
                "$propertyName.value",
                currentNamespace,
            )
        }
        if (typeRegistry.isStructType(typeName, currentNamespace)) {
            return CodeBlock.of("writer.writeStruct(%L.toAbi())\n", propertyName)
        }
        error("Unsupported struct field type for writer: $typeName")
    }

    private fun renderStructFieldRead(typeName: String, currentNamespace: String): CodeBlock {
        return when (canonicalWinRtSpecialType(typeName)) {
            "DateTime" -> CodeBlock.of(
                "reader.readLong().let { ticks -> %T.fromEpochSeconds((ticks - %LL) / 10000000L, (((ticks - %LL) %% 10000000L) * 100).toInt()) }",
                Instant::class,
                WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET,
                WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET,
            )
            "TimeSpan" -> CodeBlock.of("%T(reader.readLong())", Duration::class)
            "EventRegistrationToken" -> CodeBlock.of("%T(reader.readLong())", PoetSymbols.eventRegistrationTokenClass)
            else -> renderPrimitiveStructFieldRead(typeName, currentNamespace)
        }
    }

    private fun renderPrimitiveStructFieldRead(typeName: String, currentNamespace: String): CodeBlock {
        val kind = primitiveStructFieldKind(typeName, currentNamespace)
        if (kind != null) {
            return when (kind) {
                "BOOLEAN" -> CodeBlock.of("reader.readBoolean()")
                "INT8" -> CodeBlock.of("reader.readByte()")
                "UINT8" -> CodeBlock.of("reader.readUByte()")
                "INT16" -> CodeBlock.of("reader.readShort()")
                "UINT16" -> CodeBlock.of("reader.readUShort()")
                "CHAR16" -> CodeBlock.of("reader.readChar()")
                "INT32" -> CodeBlock.of("reader.readInt()")
                "UINT32" -> CodeBlock.of("reader.readUInt()")
                "INT64" -> CodeBlock.of("reader.readLong()")
                "UINT64" -> CodeBlock.of("reader.readULong()")
                "FLOAT32" -> CodeBlock.of("reader.readFloat()")
                "FLOAT64" -> CodeBlock.of("reader.readDouble()")
                "GUID" -> CodeBlock.of("reader.readGuid()")
                else -> error("Unsupported struct field kind: $kind")
            }
        }
        if (typeRegistry.isEnumType(typeName, currentNamespace)) {
            val mappedType = typeNameMapper.mapTypeName(typeName, currentNamespace)
            val underlyingRead = renderStructFieldRead(
                enumUnderlyingTypeOrDefault(typeRegistry, typeName, currentNamespace),
                currentNamespace,
            )
            return CodeBlock.of("%T.fromValue(%L)", mappedType, underlyingRead)
        }
        if (typeRegistry.isStructType(typeName, currentNamespace)) {
            val mappedType = typeNameMapper.mapTypeName(typeName, currentNamespace)
            return CodeBlock.of("%T.fromAbi(reader.readStruct(%T.ABI_LAYOUT))", mappedType, mappedType)
        }
        error("Unsupported struct field type for reader: $typeName")
    }

    private fun primitiveStructFieldKind(typeName: String, currentNamespace: String): String? {
        return when (canonicalWinRtSpecialType(typeName)) {
            "Boolean" -> "BOOLEAN"
            "UInt8" -> "UINT8"
            "Int16" -> "INT16"
            "UInt16" -> "UINT16"
            "Char16" -> "CHAR16"
            "Int32" -> "INT32"
            "UInt32" -> "UINT32"
            "Int64", "DateTime", "TimeSpan", "EventRegistrationToken" -> "INT64"
            "UInt64" -> "UINT64"
            "Float32" -> "FLOAT32"
            "Float64" -> "FLOAT64"
            "Guid" -> "GUID"
            else -> null
        }
    }

    private fun renderEnum(type: WinMdType): TypeSpec {
        val declarationName = projectedDeclarationSimpleName(type.name)
        val underlyingType = type.enumUnderlyingType ?: "Int32"
        val valueTypeName = enumValueTypeName(underlyingType)
        return TypeSpec.enumBuilder(declarationName)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("value", valueTypeName)
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("value", valueTypeName)
                    .initializer("value")
                    .build(),
            )
            .apply {
                type.enumMembers.forEach { member ->
                    addEnumConstant(
                        member.name,
                        TypeSpec.anonymousClassBuilder()
                            .addSuperclassConstructorParameter("%L", enumMemberLiteral(member.value, underlyingType))
                            .build(),
                    )
                }
            }
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addFunction(
                        FunSpec.builder("fromValue")
                            .addParameter("value", valueTypeName)
                            .returns(ClassName.bestGuess(declarationName))
                            .addCode(
                                CodeBlock.builder()
                                    .add("return entries.first { entry ->\n")
                                    .indent()
                                    .add("entry.value == value\n")
                                    .unindent()
                                    .add("}\n")
                                    .build(),
                            )
                            .build(),
                    )
                    .build(),
            )
            .build()
    }
}
