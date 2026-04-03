package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AsyncMethodRuleRegistryTest {
    private val typeNameMapper = TypeNameMapper()
    private val typeRegistry = TypeRegistry(
        WinMdModel(
            files = emptyList(),
            namespaces = listOf(WinMdNamespace(name = "Windows.Foundation", types = emptyList())),
        ),
    )
    private val planner = AsyncMethodProjectionPlanner(typeNameMapper, WinRtSignatureMapper(typeRegistry))
    private val registry = AsyncMethodRuleRegistry(typeNameMapper, planner)

    @Test
    fun plans_async_operations_with_scalar_results() {
        val plan = registry.plan(
            WinMdMethod(
                name = "GetStringAsync",
                returnType = "Windows.Foundation.IAsyncOperation<String>",
                vtableIndex = 6,
            ),
            "Windows.Foundation",
        )

        assertNotNull(plan)
        assertEquals("windows.foundation.IAsyncOperation<kotlin.String>", plan!!.rawReturnType.toString())
        assertEquals("kotlin.String", plan.awaitReturnType.toString())
    }

    @Test
    fun plans_async_operations_with_progress() {
        val plan = registry.plan(
            WinMdMethod(
                name = "GetProgressAsync",
                returnType = "Windows.Foundation.IAsyncOperationWithProgress<String, Float64>",
                vtableIndex = 7,
            ),
            "Windows.Foundation",
        )

        assertNotNull(plan)
        assertEquals("windows.foundation.IAsyncOperationWithProgress<kotlin.String, kotlin.Double>", plan!!.rawReturnType.toString())
        assertEquals("kotlin.String", plan.awaitReturnType.toString())
    }

    @Test
    fun rejects_non_async_method_returns() {
        assertNull(
            registry.plan(
                WinMdMethod(
                    name = "NotAsync",
                    returnType = "String",
                    vtableIndex = 6,
                ),
                "Windows.Foundation",
            ),
        )
    }

    @Test
    fun rejects_async_methods_when_generic_result_or_progress_cannot_be_projected() {
        assertNull(
            registry.plan(
                WinMdMethod(
                    name = "GetValueAsync",
                    returnType = "Windows.Foundation.IAsyncOperation<T>",
                    vtableIndex = 6,
                ),
                "Windows.Foundation",
                genericParameters = setOf("T"),
            ),
        )
        assertNull(
            registry.plan(
                WinMdMethod(
                    name = "GetProgressAsync",
                    returnType = "Windows.Foundation.IAsyncOperationWithProgress<T, TProgress>",
                    vtableIndex = 7,
                ),
                "Windows.Foundation",
                genericParameters = setOf("T", "TProgress"),
            ),
        )
    }

    @Test
    fun rejects_async_methods_with_nested_generic_result_or_progress_parameters() {
        assertNull(
            registry.plan(
                WinMdMethod(
                    name = "GetValueAsync",
                    returnType = "Windows.Foundation.IAsyncOperation<Windows.Foundation.Collections.IMapView<String, T>>",
                    vtableIndex = 6,
                ),
                "Windows.Foundation",
                genericParameters = setOf("T"),
            ),
        )
        assertNull(
            registry.plan(
                WinMdMethod(
                    name = "GetProgressAsync",
                    returnType = "Windows.Foundation.IAsyncOperationWithProgress<String, Windows.Foundation.Collections.IMapView<String, TProgress>>",
                    vtableIndex = 7,
                ),
                "Windows.Foundation",
                genericParameters = setOf("TProgress"),
            ),
        )
    }
}
