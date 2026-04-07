package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import kotlin.time.Duration
import kotlin.time.Instant
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind

internal class ValueTypeRenderer(
    private val typeNameMapper: TypeNameMapper,
    private val typeRegistry: TypeRegistry,
    private val valueTypeProjectionSupport: ValueTypeProjectionSupport = ValueTypeProjectionSupport(typeNameMapper, typeRegistry),
) {
    private data class DirectStructFieldProjection(
        val kotlinTypeName: TypeName,
        val layoutKindLiteral: String,
        val writer: (String) -> CodeBlock,
        val reader: CodeBlock,
    )

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

    private fun structFieldTypeName(typeName: String, currentNamespace: String): TypeName =
        directStructFieldProjection(typeName)?.kotlinTypeName ?: typeNameMapper.mapTypeName(typeName, currentNamespace)

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
        val directProjection = directStructFieldProjection(typeName)
        if (directProjection != null) {
            builder.addStatement("add(%T.%L)", PoetSymbols.comStructFieldKindClass, directProjection.layoutKindLiteral)
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
        if (
            supportsIReferenceValueProjection(typeName, currentNamespace, typeRegistry) ||
            supportsGenericIReferenceStructProjection(typeName, currentNamespace, typeRegistry) ||
            supportsGenericIReferenceEnumProjection(typeName, currentNamespace, typeRegistry)
        ) {
            builder.addStatement("add(%T.%L)", PoetSymbols.comStructFieldKindClass, "OBJECT")
            return
        }
        error("Unsupported struct field type for layout: $typeName")
    }

    private fun renderStructFieldWrite(typeName: String, propertyName: String, currentNamespace: String): CodeBlock {
        val directProjection = directStructFieldProjection(typeName)
        if (directProjection != null) {
            return directProjection.writer(propertyName)
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
        if (supportsIReferenceValueProjection(typeName, currentNamespace, typeRegistry)) {
            return CodeBlock.of(
                "writer.writeObjectPointer(%L, releaseOnClose = true)\n",
                valueTypeProjectionSupport.nullableValuePointerExpression(typeName, currentNamespace, propertyName)
                    ?: error("Unsupported struct field type for writer: $typeName"),
            )
        }
        if (supportsGenericIReferenceStructProjection(typeName, currentNamespace, typeRegistry)) {
            return CodeBlock.of(
                "writer.writeObjectPointer(%L, releaseOnClose = true)\n",
                valueTypeProjectionSupport.genericStructReferencePointerExpression(typeName, currentNamespace, propertyName)
                    ?: error("Unsupported struct field type for writer: $typeName"),
            )
        }
        if (supportsGenericIReferenceEnumProjection(typeName, currentNamespace, typeRegistry)) {
            return CodeBlock.of(
                "writer.writeObjectPointer(%L, releaseOnClose = true)\n",
                valueTypeProjectionSupport.genericEnumReferencePointerExpression(typeName, currentNamespace, propertyName)
                    ?: error("Unsupported struct field type for writer: $typeName"),
            )
        }
        error("Unsupported struct field type for writer: $typeName")
    }

    private fun renderStructFieldRead(typeName: String, currentNamespace: String): CodeBlock {
        val directProjection = directStructFieldProjection(typeName)
        if (directProjection != null) {
            return directProjection.reader
        }
        return when {
            typeRegistry.isStructType(typeName, currentNamespace) ->
                CodeBlock.of(
                    "%T.fromAbi(reader.readStruct(%T.ABI_LAYOUT))",
                    typeNameMapper.mapTypeName(typeName, currentNamespace),
                    typeNameMapper.mapTypeName(typeName, currentNamespace),
                )
            supportsIReferenceValueProjection(typeName, currentNamespace, typeRegistry) ->
                valueTypeProjectionSupport.nullableValueReturnExpression(
                    referenceType = typeName,
                    currentNamespace = currentNamespace,
                    abiCall = CodeBlock.of("reader.readObjectPointer()"),
                ) ?: error("Unsupported struct field type for reader: $typeName")
            supportsGenericIReferenceStructProjection(typeName, currentNamespace, typeRegistry) ->
                valueTypeProjectionSupport.genericStructReferenceReturnExpression(
                    referenceType = typeName,
                    currentNamespace = currentNamespace,
                    abiCall = CodeBlock.of("reader.readObjectPointer()"),
                ) ?: error("Unsupported struct field type for reader: $typeName")
            supportsGenericIReferenceEnumProjection(typeName, currentNamespace, typeRegistry) ->
                valueTypeProjectionSupport.genericEnumReferenceReturnExpression(
                    referenceType = typeName,
                    currentNamespace = currentNamespace,
                    abiCall = CodeBlock.of("reader.readObjectPointer()"),
                ) ?: error("Unsupported struct field type for reader: $typeName")
            typeRegistry.isEnumType(typeName, currentNamespace) -> {
                val mappedType = typeNameMapper.mapTypeName(typeName, currentNamespace)
                val underlyingRead = renderStructFieldRead(
                    enumUnderlyingTypeOrDefault(typeRegistry, typeName, currentNamespace),
                    currentNamespace,
                )
                CodeBlock.of("%T.fromValue(%L)", mappedType, underlyingRead)
            }
            else -> error("Unsupported struct field type for reader: $typeName")
        }
    }

    private fun directStructFieldProjection(typeName: String): DirectStructFieldProjection? {
        return when (canonicalWinRtSpecialType(typeName)) {
            "Boolean" -> scalarStructFieldProjection(Boolean::class.asTypeName(), "BOOLEAN", "writeBoolean", "readBoolean")
            "UInt8" -> scalarStructFieldProjection(UByte::class.asTypeName(), "UINT8", "writeUByte", "readUByte")
            "Int16" -> scalarStructFieldProjection(Short::class.asTypeName(), "INT16", "writeShort", "readShort")
            "UInt16" -> scalarStructFieldProjection(UShort::class.asTypeName(), "UINT16", "writeUShort", "readUShort")
            "Char16" -> scalarStructFieldProjection(Char::class.asTypeName(), "CHAR16", "writeChar", "readChar")
            "Int32" -> scalarStructFieldProjection(Int::class.asTypeName(), "INT32", "writeInt", "readInt")
            "UInt32" -> scalarStructFieldProjection(UInt::class.asTypeName(), "UINT32", "writeUInt", "readUInt")
            "Int64" -> scalarStructFieldProjection(Long::class.asTypeName(), "INT64", "writeLong", "readLong")
            "UInt64" -> scalarStructFieldProjection(ULong::class.asTypeName(), "UINT64", "writeULong", "readULong")
            "Float32" -> scalarStructFieldProjection(Float::class.asTypeName(), "FLOAT32", "writeFloat", "readFloat")
            "Float64" -> scalarStructFieldProjection(Double::class.asTypeName(), "FLOAT64", "writeDouble", "readDouble")
            "String" -> scalarStructFieldProjection(String::class.asTypeName(), "HSTRING", "writeHString", "readHString")
            "Guid" -> DirectStructFieldProjection(
                kotlinTypeName = PoetSymbols.guidValueClass,
                layoutKindLiteral = "GUID",
                writer = { propertyName -> CodeBlock.of("writer.writeGuid(%L)\n", propertyName) },
                reader = CodeBlock.of("reader.readGuid()"),
            )
            "DateTime" -> DirectStructFieldProjection(
                kotlinTypeName = Instant::class.asTypeName(),
                layoutKindLiteral = "INT64",
                writer = { propertyName ->
                    CodeBlock.of(
                        "writer.writeLong((((%L.epochSeconds * 10000000L) + (%L.nanosecondsOfSecond / 100)) + %LL))\n",
                        propertyName,
                        propertyName,
                        WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET,
                    )
                },
                reader = CodeBlock.of(
                    "reader.readLong().let { ticks -> %T.fromEpochSeconds((ticks - %LL) / 10000000L, (((ticks - %LL) %% 10000000L) * 100).toInt()) }",
                    Instant::class,
                    WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET,
                    WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET,
                ),
            )
            "TimeSpan" -> DirectStructFieldProjection(
                kotlinTypeName = Duration::class.asTypeName(),
                layoutKindLiteral = "INT64",
                writer = { propertyName -> CodeBlock.of("writer.writeLong(%L.inWholeNanoseconds / 100)\n", propertyName) },
                reader = CodeBlock.of("%T(reader.readLong())", Duration::class),
            )
            "EventRegistrationToken" -> DirectStructFieldProjection(
                kotlinTypeName = PoetSymbols.eventRegistrationTokenClass,
                layoutKindLiteral = "INT64",
                writer = { propertyName -> CodeBlock.of("writer.writeLong(%L.value)\n", propertyName) },
                reader = CodeBlock.of("%T(reader.readLong())", PoetSymbols.eventRegistrationTokenClass),
            )
            else -> null
        }
    }

    private fun scalarStructFieldProjection(
        kotlinTypeName: TypeName,
        layoutKindLiteral: String,
        writerMethod: String,
        readerMethod: String,
    ): DirectStructFieldProjection =
        DirectStructFieldProjection(
            kotlinTypeName = kotlinTypeName,
            layoutKindLiteral = layoutKindLiteral,
            writer = { propertyName -> CodeBlock.of("writer.$writerMethod(%L)\n", propertyName) },
            reader = CodeBlock.of("reader.$readerMethod()"),
        )

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
