package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdProperty

internal class RuntimePropertyRenderer(
    private val typeNameMapper: TypeNameMapper,
) {
    fun canRenderRuntimeProperty(property: WinMdProperty): Boolean {
        return when (property.type) {
            "Boolean", "Guid", "DateTime", "TimeSpan", "EventRegistrationToken", "IReference<String>", "String", "Int32" ->
                property.getterVtableIndex != null || ((property.type == "String" || property.type == "Int32") && property.setterVtableIndex != null)
            else -> false
        }
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
        return when {
            property.type == "Boolean" && property.getterVtableIndex != null -> {
                val getterVtableIndex = property.getterVtableIndex!!
                builder
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("return %N.get()", backingName)
                    .endControlFlow()
                    .addStatement("return %T(%T.invokeBooleanGetter(pointer, %L).getOrThrow())", PoetSymbols.winRtBooleanClass, PoetSymbols.platformComInteropClass, getterVtableIndex)
                    .build()
            }
            property.type == "Guid" && property.getterVtableIndex != null -> {
                val getterVtableIndex = property.getterVtableIndex!!
                builder
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("return %N.get()", backingName)
                    .endControlFlow()
                    .addStatement("return %T(%T.invokeGuidGetter(pointer, %L).getOrThrow().toString())", PoetSymbols.guidValueClass, PoetSymbols.platformComInteropClass, getterVtableIndex)
                    .build()
            }
            property.type == "DateTime" && property.getterVtableIndex != null -> {
                val getterVtableIndex = property.getterVtableIndex!!
                builder
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("return %N.get()", backingName)
                    .endControlFlow()
                    .addStatement("return %T(%T.invokeInt64Getter(pointer, %L).getOrThrow())", PoetSymbols.dateTimeClass, PoetSymbols.platformComInteropClass, getterVtableIndex)
                    .build()
            }
            property.type == "TimeSpan" && property.getterVtableIndex != null -> {
                val getterVtableIndex = property.getterVtableIndex!!
                builder
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("return %N.get()", backingName)
                    .endControlFlow()
                    .addStatement("return %T(%T.invokeInt64Getter(pointer, %L).getOrThrow())", PoetSymbols.timeSpanClass, PoetSymbols.platformComInteropClass, getterVtableIndex)
                    .build()
            }
            property.type == "EventRegistrationToken" && property.getterVtableIndex != null -> {
                val getterVtableIndex = property.getterVtableIndex!!
                builder
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("return %N.get()", backingName)
                    .endControlFlow()
                    .addStatement("return %T(%T.invokeInt64Getter(pointer, %L).getOrThrow())", PoetSymbols.eventRegistrationTokenClass, PoetSymbols.platformComInteropClass, getterVtableIndex)
                    .build()
            }
            property.type == "IReference<String>" && property.getterVtableIndex != null -> {
                val getterVtableIndex = property.getterVtableIndex!!
                builder
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("return %N.get()", backingName)
                    .endControlFlow()
                    .addStatement("return %T(kotlin.io.use(%T.invokeHStringMethod(pointer, %L).getOrThrow()) { it.toKotlinString() })", PoetSymbols.iReferenceClass.parameterizedBy(String::class.asTypeName()), PoetSymbols.platformComInteropClass, getterVtableIndex)
                    .build()
            }
            property.type == "String" && property.getterVtableIndex != null -> {
                val getterVtableIndex = property.getterVtableIndex!!
                builder
                    .beginControlFlow("if (pointer.isNull)")
                    .addStatement("return %N.get()", backingName)
                    .endControlFlow()
                    .addStatement("return kotlin.io.use(%T.invokeHStringMethod(pointer, %L).getOrThrow()) { it.toKotlinString() }", PoetSymbols.platformComInteropClass, getterVtableIndex)
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
}
