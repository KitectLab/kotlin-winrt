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
        val supportsGetter = scalarRuntimePropertyPlan(property.type) != null || property.type == "IReference<String>"
        val supportsSetter = property.type == "String" || property.type == "Int32"
        return (supportsGetter && property.getterVtableIndex != null) ||
            (supportsSetter && property.setterVtableIndex != null)
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
        val backingName = "backing_${property.name}"
        val builder = FunSpec.getterBuilder()
        val scalarPlan = scalarRuntimePropertyPlan(property.type)
        if (scalarPlan != null && property.getterVtableIndex != null) {
            val getterVtableIndex = property.getterVtableIndex!!
            return builder
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("return %N.get()", backingName)
                .endControlFlow()
                .addStatement("return %L", scalarPlan.renderGetter(getterVtableIndex))
                .build()
        }
        return when {
            property.type == "IReference<String>" && property.getterVtableIndex != null -> {
                val getterVtableIndex = property.getterVtableIndex!!
                builder
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("return %N.get()", backingName)
                    .endControlFlow()
                    .addStatement("return %T(%L)", PoetSymbols.iReferenceClass.parameterizedBy(String::class.asTypeName()), hStringToKotlinString("pointer", getterVtableIndex))
                    .build()
            }
            property.type == "String" && property.getterVtableIndex != null -> {
                val getterVtableIndex = property.getterVtableIndex!!
                builder
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("return %N.get()", backingName)
                    .endControlFlow()
                    .addStatement("return %L", hStringToKotlinString("pointer", getterVtableIndex))
                    .build()
            }
            property.type == "Int32" && property.getterVtableIndex != null -> {
                val getterVtableIndex = property.getterVtableIndex!!
                builder
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("return %N.get()", backingName)
                    .endControlFlow()
                    .addStatement("return %T(%T.invokeInt32Method(pointer, %L).getOrThrow())", PoetSymbols.int32Class, PoetSymbols.platformComInteropClass, getterVtableIndex)
                    .build()
            }
            else -> builder
                .addStatement("return %N.get()", backingName)
                .build()
        }
    }

    private fun renderRuntimeSetter(property: WinMdProperty, currentNamespace: String): FunSpec {
        val backingName = "backing_${property.name}"
        val builder = FunSpec.setterBuilder()
            .addParameter("value", typeNameMapper.mapTypeName(property.type, currentNamespace))
        return if (property.type == "String" && property.setterVtableIndex != null) {
            val setterVtableIndex = property.setterVtableIndex!!
            builder
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("%N.set(value)", backingName)
                .addStatement("return")
                .endControlFlow()
                .addStatement("%T.invokeStringSetter(pointer, %L, value).getOrThrow()", PoetSymbols.platformComInteropClass, setterVtableIndex)
                .build()
        } else if (property.type == "Int32" && property.setterVtableIndex != null) {
            val setterVtableIndex = property.setterVtableIndex!!
            builder
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("%N.set(value)", backingName)
                .addStatement("return")
                .endControlFlow()
                .addStatement("%T.invokeInt32Setter(pointer, %L, value.value).getOrThrow()", PoetSymbols.platformComInteropClass, setterVtableIndex)
                .build()
        } else {
            builder
                .addStatement("%N.set(value)", backingName)
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

    private data class ScalarRuntimePropertyPlan(
        val renderGetter: (Int) -> CodeBlock,
    )
}
