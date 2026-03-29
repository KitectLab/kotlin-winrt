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
        return when (type) {
            "Boolean" -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T(%T.invokeBooleanGetter(pointer, %L).getOrThrow())",
                    PoetSymbols.winRtBooleanClass,
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                )
            }
            "Guid" -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T(%T.invokeGuidGetter(pointer, %L).getOrThrow().toString())",
                    PoetSymbols.guidValueClass,
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                )
            }
            "DateTime" -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T(%T.invokeInt64Getter(pointer, %L).getOrThrow())",
                    PoetSymbols.dateTimeClass,
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                )
            }
            "TimeSpan" -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T(%T.invokeInt64Getter(pointer, %L).getOrThrow())",
                    PoetSymbols.timeSpanClass,
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                )
            }
            "EventRegistrationToken" -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T(%T.invokeInt64Getter(pointer, %L).getOrThrow())",
                    PoetSymbols.eventRegistrationTokenClass,
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                )
            }
            "String" -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                hStringToKotlinString("pointer", getterVtableIndex)
            }
            "Int32" -> ScalarRuntimePropertyPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T(%T.invokeInt32Method(pointer, %L).getOrThrow())",
                    PoetSymbols.int32Class,
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                )
            }
            else -> null
        }
    }

    private fun runtimePropertyPlan(property: WinMdProperty): RuntimePropertyPlan? {
        val getterPlan = when {
            property.getterVtableIndex == null -> null
            property.type == "IReference<String>" -> RuntimePropertyGetterPlan { getterVtableIndex ->
                CodeBlock.of(
                    "%T(%L)",
                    PoetSymbols.iReferenceClass.parameterizedBy(String::class.asTypeName()),
                    hStringToKotlinString("pointer", getterVtableIndex),
                )
            }
            else -> scalarRuntimePropertyPlan(property.type)?.let { scalarPlan ->
                RuntimePropertyGetterPlan { getterVtableIndex -> scalarPlan.renderGetter(getterVtableIndex) }
            }
        }
        val setterPlan = when {
            property.setterVtableIndex == null -> null
            property.type == "String" -> RuntimePropertySetterPlan(
                statement = "%T.invokeStringSetter(pointer, %L, value).getOrThrow()",
                args = { setterVtableIndex -> arrayOf(PoetSymbols.platformComInteropClass, setterVtableIndex) },
            )
            property.type == "Int32" -> RuntimePropertySetterPlan(
                statement = "%T.invokeInt32Setter(pointer, %L, value.value).getOrThrow()",
                args = { setterVtableIndex -> arrayOf(PoetSymbols.platformComInteropClass, setterVtableIndex) },
            )
            else -> null
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
