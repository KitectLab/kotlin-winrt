package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EnumProjectionSupportTest {
    @Test
    fun accepts_valid_winrt_enum_underlying_types() {
        val typeRegistry = TypeRegistry(
            WinMdModel(
                files = emptyList(),
                namespaces = listOf(
                    WinMdNamespace(
                        name = "Example.Contracts",
                        types = listOf(
                            WinMdType(
                                namespace = "Example.Contracts",
                                name = "Mode",
                                kind = WinMdTypeKind.Enum,
                                enumUnderlyingType = "Int32",
                            ),
                            WinMdType(
                                namespace = "Example.Contracts",
                                name = "Options",
                                kind = WinMdTypeKind.Enum,
                                enumUnderlyingType = "UInt32",
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals("Int32", enumUnderlyingTypeOrDefault(typeRegistry, "Example.Contracts.Mode", "Example.Contracts"))
        assertEquals("UInt32", enumUnderlyingTypeOrDefault(typeRegistry, "Example.Contracts.Options", "Example.Contracts"))
    }

    @Test
    fun rejects_invalid_winrt_enum_underlying_types() {
        val typeRegistry = TypeRegistry(
            WinMdModel(
                files = emptyList(),
                namespaces = listOf(
                    WinMdNamespace(
                        name = "Example.Contracts",
                        types = listOf(
                            WinMdType(
                                namespace = "Example.Contracts",
                                name = "InvalidMode",
                                kind = WinMdTypeKind.Enum,
                                enumUnderlyingType = "Int64",
                            ),
                        ),
                    ),
                ),
            ),
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            enumUnderlyingTypeOrDefault(typeRegistry, "Example.Contracts.InvalidMode", "Example.Contracts")
        }
        assertEquals(
            "Unsupported WinRT enum underlying type: Int64 for Example.Contracts.InvalidMode. WinRT enums must use Int32 or UInt32.",
            exception.message,
        )
    }
}
