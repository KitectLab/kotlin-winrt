package dev.winrt.winmd.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WinMdMetadataReaderTest {
    @Test
    fun reads_real_types_from_windows_sdk_winmd() {
        val universalContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.UniversalApiContract",
            sdkVersion = "10.0.22621.0",
        )
        val foundationContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.FoundationContract",
            sdkVersion = "10.0.22621.0",
        )

        val model = WinMdMetadataReader.readModel(
            listOf(
                universalContract.winmdPath,
                foundationContract.winmdPath,
            ),
        )
        val allTypes = model.namespaces.flatMap { namespace ->
            namespace.types.map { "${namespace.name}.${it.name}" }
        }

        val sample = allTypes.take(80).joinToString(separator = "\n")
        assertTrue("Missing Windows.Foundation.IStringable.\n$sample", allTypes.contains("Windows.Foundation.IStringable"))
        assertTrue("Missing Windows.Data.Json.JsonObject.\n$sample", allTypes.contains("Windows.Data.Json.JsonObject"))
        assertTrue("Missing Windows.Globalization.Calendar.\n$sample", allTypes.contains("Windows.Globalization.Calendar"))

        val stringable = model.namespaces
            .first { it.name == "Windows.Foundation" }
            .types.first { it.name == "IStringable" }
        assertEquals("96369f54-8eb6-48f0-abce-c1b211e627c3", stringable.guid)
        assertEquals(1, stringable.methods.size)
        assertEquals(stringable.methods.toString(), "ToString", stringable.methods.single().name)
        assertEquals(stringable.methods.toString(), "String", stringable.methods.single().returnType)

        val jsonObject = model.namespaces
            .first { it.name == "Windows.Data.Json" }
            .types.first { it.name == "JsonObject" }
        assertEquals("Windows.Data.Json.IJsonObject", jsonObject.defaultInterface)
        assertTrue(jsonObject.properties.toString(), jsonObject.properties.any { it.name == "ValueType" && it.type == "Windows.Data.Json.JsonValueType" })
        assertTrue(jsonObject.properties.toString(), jsonObject.properties.any { it.name == "Size" && it.type == "UInt32" })

        val jsonObjectInterface = model.namespaces
            .first { it.name == "Windows.Data.Json" }
            .types.first { it.name == "IJsonObject" }
        assertTrue(
            jsonObjectInterface.methods.toString(),
            jsonObjectInterface.methods.any { method ->
                method.name == "GetNamedObject" &&
                    method.returnType == "Windows.Data.Json.JsonObject" &&
                    method.parameters.singleOrNull()?.type == "String"
            },
        )
        assertTrue(
            jsonObjectInterface.methods.toString(),
            jsonObjectInterface.methods.any { method ->
                method.name == "GetNamedArray" &&
                    method.returnType == "Windows.Data.Json.JsonArray" &&
                    method.parameters.singleOrNull()?.type == "String"
            },
        )

        val jsonValueInterface = model.namespaces
            .first { it.name == "Windows.Data.Json" }
            .types.first { it.name == "IJsonValue" }
        assertEquals(
            listOf("get_ValueType", "Stringify", "GetString", "GetNumber", "GetBoolean", "GetArray", "GetObject"),
            jsonValueInterface.methods.map { it.name },
        )
        assertTrue(
            jsonValueInterface.methods.toString(),
            jsonValueInterface.methods.any { method ->
                method.name == "GetObject" &&
                    method.returnType == "Windows.Data.Json.JsonObject"
            },
        )
        assertTrue(
            jsonValueInterface.methods.toString(),
            jsonValueInterface.methods.any { method ->
                method.name == "GetArray" &&
                    method.returnType == "Windows.Data.Json.JsonArray"
            },
        )

        val jsonArray = model.namespaces
            .first { it.name == "Windows.Data.Json" }
            .types.first { it.name == "JsonArray" }
        assertEquals("Windows.Data.Json.IJsonArray", jsonArray.defaultInterface)

        val jsonArrayInterface = model.namespaces
            .first { it.name == "Windows.Data.Json" }
            .types.first { it.name == "IJsonArray" }
        assertEquals("08c1ddb6-0cbd-4a9a-b5d3-2f852dc37e81", jsonArrayInterface.guid)
        assertTrue(
            jsonArrayInterface.baseInterfaces.toString(),
            jsonArrayInterface.baseInterfaces.contains("Windows.Data.Json.IJsonValue"),
        )
        assertEquals(
            listOf("GetObjectAt", "GetArrayAt", "GetStringAt", "GetNumberAt", "GetBooleanAt"),
            jsonArrayInterface.methods.map { it.name },
        )
        assertTrue(
            jsonArrayInterface.methods.toString(),
            jsonArrayInterface.methods.any { method ->
                method.name == "GetObjectAt" &&
                    method.returnType == "Windows.Data.Json.JsonObject" &&
                    method.parameters.singleOrNull()?.type == "UInt32"
            },
        )
        assertEquals(13, InterfaceVtableResolver.inferMethodSlot(jsonArrayInterface, model, "GetObjectAt"))

        val calendar = model.namespaces
            .first { it.name == "Windows.Globalization" }
            .types.first { it.name == "Calendar" }
        assertEquals("Windows.Globalization.ICalendar", calendar.defaultInterface)
    }

    @Test
    fun infers_vtable_slots_for_interface_methods_from_metadata_model() {
        val universalContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.UniversalApiContract",
            sdkVersion = "10.0.22621.0",
        )
        val foundationContract = WindowsSdkReferences.findContract(
            contractName = "Windows.Foundation.FoundationContract",
            sdkVersion = "10.0.22621.0",
        )

        val model = WinMdModelFactory.metadataModel(
            listOf(
                universalContract.winmdPath,
                foundationContract.winmdPath,
            ),
        )
        val jsonArrayInterface = model.namespaces
            .first { it.name == "Windows.Data.Json" }
            .types.first { it.name == "IJsonArray" }
        val getObjectAt = jsonArrayInterface.methods.first { it.name == "GetObjectAt" }
        val jsonValueInterface = model.namespaces
            .first { it.name == "Windows.Data.Json" }
            .types.first { it.name == "IJsonValue" }
        val valueTypeProperty = jsonValueInterface.properties.first { it.name == "ValueType" }

        assertEquals(13, getObjectAt.vtableIndex)
        assertEquals(6, valueTypeProperty.getterVtableIndex)
    }
}
