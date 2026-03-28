package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdType

internal class KotlinCollectionProjectionMapper {
    fun runtimeClassProjection(type: WinMdType): RuntimeCollectionProjection? {
        if (type.namespace == "Windows.Foundation.Collections" && type.name == "StringVectorView") {
            return RuntimeCollectionProjection(
                superinterface = PoetSymbols.listClass.parameterizedBy(String::class.asTypeName()),
                delegateFactory = CodeBlock.of(
                    "%T(sizeProvider = { %T(%T.invokeUInt32Method(pointer, 7).getOrThrow()).value.toInt() }, getter = { index -> val value = %T.invokeHStringMethodWithUInt32Arg(pointer, 6, index.toUInt()).getOrThrow(); try { %T.toKotlin(value) } finally { %T.release(value) } })",
                    PoetSymbols.winRtListProjectionClass.parameterizedBy(String::class.asTypeName()),
                    PoetSymbols.uint32Class,
                    PoetSymbols.platformComInteropClass,
                    PoetSymbols.platformComInteropClass,
                    PoetSymbols.winRtStringsClass,
                    PoetSymbols.winRtStringsClass,
                ),
                winRtSizeSlot = 7,
                extraFunctions = listOf(
                    FunSpec.builder("getAt")
                        .addParameter("index", PoetSymbols.uint32Class)
                        .returns(String::class)
                        .addStatement(
                            "val value = %T.invokeHStringMethodWithUInt32Arg(pointer, 6, index.value).getOrThrow()",
                            PoetSymbols.platformComInteropClass,
                        )
                        .beginControlFlow("return try")
                        .addStatement("%T.toKotlin(value)", PoetSymbols.winRtStringsClass)
                        .nextControlFlow("finally")
                        .addStatement("%T.release(value)", PoetSymbols.winRtStringsClass)
                        .endControlFlow()
                        .build(),
                ),
            )
        }
        return null
    }

    fun interfaceProjection(type: WinMdType): InterfaceCollectionProjection? {
        if (type.namespace == "Microsoft.UI.Xaml.Interop" && type.name == "IBindableVector") {
            return InterfaceCollectionProjection(
                superinterface = PoetSymbols.mutableListClass.parameterizedBy(PoetSymbols.inspectableClass),
                delegateFactory = CodeBlock.of(
                    "%T(sizeProvider = { %T(%T.invokeUInt32Method(pointer, 8).getOrThrow()).value.toInt() }, getter = { index -> %T(%T.invokeObjectMethodWithUInt32Arg(pointer, 7, index.toUInt()).getOrThrow()) }, append = { value -> %T.invokeObjectSetter(pointer, 14, value.pointer).getOrThrow() }, clearer = { %T.invokeUnitMethod(pointer, 16).getOrThrow() })",
                    PoetSymbols.winRtMutableListProjectionClass.parameterizedBy(PoetSymbols.inspectableClass),
                    PoetSymbols.uint32Class,
                    PoetSymbols.platformComInteropClass,
                    PoetSymbols.inspectableClass,
                    PoetSymbols.platformComInteropClass,
                    PoetSymbols.platformComInteropClass,
                    PoetSymbols.platformComInteropClass,
                ),
                winRtSizeSlot = 8,
            )
        }
        if (type.namespace == "Microsoft.UI.Xaml.Interop" && type.name == "IBindableVectorView") {
            return InterfaceCollectionProjection(
                superinterface = PoetSymbols.listClass.parameterizedBy(PoetSymbols.inspectableClass),
                delegateFactory = CodeBlock.of(
                    "%T(sizeProvider = { %T(%T.invokeUInt32Method(pointer, 8).getOrThrow()).value.toInt() }, getter = { index -> %T(%T.invokeObjectMethodWithUInt32Arg(pointer, 7, index.toUInt()).getOrThrow()) })",
                    PoetSymbols.winRtListProjectionClass.parameterizedBy(PoetSymbols.inspectableClass),
                    PoetSymbols.uint32Class,
                    PoetSymbols.platformComInteropClass,
                    PoetSymbols.inspectableClass,
                    PoetSymbols.platformComInteropClass,
                ),
                winRtSizeSlot = 8,
            )
        }
        return null
    }

    fun buildWinRtSizeProperty(slot: Int): PropertySpec {
        return PropertySpec.builder("winRtSize", PoetSymbols.uint32Class)
            .getter(
                FunSpec.getterBuilder()
                    .addStatement(
                        "return %T(%T.invokeUInt32Method(pointer, %L).getOrThrow())",
                        PoetSymbols.uint32Class,
                        PoetSymbols.platformComInteropClass,
                        slot,
                    )
                    .build(),
            )
            .build()
    }
}

internal data class RuntimeCollectionProjection(
    val superinterface: TypeName,
    val delegateFactory: CodeBlock,
    val winRtSizeSlot: Int,
    val extraFunctions: List<FunSpec> = emptyList(),
)

internal data class InterfaceCollectionProjection(
    val superinterface: TypeName,
    val delegateFactory: CodeBlock,
    val winRtSizeSlot: Int,
)
