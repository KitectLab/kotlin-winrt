package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AsyncMethodProjectionPlannerTest {
    private val typeRegistry = TypeRegistry(
        WinMdModel(
            files = emptyList(),
            namespaces = listOf(WinMdNamespace(name = "Windows.Foundation", types = emptyList())),
        ),
    )
    private val planner = AsyncMethodProjectionPlanner(TypeNameMapper(), WinRtSignatureMapper(typeRegistry))

    @Test
    fun renders_scalar_async_result_and_progress_descriptors() {
        assertTrue(
            planner.asyncResultDescriptorExpression("Windows.Foundation.IAsyncOperation<String>", "Windows.Foundation")
                .toString()
                .contains("AsyncResultTypes"),
        )
        assertTrue(
            planner.asyncResultDescriptorExpression("Windows.Foundation.IAsyncOperation<String>", "Windows.Foundation")
                .toString()
                .contains("signature"),
        )
        assertTrue(
            planner.asyncProgressDescriptorExpression("Windows.Foundation.IAsyncOperationWithProgress<String, Float64>", "Windows.Foundation")
                .toString()
                .contains("AsyncProgressTypes"),
        )
        assertTrue(
            planner.asyncProgressDescriptorExpression("Windows.Foundation.IAsyncOperationWithProgress<String, Float64>", "Windows.Foundation")
                .toString()
                .contains("FLOAT64"),
        )
    }

    @Test
    fun recognizes_async_action_and_operation_result_types() {
        assertEquals(
            "kotlin.Unit",
            planner.awaitReturnType("Windows.Foundation.IAsyncAction", "Windows.Foundation").toString(),
        )
        assertNull(
            planner.asyncResultDescriptorExpression("String", "Windows.Foundation"),
        )
    }

    @Test
    fun rejects_generic_parameters_for_async_descriptors() {
        assertNull(
            planner.asyncResultDescriptorExpression(
                "Windows.Foundation.IAsyncOperation<T>",
                "Windows.Foundation",
                setOf("T"),
            ),
        )
        assertNull(
            planner.asyncProgressDescriptorExpression(
                "Windows.Foundation.IAsyncOperationWithProgress<T, TProgress>",
                "Windows.Foundation",
                setOf("T", "TProgress"),
            ),
        )
    }

    @Test
    fun rejects_nested_generic_parameters_for_async_descriptors() {
        assertNull(
            planner.asyncResultDescriptorExpression(
                "Windows.Foundation.IAsyncOperation<Windows.Foundation.Collections.IMapView<String, T>>",
                "Windows.Foundation",
                setOf("T"),
            ),
        )
        assertNull(
            planner.asyncProgressDescriptorExpression(
                "Windows.Foundation.IAsyncOperationWithProgress<String, Windows.Foundation.Collections.IMapView<String, TProgress>>",
                "Windows.Foundation",
                setOf("TProgress"),
            ),
        )
    }
}
