package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdProperty

internal class RuntimePropertyRenderer(
    private val typeNameMapper: TypeNameMapper,
    private val typeRegistry: TypeRegistry,
    private val valueTypeProjectionSupport: ValueTypeProjectionSupport = ValueTypeProjectionSupport(typeNameMapper, typeRegistry),
) {
    fun canRenderRuntimeProperty(property: WinMdProperty, currentNamespace: String): Boolean {
        return runtimePropertyPlan(property, currentNamespace) != null
    }

    fun renderBackingProperty(property: WinMdProperty, currentNamespace: String): PropertySpec {
        val kotlinType = typeNameMapper.mapTypeName(property.type, currentNamespace)
        val defaultValue = when {
            typeRegistry.isEnumType(property.type, currentNamespace) -> {
                val underlyingType = enumUnderlyingTypeOrDefault(typeRegistry, property.type, currentNamespace)
                CodeBlock.of("%T.fromValue(%L)", kotlinType, enumZeroLiteral(underlyingType))
            }
            typeRegistry.isStructType(property.type, currentNamespace) ->
                valueTypeProjectionSupport.structDefaultValue(property.type, currentNamespace)
            supportsProjectedObjectTypeName(property.type) ->
                CodeBlock.of("%T(%T.NULL)", kotlinType, PoetSymbols.comPtrClass)
            else -> typeNameMapper.defaultValueFor(kotlinType)
        }
        return PropertySpec.builder("backing_${property.name}", PoetSymbols.runtimePropertyClass.parameterizedBy(kotlinType))
            .addModifiers(KModifier.PRIVATE)
            .initializer("%T(%L)", PoetSymbols.runtimePropertyClass.parameterizedBy(kotlinType), defaultValue)
            .build()
    }

    fun renderRuntimeProperty(property: WinMdProperty, currentNamespace: String): PropertySpec {
        val propertyName = property.name.replaceFirstChar(Char::lowercase)
        val kotlinType = typeNameMapper.mapTypeName(property.type, currentNamespace)
        val builder = PropertySpec.builder(propertyName, kotlinType)
        if (property.mutable) {
            builder.mutable()
        }
        builder.getter(renderRuntimeGetter(property, currentNamespace))
        if (property.mutable) {
            builder.setter(renderRuntimeSetter(property, currentNamespace))
        }
        return builder.build()
    }

    private fun renderRuntimeGetter(property: WinMdProperty, currentNamespace: String): FunSpec {
        val plan = runtimePropertyPlan(property, currentNamespace)
        val backingName = "backing_${property.name}"
        val builder = FunSpec.getterBuilder()
        val getterPlan = plan?.getter
        if (getterPlan == null || property.getterVtableIndex == null) {
            return builder
                .addStatement("return %N.get()", backingName)
                .build()
        }
        return builder
            .beginControlFlow("if (pointer.isNull)")
            .addStatement("return %N.get()", backingName)
            .endControlFlow()
            .addStatement("return %L", getterPlan.render(property.getterVtableIndex!!))
            .build()
    }

    private fun renderRuntimeSetter(property: WinMdProperty, currentNamespace: String): FunSpec {
        val plan = runtimePropertyPlan(property, currentNamespace)
        val backingName = "backing_${property.name}"
        val builder = FunSpec.setterBuilder()
            .addParameter("value", typeNameMapper.mapTypeName(property.type, currentNamespace))
        val setterPlan = plan?.setter
        return if (setterPlan == null || property.setterVtableIndex == null) {
            builder
                .addStatement("%N.set(value)", backingName)
                .build()
        } else {
            builder
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("%N.set(value)", backingName)
                .addStatement("return")
                .endControlFlow()
                .addStatement(setterPlan.statement, *setterPlan.args(property.setterVtableIndex!!))
                .build()
        }
    }

    private fun runtimePropertyPlan(property: WinMdProperty, currentNamespace: String): RuntimePropertyPlan? {
        val propertyProjection = valueTypeProjectionSupport.propertyProjection(property.type, currentNamespace)
        val iReferenceInnerType = iReferenceInnerType(property.type)
        val enumType = typeNameMapper.mapTypeName(property.type, currentNamespace)
            .takeIf { typeRegistry.isEnumType(property.type, currentNamespace) }
        val objectPropertyType = supportsProjectedObjectTypeName(property.type)
            .takeIf { it && propertyProjection == null }
            ?.let { typeNameMapper.mapTypeName(property.type, currentNamespace) }
        val getterPlan = when {
            property.getterVtableIndex == null -> null
            propertyProjection != null -> RuntimePropertyGetterPlan { getterVtableIndex ->
                propertyProjection.runtimeGetterExpression(
                    property.type,
                    currentNamespace,
                    getterVtableIndex,
                ) ?: error("Unsupported value-aware property projection type: ${property.type}")
            }
            iReferenceInnerType != null -> RuntimePropertyGetterPlan { getterVtableIndex ->
                val valueGetter = when {
                    iReferenceInnerType == "Object" || canonicalWinRtSpecialType(iReferenceInnerType) == "Object" -> CodeBlock.of(
                        "%T.invokeObjectMethod(pointer, %L).getOrThrow().let { if (it.isNull) null else %T(it) }",
                        PoetSymbols.platformComInteropClass,
                        getterVtableIndex,
                        PoetSymbols.inspectableClass,
                    )
                    iReferenceInnerType == "String" || canonicalWinRtSpecialType(iReferenceInnerType) == "String" ->
                        HStringSupport.nullableFromCall(AbiCallCatalog.hstringMethod(getterVtableIndex))
                    else -> PropertyRuleRegistry.runtimeGetterDescriptor(canonicalWinRtSpecialType(iReferenceInnerType))
                        ?.render(iReferenceInnerType, getterVtableIndex, valueTypeProjectionSupport)
                        ?: error("Unsupported IReference projection type: $iReferenceInnerType")
                }
                CodeBlock.of("if (pointer.isNull) null else %L", valueGetter)
            }
            enumType != null -> RuntimePropertyGetterPlan { getterVtableIndex ->
                val underlyingType = enumUnderlyingTypeOrDefault(typeRegistry, property.type, currentNamespace)
                CodeBlock.of(
                    "%T.fromValue(%L)",
                    enumType,
                    enumGetterAbiCall(underlyingType, getterVtableIndex),
                )
            }
            objectPropertyType != null -> RuntimePropertyGetterPlan { getterVtableIndex ->
                CodeBlock.of("%T(%L)", objectPropertyType, AbiCallCatalog.objectMethod(getterVtableIndex))
            }
            else -> PropertyRuleRegistry.runtimeGetterDescriptor(property.type)?.let { descriptor ->
                RuntimePropertyGetterPlan { getterVtableIndex ->
                    descriptor.render(property.type, getterVtableIndex, valueTypeProjectionSupport)
                        ?: error("Unsupported scalar runtime property type: ${property.type}")
                }
            }
        }
        val setterPlan = when {
            propertyProjection != null -> RuntimePropertySetterPlan(
                statement = "%L",
                args = { setterVtableIndex ->
                    arrayOf(
                        propertyProjection.runtimeSetterExpression(
                            property.type,
                            currentNamespace,
                            setterVtableIndex,
                            "value",
                        ) ?: error("Unsupported value-aware property projection type: ${property.type}"),
                    )
                },
            )
            enumType != null -> {
                val underlyingType = enumUnderlyingTypeOrDefault(typeRegistry, property.type, currentNamespace)
                RuntimePropertySetterPlan(
                    statement = "%L",
                    args = { setterVtableIndex -> arrayOf(enumSetterAbiCall(underlyingType, setterVtableIndex)) },
                )
            }
            objectPropertyType != null -> RuntimePropertySetterPlan(
                statement = "%L",
                args = { setterVtableIndex -> arrayOf(AbiCallCatalog.objectSetter(setterVtableIndex, "value")) },
            )
            else -> PropertyRuleRegistry.runtimeSetterDescriptor(property.type)?.let { descriptor ->
                RuntimePropertySetterPlan(
                    statement = "%L",
                    args = { setterVtableIndex ->
                        arrayOf(descriptor.render(setterVtableIndex, valueTypeProjectionSupport))
                    },
                )
            }
        }
        return if (getterPlan != null || setterPlan != null) {
            RuntimePropertyPlan(getter = getterPlan, setter = setterPlan)
        } else {
            null
        }
    }

    private data class RuntimePropertyPlan(
        val getter: RuntimePropertyGetterPlan?,
        val setter: RuntimePropertySetterPlan?,
    )

    private data class RuntimePropertyGetterPlan(
        val render: (Int) -> CodeBlock,
    )

    private data class RuntimePropertySetterPlan(
        val statement: String,
        val args: (Int) -> Array<Any>,
    )
}
