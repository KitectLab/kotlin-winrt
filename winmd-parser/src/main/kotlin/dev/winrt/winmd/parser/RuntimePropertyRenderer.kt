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
) {
    fun canRenderRuntimeProperty(property: WinMdProperty): Boolean {
        return runtimePropertyPlan(property) != null
    }

    fun renderBackingProperty(property: WinMdProperty, currentNamespace: String): PropertySpec {
        val kotlinType = typeNameMapper.mapTypeName(property.type, currentNamespace)
        return PropertySpec.builder("backing_${property.name}", PoetSymbols.runtimePropertyClass.parameterizedBy(kotlinType))
            .addModifiers(KModifier.PRIVATE)
            .initializer("%T(%L)", PoetSymbols.runtimePropertyClass.parameterizedBy(kotlinType), typeNameMapper.defaultValueFor(kotlinType))
            .build()
    }

    fun renderRuntimeProperty(property: WinMdProperty, currentNamespace: String): PropertySpec {
        val propertyName = property.name.replaceFirstChar(Char::lowercase)
        val kotlinType = typeNameMapper.mapTypeName(property.type, currentNamespace)
        val builder = PropertySpec.builder(propertyName, kotlinType)
        if (property.mutable) {
            builder.mutable()
        }
        builder.getter(renderRuntimeGetter(property))
        if (property.mutable) {
            builder.setter(renderRuntimeSetter(property, currentNamespace))
        }
        return builder.build()
    }

    private fun renderRuntimeGetter(property: WinMdProperty): FunSpec {
        val plan = runtimePropertyPlan(property)
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
        val plan = runtimePropertyPlan(property)
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
        return CodeBlock.of(
            "%T.invokeHStringMethod($pointerName, $vtableIndex).getOrThrow().use { it.toKotlinString() }",
            PoetSymbols.platformComInteropClass,
        )
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
                CodeBlock.of(
                    "%T(%L)",
                    PoetSymbols.winRtBooleanClass,
                    AbiCallCatalog.booleanMethod(getterVtableIndex),
                )
            }
            RuntimePropertyGetterRuleFamily.GUID -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T(%L.toString())",
                    PoetSymbols.guidValueClass,
                    AbiCallCatalog.guidGetter(getterVtableIndex),
                )
            }
            RuntimePropertyGetterRuleFamily.DATE_TIME -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T(%L)",
                    PoetSymbols.dateTimeClass,
                    AbiCallCatalog.int64Getter(getterVtableIndex),
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
            RuntimePropertyGetterRuleFamily.FLOAT32 -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T(%L)",
                    PoetSymbols.float32Class,
                    AbiCallCatalog.float32Method(getterVtableIndex),
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

    private fun runtimePropertyPlan(property: WinMdProperty): RuntimePropertyPlan? {
        val iReferenceInnerType = iReferenceInnerType(property.type)
        val getterPlan = when {
            property.getterVtableIndex == null -> null
            iReferenceInnerType != null -> RuntimePropertyGetterPlan { getterVtableIndex ->
                when (iReferenceInnerType) {
                    "String" -> CodeBlock.of(
                        "%T.invokeHStringMethod(pointer, %L).getOrThrow().use { value -> if (value.isNull) null else value.toKotlinString() }",
                        PoetSymbols.platformComInteropClass,
                        getterVtableIndex,
                    )
                    else -> scalarRuntimePropertyPlan(iReferenceInnerType)?.renderGetter(getterVtableIndex)
                        ?: error("Unsupported IReference projection type: $iReferenceInnerType")
                }
            }
            else -> scalarRuntimePropertyPlan(property.type)?.let { scalarPlan ->
                RuntimePropertyGetterPlan { getterVtableIndex -> scalarPlan.renderGetter(getterVtableIndex) }
            }
        }
        val setterPlan = when (PropertyRuleRegistry.setterRuleFamily(property.type)) {
            null -> null
            RuntimePropertySetterRuleFamily.OBJECT -> RuntimePropertySetterPlan(
                statement = "%L",
                args = { setterVtableIndex -> arrayOf(AbiCallCatalog.objectSetter(setterVtableIndex, "value")) },
            )
            RuntimePropertySetterRuleFamily.STRING -> RuntimePropertySetterPlan(
                statement = "%L",
                args = { setterVtableIndex -> arrayOf(AbiCallCatalog.stringSetter(setterVtableIndex)) },
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
        }
        return if (getterPlan != null || setterPlan != null) {
            RuntimePropertyPlan(getter = getterPlan, setter = setterPlan)
        } else {
            null
        }
    }

    private fun iReferenceInnerType(type: String): String? {
        val rawType = type.substringBefore('<').substringAfterLast('.')
        if (rawType != "IReference" || !type.endsWith(">") || '<' !in type) {
            return null
        }
        return type.substringAfter('<').substringBeforeLast('>')
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
