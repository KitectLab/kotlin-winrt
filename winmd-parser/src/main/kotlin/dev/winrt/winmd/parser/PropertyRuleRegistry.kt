package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.CodeBlock

internal data class ScalarPropertyGetterDescriptor(
    val render: (type: String, getterVtableIndex: Int, valueTypeProjectionSupport: ValueTypeProjectionSupport) -> CodeBlock?,
)

internal data class ScalarPropertySetterDescriptor(
    val render: (setterVtableIndex: Int, valueTypeProjectionSupport: ValueTypeProjectionSupport) -> CodeBlock,
)

internal object PropertyRuleRegistry {
    private val smallScalarGetterDescriptor = ScalarPropertyGetterDescriptor { type, getterVtableIndex, valueTypeProjectionSupport ->
        valueTypeProjectionSupport.smallScalarAbiCall(type, getterVtableIndex, emptyList())
    }

    private val smallScalarSetterDescriptor = ScalarPropertySetterDescriptor { setterVtableIndex, valueTypeProjectionSupport ->
        valueTypeProjectionSupport.invokeUnitMethodWithArgs(
            vtableIndex = setterVtableIndex,
            arguments = listOf(CodeBlock.of("value")),
        )
    }

    private val runtimeGetterDescriptors = mapOf(
        "Object" to ScalarPropertyGetterDescriptor { _, getterVtableIndex, _ ->
            CodeBlock.of(
                "%T(%L)",
                PoetSymbols.inspectableClass,
                AbiCallCatalog.objectMethod(getterVtableIndex),
            )
        },
        "String" to ScalarPropertyGetterDescriptor { _, getterVtableIndex, _ ->
            HStringSupport.toKotlinString("pointer", getterVtableIndex)
        },
        "UInt8" to smallScalarGetterDescriptor,
        "Int16" to smallScalarGetterDescriptor,
        "UInt16" to smallScalarGetterDescriptor,
        "Char16" to smallScalarGetterDescriptor,
        "Float32" to ScalarPropertyGetterDescriptor { _, getterVtableIndex, _ ->
            CodeBlock.of(
                "%T(%L)",
                PoetSymbols.float32Class,
                AbiCallCatalog.float32Method(getterVtableIndex),
            )
        },
        "Float64" to ScalarPropertyGetterDescriptor { _, getterVtableIndex, _ ->
            CodeBlock.of(
                "%T(%L)",
                PoetSymbols.float64Class,
                AbiCallCatalog.float64Method(getterVtableIndex),
            )
        },
        "Boolean" to ScalarPropertyGetterDescriptor { _, getterVtableIndex, _ ->
            CodeBlock.of("%T(%L)", PoetSymbols.winRtBooleanClass, AbiCallCatalog.booleanGetter(getterVtableIndex))
        },
        "Guid" to ScalarPropertyGetterDescriptor { _, getterVtableIndex, _ ->
            CodeBlock.of(
                "%T.parse(%L.toString())",
                PoetSymbols.guidValueClass,
                AbiCallCatalog.guidGetter(getterVtableIndex),
            )
        },
        "DateTime" to ScalarPropertyGetterDescriptor { _, getterVtableIndex, _ ->
            val ticksOffsetLiteral = WINDOWS_FOUNDATION_DATE_TIME_TICKS_OFFSET.toString()
            CodeBlock.of(
                "Instant.fromEpochSeconds((PlatformComInterop.invokeInt64Getter(pointer, $getterVtableIndex).getOrThrow() - $ticksOffsetLiteral) / 10000000L, ((PlatformComInterop.invokeInt64Getter(pointer, $getterVtableIndex).getOrThrow() - $ticksOffsetLiteral) %% 10000000L * 100).toInt())",
            )
        },
        "TimeSpan" to ScalarPropertyGetterDescriptor { _, getterVtableIndex, _ ->
            CodeBlock.of(
                "%T(%L)",
                PoetSymbols.timeSpanClass,
                AbiCallCatalog.int64Getter(getterVtableIndex),
            )
        },
        "EventRegistrationToken" to ScalarPropertyGetterDescriptor { _, getterVtableIndex, _ ->
            CodeBlock.of(
                "%T(%L)",
                PoetSymbols.eventRegistrationTokenClass,
                AbiCallCatalog.int64Getter(getterVtableIndex),
            )
        },
        "HResult" to ScalarPropertyGetterDescriptor { _, getterVtableIndex, _ ->
            CodeBlock.of(
                "%M(%L)",
                PoetSymbols.exceptionFromHResultMember,
                AbiCallCatalog.int32Method(getterVtableIndex),
            )
        },
        "Int32" to ScalarPropertyGetterDescriptor { _, getterVtableIndex, _ ->
            CodeBlock.of(
                "%T(%L)",
                PoetSymbols.int32Class,
                AbiCallCatalog.int32Method(getterVtableIndex),
            )
        },
        "UInt32" to ScalarPropertyGetterDescriptor { _, getterVtableIndex, _ ->
            CodeBlock.of(
                "%T(%L)",
                PoetSymbols.uint32Class,
                AbiCallCatalog.uint32Method(getterVtableIndex),
            )
        },
        "Int64" to ScalarPropertyGetterDescriptor { _, getterVtableIndex, _ ->
            CodeBlock.of(
                "%T(%L)",
                PoetSymbols.int64Class,
                AbiCallCatalog.int64Getter(getterVtableIndex),
            )
        },
        "UInt64" to ScalarPropertyGetterDescriptor { _, getterVtableIndex, _ ->
            CodeBlock.of(
                "%T(%L.toULong())",
                PoetSymbols.uint64Class,
                AbiCallCatalog.int64Getter(getterVtableIndex),
            )
        },
    )

