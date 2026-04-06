package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeAliasSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdType

internal class DelegateTypeRenderer(
    private val typeNameMapper: TypeNameMapper,
) {
    fun render(type: WinMdType): TypeSpec {
        val typeVariables = type.genericParameters.map { TypeVariableName(it) }
        val rawTypeClass = projectedDeclarationClassName(type.namespace, type.name)
        val typeClass = if (typeVariables.isEmpty()) rawTypeClass else rawTypeClass.parameterizedBy(typeVariables)
        val declarationName = projectedDeclarationSimpleName(type.name)
        return TypeSpec.classBuilder(declarationName)
            .addModifiers(KModifier.OPEN)
            .apply {
                typeVariables.forEach(::addTypeVariable)
            }
            .primaryConstructor(pointerConstructor())
            .superclass(PoetSymbols.winRtInterfaceProjectionClass)
            .addSuperclassConstructorParameter("pointer")
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addSuperinterface(PoetSymbols.winRtInterfaceMetadataClass)
                    .addProperty(overrideStringProperty("qualifiedName", "${type.namespace}.${type.name}"))
                    .addProperty(
                        PropertySpec.builder("iid", PoetSymbols.guidClass)
                            .addModifiers(KModifier.OVERRIDE)
                            .initializer("%M(%S)", PoetSymbols.guidOfMember, type.guid ?: "00000000-0000-0000-0000-000000000000")
                            .build(),
                    )
                    .addInitializerBlock(
                        com.squareup.kotlinpoet.CodeBlock.of(
                            "%T.registerFactory(%S) { inspectable -> from(inspectable) }\n",
                            PoetSymbols.winRtProjectionFactoryRegistryClass,
                            "${type.namespace}.${type.name}",
                        ),
                    )
                    .addFunction(
                        FunSpec.builder("from")
                            .apply {
                                typeVariables.forEach(::addTypeVariable)
                            }
                            .returns(typeClass)
                            .addParameter("inspectable", PoetSymbols.inspectableClass)
                            .addStatement("return inspectable.%M(this, ::%L)", PoetSymbols.projectInterfaceMember, declarationName)
                            .build(),
                    )
                    .build(),
            )
            .build()
    }

    fun renderLambdaAlias(type: WinMdType): TypeAliasSpec? {
        val invokeMethod = type.methods.singleOrNull { it.name == "Invoke" } ?: return null
        val genericParameters = type.genericParameters.toSet()
        val typeVariables = type.genericParameters.map { TypeVariableName(it) }
        val returnType = delegateFunctionTypeName(invokeMethod.returnType, type.namespace, genericParameters) ?: return null
        val parameterTypes = invokeMethod.parameters.map { parameter ->
            delegateFunctionTypeName(parameter.type, type.namespace, genericParameters) ?: return null
        }
        return TypeAliasSpec.builder(
            "${projectedDeclarationSimpleName(type.name)}Handler",
            LambdaTypeName.get(returnType = returnType, parameters = parameterTypes.toTypedArray()),
        )
            .apply {
                typeVariables.forEach(::addTypeVariable)
            }
            .build()
    }

    private fun delegateFunctionTypeName(
        typeName: String,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): TypeName? {
        return when (typeName) {
            "Unit" -> Unit::class.asTypeName()
            "String" -> String::class.asTypeName()
            "Boolean" -> Boolean::class.asTypeName()
            "Int32" -> Int::class.asTypeName()
            "UInt32" -> UInt::class.asTypeName()
            "Int64" -> Long::class.asTypeName()
            "UInt64" -> ULong::class.asTypeName()
            "Float32" -> Float::class.asTypeName()
            "Float64" -> Double::class.asTypeName()
            "Object" -> PoetSymbols.inspectableClass
            else -> typeNameMapper.mapTypeName(typeName, currentNamespace, genericParameters)
        }
    }
}
