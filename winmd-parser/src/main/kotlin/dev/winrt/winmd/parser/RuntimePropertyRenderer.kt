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

    private fun hStringToKotlinString(pointerName: String, vtableIndex: Int): CodeBlock {
        return HStringSupport.toKotlinString(pointerName, vtableIndex)
    }

    private fun scalarRuntimePropertyPlan(type: String): ScalarRuntimePropertyPlan? {
        return when (PropertyRuleRegistry.getterRuleFamily(type)) {
            RuntimePropertyGetterRuleFamily.OBJECT -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T(%L)",
                    PoetSymbols.inspectableClass,
                    AbiCallCatalog.objectMethod(getterVtableIndex),
                )
            }
            RuntimePropertyGetterRuleFamily.BOOLEAN -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of("%T(%L)", PoetSymbols.winRtBooleanClass, AbiCallCatalog.booleanGetter(getterVtableIndex))
            }
            RuntimePropertyGetterRuleFamily.GUID -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T.parse(%L.toString())",
                    PoetSymbols.guidValueClass,
                    AbiCallCatalog.guidGetter(getterVtableIndex),
                )
            }
            RuntimePropertyGetterRuleFamily.DATE_TIME -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                val ticksOffsetLiteral = WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET.toString()
                CodeBlock.of(
                    "Instant.fromEpochSeconds((PlatformComInterop.invokeInt64Getter(pointer, $getterVtableIndex).getOrThrow() - $ticksOffsetLiteral) / 10000000L, ((PlatformComInterop.invokeInt64Getter(pointer, $getterVtableIndex).getOrThrow() - $ticksOffsetLiteral) %% 10000000L * 100).toInt())",
                )
            }
            RuntimePropertyGetterRuleFamily.TIME_SPAN -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T(%L)",
                    PoetSymbols.timeSpanClass,
                    AbiCallCatalog.int64Getter(getterVtableIndex),
                )
            }
            RuntimePropertyGetterRuleFamily.EVENT_REGISTRATION_TOKEN -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T(%L)",
                    PoetSymbols.eventRegistrationTokenClass,
                    AbiCallCatalog.int64Getter(getterVtableIndex),
                )
            }
            RuntimePropertyGetterRuleFamily.STRING -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                hStringToKotlinString("pointer", getterVtableIndex)
            }
            RuntimePropertyGetterRuleFamily.UINT8,
            RuntimePropertyGetterRuleFamily.INT16,
            RuntimePropertyGetterRuleFamily.UINT16,
            RuntimePropertyGetterRuleFamily.CHAR16,
            -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                valueTypeProjectionSupport.smallScalarAbiCall(type, getterVtableIndex, emptyList())
                    ?: error("Unsupported small scalar property type: $type")
            }
            RuntimePropertyGetterRuleFamily.FLOAT32 -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T(%L)",
                    PoetSymbols.float32Class,
                    AbiCallCatalog.float32Method(getterVtableIndex),
                )
            }
            RuntimePropertyGetterRuleFamily.FLOAT64 -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T(%L)",
                    PoetSymbols.float64Class,
                    AbiCallCatalog.float64Method(getterVtableIndex),
                )
            }
            RuntimePropertyGetterRuleFamily.INT32 -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T(%L)",
                    PoetSymbols.int32Class,
                    AbiCallCatalog.int32Method(getterVtableIndex),
                )
            }
            RuntimePropertyGetterRuleFamily.UINT32 -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T(%L)",
                    PoetSymbols.uint32Class,
                    AbiCallCatalog.uint32Method(getterVtableIndex),
                )
            }
            RuntimePropertyGetterRuleFamily.INT64 -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T(%L)",
                    PoetSymbols.int64Class,
                    AbiCallCatalog.int64Getter(getterVtableIndex),
                )
            }
            RuntimePropertyGetterRuleFamily.UINT64 -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T(%L.toULong())",
                    PoetSymbols.uint64Class,
                    AbiCallCatalog.int64Getter(getterVtableIndex),
                )
            }
            else -> null
        }
    }

    private fun runtimePropertyPlan(property: WinMdProperty, currentNamespace: String): RuntimePropertyPlan? {
        val iReferenceInnerType = iReferenceInnerType(property.type)
        val supportsNullableValueReference = supportsIReferenceValueProjection(property.type, currentNamespace, typeRegistry)
        val structPropertyType = typeNameMapper.mapTypeName(property.type, currentNamespace)
            .takeIf { typeRegistry.isStructType(property.type, currentNamespace) }
        val enumType = typeNameMapper.mapTypeName(property.type, currentNamespace)
            .takeIf { typeRegistry.isEnumType(property.type, currentNamespace) }
        val objectPropertyType = supportsProjectedObjectTypeName(property.type)
            .takeIf { it && structPropertyType == null && !supportsNullableValueReference }
            ?.let { typeNameMapper.mapTypeName(property.type, currentNamespace) }
        val getterPlan = when {
            property.getterVtableIndex == null -> null
            structPropertyType != null -> RuntimePropertyGetterPlan { getterVtableIndex ->
                valueTypeProjectionSupport.structReturnExpression(
                    type = property.type,
                    currentNamespace = currentNamespace,
                    abiCall = valueTypeProjectionSupport.invokeStructMethodWithArgs(
                        vtableIndex = getterVtableIndex,
                        structType = structPropertyType,
                        arguments = emptyList(),
                    ),
                )
            }
            iReferenceInnerType != null -> RuntimePropertyGetterPlan { getterVtableIndex ->
                val valueGetter = when {
                    supportsNullableValueReference -> valueTypeProjectionSupport.nullableValueReturnExpression(
                        referenceType = property.type,
                        currentNamespace = currentNamespace,
                        abiCall = AbiCallCatalog.objectMethod(getterVtableIndex),
                    ) ?: error("Unsupported IReference projection type: $iReferenceInnerType")
                    supportsGenericIReferenceStructProjection(property.type, currentNamespace, typeRegistry) ->
                        valueTypeProjectionSupport.genericStructReferenceReturnExpression(
                            referenceType = property.type,
                            currentNamespace = currentNamespace,
                            abiCall = AbiCallCatalog.objectMethod(getterVtableIndex),
                        ) ?: error("Unsupported IReference projection type: $iReferenceInnerType")
                    supportsGenericIReferenceEnumProjection(property.type, currentNamespace, typeRegistry) ->
                        valueTypeProjectionSupport.genericEnumReferenceReturnExpression(
                            referenceType = property.type,
                            currentNamespace = currentNamespace,
                            abiCall = AbiCallCatalog.objectMethod(getterVtableIndex),
                        ) ?: error("Unsupported IReference projection type: $iReferenceInnerType")
                    iReferenceInnerType == "Object" || canonicalWinRtSpecialType(iReferenceInnerType) == "Object" -> CodeBlock.of(
                        "%T.invokeObjectMethod(pointer, %L).getOrThrow().let { if (it.isNull) null else %T(it) }",
                        PoetSymbols.platformComInteropClass,
                        getterVtableIndex,
                        PoetSymbols.inspectableClass,
                    )
                    iReferenceInnerType == "String" || canonicalWinRtSpecialType(iReferenceInnerType) == "String" ->
                        HStringSupport.nullableFromCall(AbiCallCatalog.hstringMethod(getterVtableIndex))
                    else -> scalarRuntimePropertyPlan(canonicalWinRtSpecialType(iReferenceInnerType))?.renderGetter(getterVtableIndex)
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
            else -> when {
                else -> scalarRuntimePropertyPlan(property.type)?.let { scalarPlan ->
                RuntimePropertyGetterPlan { getterVtableIndex -> scalarPlan.renderGetter(getterVtableIndex) }
                }
            }
        }
        val setterPlan = when {
            structPropertyType != null -> RuntimePropertySetterPlan(
                statement = "%L",
                args = { setterVtableIndex ->
                    arrayOf(
                        valueTypeProjectionSupport.invokeUnitMethodWithArgs(
                            vtableIndex = setterVtableIndex,
                            arguments = listOf(CodeBlock.of("value.toAbi()")),
                        ),
                    )
                },
            )
            supportsNullableValueReference -> RuntimePropertySetterPlan(
                statement = "%L",
                args = { setterVtableIndex ->
                    arrayOf(
                        AbiCallCatalog.objectSetterExpression(
                            setterVtableIndex,
                            valueTypeProjectionSupport.nullableValuePointerExpression(
                                property.type,
                                currentNamespace,
                                "value",
                            ) ?: error("Unsupported IReference projection type: ${property.type}"),
                        ),
                    )
                },
            )
            supportsGenericIReferenceStructProjection(property.type, currentNamespace, typeRegistry) -> RuntimePropertySetterPlan(
                statement = "%L",
                args = { setterVtableIndex ->
                    arrayOf(
                        AbiCallCatalog.objectSetterExpression(
                            setterVtableIndex,
                            valueTypeProjectionSupport.genericStructReferencePointerExpression(
                                property.type,
                                currentNamespace,
                                "value",
                            ) ?: error("Unsupported IReference projection type: ${property.type}"),
                        ),
                    )
                },
            )
            supportsGenericIReferenceEnumProjection(property.type, currentNamespace, typeRegistry) -> RuntimePropertySetterPlan(
                statement = "%L",
                args = { setterVtableIndex ->
                    arrayOf(
                        AbiCallCatalog.objectSetterExpression(
                            setterVtableIndex,
                            valueTypeProjectionSupport.genericEnumReferencePointerExpression(
                                property.type,
                                currentNamespace,
                                "value",
                            ) ?: error("Unsupported IReference projection type: ${property.type}"),
                        ),
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
            else -> when (PropertyRuleRegistry.setterRuleFamily(property.type)) {
                null -> null
                RuntimePropertySetterRuleFamily.OBJECT -> RuntimePropertySetterPlan(
                    statement = "%L",
                    args = { setterVtableIndex -> arrayOf(AbiCallCatalog.objectSetter(setterVtableIndex, "value")) },
                )
                RuntimePropertySetterRuleFamily.STRING -> RuntimePropertySetterPlan(
                    statement = "%L",
                    args = { setterVtableIndex -> arrayOf(AbiCallCatalog.stringSetter(setterVtableIndex)) },
                )
                RuntimePropertySetterRuleFamily.UINT8,
                RuntimePropertySetterRuleFamily.INT16,
                RuntimePropertySetterRuleFamily.UINT16,
                RuntimePropertySetterRuleFamily.CHAR16,
                -> RuntimePropertySetterPlan(
                    statement = "%L",
                    args = { setterVtableIndex ->
                        arrayOf(
                            valueTypeProjectionSupport.invokeUnitMethodWithArgs(
                                vtableIndex = setterVtableIndex,
                                arguments = listOf(CodeBlock.of("value")),
                            ),
                        )
                    },
                )
                RuntimePropertySetterRuleFamily.INT32 -> RuntimePropertySetterPlan(
                    statement = "%L",
                    args = { setterVtableIndex -> arrayOf(AbiCallCatalog.int32Setter(setterVtableIndex)) },
                )
                RuntimePropertySetterRuleFamily.UINT32 -> RuntimePropertySetterPlan(
                    statement = "%L",
                    args = { setterVtableIndex -> arrayOf(AbiCallCatalog.uint32Setter(setterVtableIndex)) },
                )
                RuntimePropertySetterRuleFamily.FLOAT32 -> RuntimePropertySetterPlan(
                    statement = "%L",
                    args = { setterVtableIndex -> arrayOf(AbiCallCatalog.float32Setter(setterVtableIndex)) },
                )
                RuntimePropertySetterRuleFamily.BOOLEAN -> RuntimePropertySetterPlan(
                    statement = "%L",
                    args = { setterVtableIndex -> arrayOf(AbiCallCatalog.booleanSetter(setterVtableIndex)) },
                )
                RuntimePropertySetterRuleFamily.FLOAT64 -> RuntimePropertySetterPlan(
                    statement = "%L",
                    args = { setterVtableIndex -> arrayOf(AbiCallCatalog.float64Setter(setterVtableIndex)) },
                )
                RuntimePropertySetterRuleFamily.INT64 -> RuntimePropertySetterPlan(
                    statement = "%L",
                    args = { setterVtableIndex -> arrayOf(AbiCallCatalog.int64Setter(setterVtableIndex)) },
                )
                RuntimePropertySetterRuleFamily.UINT64 -> RuntimePropertySetterPlan(
                    statement = "%L",
                    args = { setterVtableIndex -> arrayOf(AbiCallCatalog.uint64Setter(setterVtableIndex)) },
                )
            }
        }
        return if (getterPlan != null || setterPlan != null) {
            RuntimePropertyPlan(getter = getterPlan, setter = setterPlan)
        } else {
            null
        }
    }

    private data class ScalarRuntimePropertyPlan(
        val renderGetter: (Int) -> CodeBlock,
    )

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
