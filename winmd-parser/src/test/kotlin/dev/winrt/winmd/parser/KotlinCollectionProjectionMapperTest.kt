package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind
import org.junit.Assert.assertEquals
import org.junit.Test

class KotlinCollectionProjectionMapperTest {
    private val typeRegistry = TypeRegistry(
        WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Globalization",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Globalization",
                            name = "Calendar",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Globalization.ICalendar",
                            baseInterfaces = listOf(
                                "Windows.Globalization.ICalendar",
                                "Windows.Foundation.Collections.IIterable`1<Windows.Foundation.String>",
                                "Windows.Foundation.Collections.IIterator`1<Windows.Foundation.String>",
                            ),
                        ),
                        WinMdType(
                            namespace = "Windows.Globalization",
                            name = "ICalendar",
                            kind = WinMdTypeKind.Interface,
                            guid = "cccccccc-cccc-cccc-cccc-cccccccccccc",
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterable`1",
                            kind = WinMdTypeKind.Interface,
                            guid = "faa585ea-6214-4217-afda-7f46de5869b3",
                            genericParameters = listOf("T"),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation.Collections",
                            name = "IIterator`1",
                            kind = WinMdTypeKind.Interface,
                            guid = "6a79e863-4300-459a-9966-cbb660963ee1",
                            genericParameters = listOf("T"),
                        ),
                    ),
                ),
            ),
        ),
    )

    private val mapper = KotlinCollectionProjectionMapper()

    @Test
    fun enumerates_collection_interfaces_using_registry_helpers() {
        assertEquals(
            listOf(
                "Windows.Globalization.ICalendar",
                "Windows.Foundation.Collections.IIterable`1<Windows.Foundation.String>",
                "Windows.Foundation.Collections.IIterator`1<Windows.Foundation.String>",
            ),
            mapper.runtimeClassCollectionInterfaces(typeRegistry.findType("Calendar", "Windows.Globalization")!!).toList(),
        )
    }
}
