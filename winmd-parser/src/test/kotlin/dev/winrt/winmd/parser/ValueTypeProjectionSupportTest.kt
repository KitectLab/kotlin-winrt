package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValueTypeProjectionSupportTest {
    private val typeRegistry = TypeRegistry(
        WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Rect",
                            kind = WinMdTypeKind.Struct,
                        ),
                    ),
                ),
                WinMdNamespace(
                    name = "Example.Contracts",
                    types = listOf(
                        WinMdType(
                            namespace = "Example.Contracts",
                            name = "Widget",
                            kind = WinMdTypeKind.RuntimeClass,
                        ),
                        WinMdType(
                            namespace = "Example.Contracts",
                            name = "PointLike",
                            kind = WinMdTypeKind.Struct,
                        ),
                        WinMdType(
                            namespace = "Example.Contracts",
                            name = "Mode",
                            kind = WinMdTypeKind.Enum,
                            enumUnderlyingType = "UInt32",
                        ),
                    ),
                ),
            ),
        ),
    )
    private val projectionSupport = ValueTypeProjectionSupport(TypeNameMapper(), typeRegistry)

    @Test
    fun resolves_descriptor_driven_property_projections() {
        assertNull(projectionSupport.propertyProjection("Int16", "Example.Contracts"))
        assertNotNull(projectionSupport.propertyProjection("Example.Contracts.PointLike", "Example.Contracts"))
        assertNotNull(projectionSupport.propertyProjection("Windows.Foundation.IReference`1<Int16>", "Example.Contracts"))
        assertNotNull(
            projectionSupport.propertyProjection(
                "Windows.Foundation.IReference`1<Example.Contracts.PointLike>",
                "Example.Contracts",
            ),
        )
        assertNotNull(
            projectionSupport.propertyProjection(
                "Windows.Foundation.IReference`1<Example.Contracts.Mode>",
                "Example.Contracts",
            ),
        )
        assertNull(projectionSupport.propertyProjection("Example.Contracts.Widget", "Example.Contracts"))
    }

    @Test
    fun classifies_value_aware_method_plan_kinds() {
        assertEquals(
            ValueAwareMethodPlanKind.SMALL_SCALAR,
            projectionSupport.methodPlanKind("Int16", emptyList(), "Example.Contracts") { false },
        )
        assertEquals(
            ValueAwareMethodPlanKind.STRUCT,
            projectionSupport.methodPlanKind("Example.Contracts.PointLike", emptyList(), "Example.Contracts") { false },
        )
        assertEquals(
            ValueAwareMethodPlanKind.IREFERENCE_VALUE,
            projectionSupport.methodPlanKind("Windows.Foundation.IReference`1<Int16>", emptyList(), "Example.Contracts") { false },
        )
        assertEquals(
            ValueAwareMethodPlanKind.IREFERENCE_VALUE,
            projectionSupport.methodPlanKind("Windows.Foundation.IReference`1<Windows.Foundation.Rect>", emptyList(), "Example.Contracts") { false },
        )
        assertEquals(
            ValueAwareMethodPlanKind.IREFERENCE_GENERIC_STRUCT,
            projectionSupport.methodPlanKind("Windows.Foundation.IReference`1<Example.Contracts.PointLike>", emptyList(), "Example.Contracts") { false },
        )
        assertEquals(
            ValueAwareMethodPlanKind.IREFERENCE_GENERIC_ENUM,
            projectionSupport.methodPlanKind("Windows.Foundation.IReference`1<Example.Contracts.Mode>", emptyList(), "Example.Contracts") { false },
        )
        assertEquals(
            ValueAwareMethodPlanKind.UNIT,
            projectionSupport.methodPlanKind(
                "Unit",
                listOf("Windows.Foundation.IReference`1<Example.Contracts.Mode>"),
                "Example.Contracts",
            ) { false },
        )
        assertEquals(
            ValueAwareMethodPlanKind.OBJECT_RETURN,
            projectionSupport.methodPlanKind(
                "Example.Contracts.Widget",
                listOf("Windows.Foundation.IReference`1<Example.Contracts.Mode>"),
                "Example.Contracts",
            ) { type -> type == "Example.Contracts.Widget" },
        )
    }

    @Test
    fun skips_unit_and_object_plans_without_value_aware_parameters() {
        assertNull(
            projectionSupport.methodPlanKind("Unit", listOf("String"), "Example.Contracts") { false },
        )
        assertNull(
            projectionSupport.methodPlanKind("Example.Contracts.Widget", listOf("String"), "Example.Contracts") {
                type -> type == "Example.Contracts.Widget"
            },
        )
    }
}
