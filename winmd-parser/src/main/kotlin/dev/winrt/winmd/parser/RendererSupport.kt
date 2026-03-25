package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec

internal fun pointerConstructor(): FunSpec {
    return FunSpec.constructorBuilder()
        .addParameter("pointer", PoetSymbols.comPtrClass)
        .build()
}

internal fun overrideStringProperty(name: String, value: String): PropertySpec {
    return PropertySpec.builder(name, String::class)
        .addModifiers(KModifier.OVERRIDE)
        .initializer("%S", value)
        .build()
}