    private val runtimeSetterDescriptors = mapOf(
        "Object" to ScalarPropertySetterDescriptor { setterVtableIndex, _ ->
            AbiCallCatalog.objectSetter(setterVtableIndex, "value")
        },
        "String" to ScalarPropertySetterDescriptor { setterVtableIndex, _ ->
            AbiCallCatalog.stringSetter(setterVtableIndex)
        },
        "UInt8" to smallScalarSetterDescriptor,
        "Int16" to smallScalarSetterDescriptor,
        "UInt16" to smallScalarSetterDescriptor,
        "Char16" to smallScalarSetterDescriptor,
        "HResult" to ScalarPropertySetterDescriptor { setterVtableIndex, _ ->
            AbiCallCatalog.int32SetterExpression(
                setterVtableIndex,
                "hResultOfException(value)",
            )
        },
        "Int32" to ScalarPropertySetterDescriptor { setterVtableIndex, _ ->
            AbiCallCatalog.int32Setter(setterVtableIndex)
        },
        "UInt32" to ScalarPropertySetterDescriptor { setterVtableIndex, _ ->
            AbiCallCatalog.uint32Setter(setterVtableIndex)
        },
        "Float32" to ScalarPropertySetterDescriptor { setterVtableIndex, _ ->
            AbiCallCatalog.float32Setter(setterVtableIndex)
        },
        "Boolean" to ScalarPropertySetterDescriptor { setterVtableIndex, _ ->
            AbiCallCatalog.booleanSetter(setterVtableIndex)
        },
        "Float64" to ScalarPropertySetterDescriptor { setterVtableIndex, _ ->
            AbiCallCatalog.float64Setter(setterVtableIndex)
        },
        "Int64" to ScalarPropertySetterDescriptor { setterVtableIndex, _ ->
            AbiCallCatalog.int64Setter(setterVtableIndex)
        },
        "UInt64" to ScalarPropertySetterDescriptor { setterVtableIndex, _ ->
            AbiCallCatalog.uint64Setter(setterVtableIndex)
        },
    )

    fun runtimeGetterDescriptor(type: String): ScalarPropertyGetterDescriptor? =
        runtimeGetterDescriptors[canonicalWinRtSpecialType(type)]

    fun runtimeSetterDescriptor(type: String): ScalarPropertySetterDescriptor? =
        runtimeSetterDescriptors[canonicalWinRtSpecialType(type)]

    fun interfaceScalarGetterDescriptor(type: String): ScalarPropertyGetterDescriptor? =
        runtimeGetterDescriptors[canonicalWinRtSpecialType(type)]
            ?.takeUnless { canonicalWinRtSpecialType(type) == "Object" }

    fun interfaceScalarSetterDescriptor(type: String): ScalarPropertySetterDescriptor? =
        runtimeSetterDescriptors[canonicalWinRtSpecialType(type)]
            ?.takeUnless { canonicalWinRtSpecialType(type) == "Object" }
}
