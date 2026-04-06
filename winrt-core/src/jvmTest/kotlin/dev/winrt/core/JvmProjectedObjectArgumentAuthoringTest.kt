package dev.winrt.core

import dev.winrt.kom.ComPtr
import dev.winrt.kom.KnownHResults
import dev.winrt.kom.PlatformComInterop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JvmProjectedObjectArgumentAuthoringTest {
    private enum class ExampleMode(
        val value: UInt,
    ) {
        Alpha(1u),
        Beta(2u),
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_iterable_string_values_on_jvm() {
        val pointer = projectedObjectArgumentPointer(
            value = listOf("en-US", "fr-FR"),
            projectionTypeKey = "kotlin.collections.Iterable<String>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "faa585ea-6214-4217-afda-7f46de5869b3",
                WinRtTypeSignature.string(),
            ),
        )

        assertFalse(pointer.isNull)

        val iterator = PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow()
        try {
            PlatformComInterop.invokeHStringMethod(iterator, 6).getOrThrow().use { current ->
                assertEquals("en-US", current.toKotlinString())
            }
            assertTrue(PlatformComInterop.invokeBooleanGetter(iterator, 7).getOrThrow())
            assertTrue(PlatformComInterop.invokeBooleanGetter(iterator, 8).getOrThrow())
            PlatformComInterop.invokeHStringMethod(iterator, 6).getOrThrow().use { current ->
                assertEquals("fr-FR", current.toKotlinString())
            }
            assertFalse(PlatformComInterop.invokeBooleanGetter(iterator, 8).getOrThrow())
            assertFalse(PlatformComInterop.invokeBooleanGetter(iterator, 7).getOrThrow())
        } finally {
            PlatformComInterop.release(iterator)
        }
    }

    @Test
    fun projected_object_argument_pointer_reuses_plain_iterable_stub_for_same_object() {
        val languages = listOf("en-US", "fr-FR")

        val first = projectedObjectArgumentPointer(
            value = languages,
            projectionTypeKey = "kotlin.collections.Iterable<String>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "faa585ea-6214-4217-afda-7f46de5869b3",
                WinRtTypeSignature.string(),
            ),
        )
        val second = projectedObjectArgumentPointer(
            value = languages,
            projectionTypeKey = "kotlin.collections.Iterable<String>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "faa585ea-6214-4217-afda-7f46de5869b3",
                WinRtTypeSignature.string(),
            ),
        )

        assertEquals(first, second)
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_map_values_on_jvm() {
        val forwardedIid = guidOf("12345678-1111-2222-3333-444444444444")

        JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(iid = forwardedIid),
        ).use { valueStub ->
            val value = Inspectable(valueStub.primaryPointer)
            val pointer = projectedObjectArgumentPointer(
                value = linkedMapOf("theme" to value),
                projectionTypeKey = "kotlin.collections.Map<String, Object>",
                signature = WinRtTypeSignature.parameterizedInterface(
                    "e480ce40-a338-4ada-adcf-272272e48cb9",
                    WinRtTypeSignature.string(),
                    WinRtTypeSignature.object_(),
                ),
            )

            assertFalse(pointer.isNull)
            assertEquals(1u, PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())
            assertTrue(PlatformComInterop.invokeBooleanMethodWithStringArg(pointer, 9, "theme").getOrThrow())
            val iterablePointer = PlatformComInterop.queryInterface(
                pointer,
                ParameterizedInterfaceId.createFromSignature(
                    WinRtTypeSignature.parameterizedInterface(
                        "faa585ea-6214-4217-afda-7f46de5869b3",
                        WinRtTypeSignature.parameterizedInterface(
                            "02b51929-c1c4-4a7e-8940-0312b5c18500",
                            WinRtTypeSignature.string(),
                            WinRtTypeSignature.object_(),
                        ),
                    ),
                ),
            ).getOrThrow()
            PlatformComInterop.release(iterablePointer)

            val lookup = PlatformComInterop.invokeObjectMethodWithStringArg(pointer, 7, "theme").getOrThrow()
            try {
                assertEquals(valueStub.primaryPointer.value.rawValue, lookup.value.rawValue)
            } finally {
                PlatformComInterop.release(lookup)
            }

            val iterator = PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow()
            try {
                val current = PlatformComInterop.invokeObjectMethod(iterator, 6).getOrThrow()
                try {
                    PlatformComInterop.invokeHStringMethod(current, 6).getOrThrow().use { key ->
                        assertEquals("theme", key.toKotlinString())
                    }
                    val currentValue = PlatformComInterop.invokeObjectMethod(current, 7).getOrThrow()
                    try {
                        assertEquals(valueStub.primaryPointer.value.rawValue, currentValue.value.rawValue)
                    } finally {
                        PlatformComInterop.release(currentValue)
                    }
                } finally {
                    PlatformComInterop.release(current)
                }
            } finally {
                PlatformComInterop.release(iterator)
            }
        }
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_list_values_for_vector_view_on_jvm() {
        val pointer = projectedObjectArgumentPointer(
            value = listOf("en-US", "fr-FR"),
            projectionTypeKey = "kotlin.collections.List<String>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "bbe1fa4c-b0e3-4583-baef-1f1b2e483e56",
                WinRtTypeSignature.string(),
            ),
        )

        assertFalse(pointer.isNull)
        assertEquals(2u, PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())
        PlatformComInterop.invokeHStringMethodWithUInt32Arg(pointer, 7, 1u).getOrThrow().use { value ->
            assertEquals("fr-FR", value.toKotlinString())
        }

        val iterablePointer = PlatformComInterop.queryInterface(
            pointer,
            ParameterizedInterfaceId.createFromSignature(
                WinRtTypeSignature.parameterizedInterface(
                    "faa585ea-6214-4217-afda-7f46de5869b3",
                    WinRtTypeSignature.string(),
                ),
            ),
        ).getOrThrow()
        PlatformComInterop.release(iterablePointer)

        val iterator = PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow()
        try {
            PlatformComInterop.invokeHStringMethod(iterator, 6).getOrThrow().use { current ->
                assertEquals("en-US", current.toKotlinString())
            }
        } finally {
            PlatformComInterop.release(iterator)
        }
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_ireference_enum_values_on_jvm() {
        val pointer = projectedObjectArgumentPointer(
            value = IReference(ExampleMode.Beta),
            projectionTypeKey = "Windows.Foundation.IReference`1<Test.ExampleMode>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "61c17706-2d65-11e0-9ae8-d48564015472",
                WinRtTypeSignature.enum("Test.ExampleMode", "u4"),
            ),
        )

        assertFalse(pointer.isNull)
        assertEquals(2u, PlatformComInterop.invokeUInt32Method(pointer, 6).getOrThrow())
    }

    @Test
    fun projected_object_argument_pointer_accepts_plain_ireference_hresult_values_on_jvm() {
        val pointer = projectedObjectArgumentPointer(
            value = IReference(IllegalArgumentException()),
            projectionTypeKey = "Windows.Foundation.IReference`1<Exception>",
            signature = WinRtTypeSignature.parameterizedInterface(
                "61c17706-2d65-11e0-9ae8-d48564015472",
                WinRtTypeSignature.struct("Windows.Foundation.HResult", "i4"),
            ),
        )

        assertFalse(pointer.isNull)
        assertEquals(KnownHResults.E_INVALIDARG.value, PlatformComInterop.invokeInt32Method(pointer, 6).getOrThrow())
    }
}
