package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdActivationKind
import dev.winrt.winmd.plugin.WinMdProperty
import dev.winrt.winmd.plugin.WinMdType

internal class RuntimeTypeRenderer(
    private val typeNameMapper: TypeNameMapper,
) {
    fun render(type: WinMdType): TypeSpec {
        return when (type.kind) {
            dev.winrt.winmd.plugin.WinMdTypeKind.RuntimeClass -> renderRuntimeClass(type)
            dev.winrt.winmd.plugin.WinMdTypeKind.Struct -> renderStruct(type)
            dev.winrt.winmd.plugin.WinMdTypeKind.Enum -> renderEnum(type)
            else -> error("Unsupported type kind for runtime renderer: ${type.kind}")
        }
    }

    private fun renderRuntimeClass(type: WinMdType): TypeSpec {
        val builder = TypeSpec.classBuilder(type.name)
            .addModifiers(KModifier.OPEN)
            .primaryConstructor(pointerConstructor())
            .superclass(PoetSymbols.inspectableClass)
            .addSuperclassConstructorParameter("pointer")

        type.properties.forEach { property ->
            builder.addProperty(renderBackingProperty(property, type.namespace))
            builder.addProperty(renderRuntimeProperty(property, type.namespace))
        }
        type.methods.forEach { builder.addFunction(renderRuntimeMethod(it, type.namespace)) }
        builder.addType(renderRuntimeClassCompanion(type))
        type.defaultInterface?.let { defaultInterface ->
            val simpleName = defaultInterface.substringAfterLast('.')
            builder.addFunction(
                FunSpec.builder("as$simpleName")
                    .returns(ClassName(defaultInterface.substringBeforeLast('.').lowercase(), simpleName))
                    .addStatement("return %L.from(this)", simpleName)
                    .build(),
            )
        }
        return builder.build()
    }

    private fun renderRuntimeClassCompanion(type: WinMdType): TypeSpec {
        val typeClass = ClassName(type.namespace.lowercase(), type.name)
        return TypeSpec.companionObjectBuilder()
            .addSuperinterface(PoetSymbols.winRtRuntimeClassMetadataClass)
            .addProperty(overrideStringProperty("qualifiedName", "${type.namespace}.${type.name}"))
            .addProperty(
                PropertySpec.builder("classId", PoetSymbols.runtimeClassIdClass)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("%T(%S, %S)", PoetSymbols.runtimeClassIdClass, type.namespace, type.name)
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("defaultInterfaceName", String::class.asTypeName().copy(nullable = true))
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer(type.defaultInterface?.let { CodeBlock.of("%S", it) } ?: CodeBlock.of("null"))
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("activationKind", PoetSymbols.winRtActivationKindClass)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("%T.%L", PoetSymbols.winRtActivationKindClass, activationKindLiteral(type.activationKind))
                    .build(),
            )
            .addFunction(
                FunSpec.builder(type.activationFunctionName)
                    .returns(typeClass)
                    .addStatement("return %T.activate(this, ::%L)", PoetSymbols.winRtRuntimeClass, type.name)
                    .build(),
            )
            .build()
    }

    private fun renderRuntimeMethod(method: dev.winrt.winmd.plugin.WinMdMethod, currentNamespace: String): FunSpec {
        val functionName = method.name.replaceFirstChar(Char::lowercase)
        val kotlinType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
        val builder = FunSpec.builder(functionName).returns(kotlinType)

        if (method.returnType == "Unit" && method.parameters.isEmpty() && method.vtableIndex != null) {
            val vtableIndex = method.vtableIndex!!
            return builder
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("return")
                .endControlFlow()
                .addStatement("%T.invokeUnitMethod(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, vtableIndex)
                .build()
        }
        if (method.returnType == "UInt32" && method.parameters.isEmpty() && method.vtableIndex != null) {
            val vtableIndex = method.vtableIndex!!
            return builder
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("return %T(0u)", PoetSymbols.uint32Class)
                .endControlFlow()
                .addStatement("return %T(%T.invokeUInt32Method(pointer, %L).getOrThrow())", PoetSymbols.uint32Class, PoetSymbols.platformComInteropClass, vtableIndex)
                .build()
        }

        return builder
            .addStatement("return %L", typeNameMapper.defaultValueFor(kotlinType, functionName))
            .build()
    }

    private fun renderBackingProperty(property: WinMdProperty, currentNamespace: String): PropertySpec {
        val kotlinType = typeNameMapper.mapTypeName(property.type, currentNamespace)
        return PropertySpec.builder("backing_${property.name}", PoetSymbols.runtimePropertyClass.parameterizedBy(kotlinType))
            .addModifiers(KModifier.PRIVATE)
            .initializer("%T(%L)", PoetSymbols.runtimePropertyClass.parameterizedBy(kotlinType), typeNameMapper.defaultValueFor(kotlinType))
            .build()
    }

    private fun renderRuntimeProperty(property: WinMdProperty, currentNamespace: String): PropertySpec {
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
                .addStatement("val value = %T.invokeHStringMethod(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, getterVtableIndex)
                .beginControlFlow("return try")
                .addStatement("%T(%T.toKotlin(value))", PoetSymbols.iReferenceClass.parameterizedBy(String::class.asTypeName()), PoetSymbols.winRtStringsClass)
                .nextControlFlow("finally")
                .addStatement("%T.release(value)", PoetSymbols.winRtStringsClass)
                .endControlFlow()
                .build()
            }
            property.type == "String" && property.getterVtableIndex != null -> {
                val getterVtableIndex = property.getterVtableIndex!!
                builder
                .beginControlFlow("if (pointer.isNull)")
                .addStatement("return %N.get()", backingName)
                .endControlFlow()
                .addStatement("val value = %T.invokeHStringMethod(pointer, %L).getOrThrow()", PoetSymbols.platformComInteropClass, getterVtableIndex)
                .beginControlFlow("return try")
                .addStatement("%T.toKotlin(value)", PoetSymbols.winRtStringsClass)
                .nextControlFlow("finally")
                .addStatement("%T.release(value)", PoetSymbols.winRtStringsClass)
                .endControlFlow()
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
        } else {
            builder
                .addStatement("%N.set(value)", backingName)
                .build()
        }
    }

    private fun renderStruct(type: WinMdType): TypeSpec {
        return TypeSpec.classBuilder(type.name)
            .addModifiers(KModifier.DATA)
            .primaryConstructor(
                FunSpec.constructorBuilder().apply {
                    type.fields.forEach { field ->
                        addParameter(field.name.replaceFirstChar(Char::lowercase), typeNameMapper.mapTypeName(field.type, type.namespace))
                    }
                }.build(),
            )
            .apply {
                type.fields.forEach { field ->
                    addProperty(
                        PropertySpec.builder(field.name.replaceFirstChar(Char::lowercase), typeNameMapper.mapTypeName(field.type, type.namespace))
                            .initializer(field.name.replaceFirstChar(Char::lowercase))
                            .build(),
                    )
                }
            }
            .build()
    }

    private fun renderEnum(type: WinMdType): TypeSpec {
        return TypeSpec.enumBuilder(type.name)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("value", Int::class)
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("value", Int::class)
                    .initializer("value")
                    .build(),
            )
            .apply {
                type.enumMembers.forEach { member ->
                    addEnumConstant(
                        member.name,
                        TypeSpec.anonymousClassBuilder()
                            .addSuperclassConstructorParameter("%L", member.value)
                            .build(),
                    )
                }
            }
            .build()
    }

    private fun activationKindLiteral(kind: WinMdActivationKind): String {
        return when (kind) {
            WinMdActivationKind.Factory -> "Factory"
        }
    }
}
