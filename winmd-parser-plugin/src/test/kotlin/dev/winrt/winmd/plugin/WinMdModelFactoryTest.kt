package dev.winrt.winmd.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class WinMdModelFactoryTest {
    @Test
    fun merges_real_metadata_with_sample_supplemental_types() {
        val tempFile = Files.createTempFile("sample", ".winmd")
        Files.write(tempFile, byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()))

        val merged = WinMdModelFactory.merge(
            primary = WinMdModelFactory.minimalModel(listOf(tempFile)),
            supplemental = WinMdModelFactory.sampleSupplementalModel(),
        )

        val namespaceNames = merged.namespaces.map { it.name }
        assertTrue(namespaceNames.contains("Windows.Foundation"))
        assertTrue(namespaceNames.contains("Microsoft.UI.Xaml"))

        val xamlTypes = merged.namespaces.first { it.name == "Microsoft.UI.Xaml" }.types
        assertEquals(listOf("Application", "Window"), xamlTypes.map { it.name })
    }

    @Test
    fun merges_json_supplemental_details_into_real_metadata_types() {
        val primary = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Data.Json",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Data.Json",
                            name = "IJsonObject",
                            kind = WinMdTypeKind.Interface,
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetNamedString",
                                    returnType = "String",
                                    parameters = listOf(WinMdParameter("name", "String")),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Windows.Data.Json",
                            name = "JsonValueType",
                            kind = WinMdTypeKind.Enum,
                        ),
                    ),
                ),
            ),
        )

        val merged = WinMdModelFactory.merge(
            primary = primary,
            supplemental = WinMdModelFactory.sampleSupplementalModel(),
        )

        val jsonNamespace = merged.namespaces.first { it.name == "Windows.Data.Json" }
        val jsonObjectInterface = jsonNamespace.types.first { it.name == "IJsonObject" }
        val jsonValueType = jsonNamespace.types.first { it.name == "JsonValueType" }

        assertEquals("064e24dd-29c2-4f83-9ac1-9ee11578beb3", jsonObjectInterface.guid)
        assertEquals(10, jsonObjectInterface.methods.single { it.name == "GetNamedString" }.vtableIndex)
        assertFalse(jsonValueType.enumMembers.isEmpty())
        assertEquals(listOf("Null", "Boolean", "Number", "String", "Array", "Object"), jsonValueType.enumMembers.map { it.name })
    }

    @Test
    fun prefers_verified_json_interface_surface_over_primary_metadata_slots() {
        val primary = WinMdModel(
            files = emptyList(),
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Data.Json",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Data.Json",
                            name = "IJsonObject",
                            kind = WinMdTypeKind.Interface,
                            guid = "064e24dd-29c2-4f83-9ac1-9ee11578beb3",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetNamedValue",
                                    returnType = "Windows.Data.Json.JsonValue",
                                    vtableIndex = 13,
                                    parameters = listOf(WinMdParameter("name", "String")),
                                ),
                                WinMdMethod(
                                    name = "GetNamedObject",
                                    returnType = "Windows.Data.Json.JsonObject",
                                    vtableIndex = 15,
                                    parameters = listOf(WinMdParameter("name", "String")),
                                ),
                                WinMdMethod(
                                    name = "GetNamedArray",
                                    returnType = "Windows.Data.Json.JsonArray",
                                    vtableIndex = 16,
                                    parameters = listOf(WinMdParameter("name", "String")),
                                ),
                                WinMdMethod(
                                    name = "GetNamedString",
                                    returnType = "String",
                                    vtableIndex = 17,
                                    parameters = listOf(WinMdParameter("name", "String")),
                                ),
                                WinMdMethod(
                                    name = "GetNamedNumber",
                                    returnType = "Float64",
                                    vtableIndex = 18,
                                    parameters = listOf(WinMdParameter("name", "String")),
                                ),
                                WinMdMethod(
                                    name = "GetNamedBoolean",
                                    returnType = "Boolean",
                                    vtableIndex = 19,
                                    parameters = listOf(WinMdParameter("name", "String")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val merged = WinMdModelFactory.merge(
            primary = primary,
            supplemental = WinMdModelFactory.sampleSupplementalModel(),
        )

        val jsonObjectInterface = merged.namespaces
            .first { it.name == "Windows.Data.Json" }
            .types.first { it.name == "IJsonObject" }

        assertEquals(
            listOf(
                "GetNamedString" to 10,
                "GetNamedObject" to 8,
                "GetNamedArray" to 9,
                "GetNamedNumber" to 11,
                "GetNamedBoolean" to 12,
            ),
            jsonObjectInterface.methods.map { it.name to it.vtableIndex },
        )
    }
}
