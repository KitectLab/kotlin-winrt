package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind

internal class ValueTypeRenderer(
    private val typeNameMapper: TypeNameMapper,
) {
    fun render(type: WinMdType): TypeSpec {
        return when (type.kind) {
            WinMdTypeKind.Struct -> renderStruct(type)
            WinMdTypeKind.Enum -> renderEnum(type)
            else -> error("Unsupported value type kind: ${type.kind}")
        }
    }

    private fun renderStruct(type: WinMdType): TypeSpec {
        return TypeSpec.classBuilder(projectedDeclarationSimpleName(type.name))
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
            .build()
    }

    private fun structFieldTypeName(typeName: String, currentNamespace: String) = when (typeName) {
        "Boolean",
        "Windows.Foundation.WinRtBoolean" -> Boolean::class.asTypeName()
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
