package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModelFactory
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class JsonInterfaceProjectionTest {
    @Test
    fun sample_supplemental_json_interfaces_keep_verified_string_and_interface_returns() {
        val generated = KotlinBindingGenerator().generate(WinMdModelFactory.sampleSupplementalModel())
            .associateBy { it.relativePath.lowercase() }

        val jsonValue = generated.getValue("windows/data/json/IJsonValue.kt".lowercase()).content
        assertTrue(jsonValue.contains("fun stringify(): String"))
        assertTrue(jsonValue.contains("fun getString(): String"))
        assertTrue(jsonValue.contains("invokeHStringMethod(pointer,"))
        assertTrue(jsonValue.contains("7).getOrThrow()"))
        assertTrue(jsonValue.contains("8).getOrThrow()"))

        val jsonArray = generated.getValue("windows/data/json/IJsonArray.kt".lowercase()).content
        assertTrue(jsonArray.contains("fun getStringAt(index: UInt32): String"))
        assertTrue(jsonArray.contains("invokeHStringMethodWithUInt32Arg(pointer, 8,"))

        val jsonObject = generated.getValue("windows/data/json/IJsonObject.kt".lowercase()).content
        assertTrue(jsonObject.contains("fun getNamedString(name: String): String"))
    }

    @Test
    fun merged_real_metadata_keeps_verified_json_string_methods() {
        val inputs = WinMdParserInputResolver.resolve(
            arrayOf(
                "build/generated/checkedInBindings",
                "--contract=Windows.Foundation.UniversalApiContract",
                "--contract=Windows.Foundation.FoundationContract",
            ),
        )
        val model = WinMdModelFactory.merge(
            primary = WinMdModelFactory.metadataModel(inputs.sources),
            supplemental = WinMdModelFactory.sampleSupplementalModel(),
        )

        val jsonNamespace = model.namespaces.first { it.name == "Windows.Data.Json" }
        val jsonValue = jsonNamespace.types.first { it.name == "IJsonValue" }
        assertEquals(
            listOf("Get_ValueType", "Stringify", "GetString", "GetNumber", "GetBoolean", "GetObject", "GetArray"),
            jsonValue.methods.map { it.name },
        )

        val jsonArray = jsonNamespace.types.first { it.name == "IJsonArray" }
        assertTrue(jsonArray.methods.any { it.name == "GetStringAt" && it.returnType == "String" && it.vtableIndex == 8 })

        val jsonObject = jsonNamespace.types.first { it.name == "IJsonObject" }
        assertTrue(jsonObject.methods.any { it.name == "GetNamedString" && it.returnType == "String" && it.vtableIndex == 10 })
        assertTrue(jsonObject.methods.any { it.name == "GetNamedValue" && it.returnType == "Windows.Data.Json.IJsonValue" && it.vtableIndex == 6 })

        val jsonObjectClass = jsonNamespace.types.first { it.name == "JsonObject" }
        assertTrue(jsonObjectClass.defaultInterface ?: "null", jsonObjectClass.defaultInterface == "Windows.Data.Json.IJsonObject")
        assertTrue(
            jsonObjectClass.baseInterfaces.toString(),
            jsonObjectClass.baseInterfaces.contains("Windows.Data.Json.IJsonObject"),
        )
        assertTrue(jsonObjectClass.methods.any { it.name == "GetNamedValue" && it.returnType == "Windows.Data.Json.IJsonValue" && it.vtableIndex == 6 })
        assertTrue(
            jsonObjectClass.methods
                .filter { it.name == "GetNamedString" }
                .map { Triple("${it.name}/${it.parameters.size}", it.returnType, it.vtableIndex) }
                .toString(),
            jsonObjectClass.methods.any { it.name == "GetNamedString" && it.returnType == "String" && it.vtableIndex == 10 },
        )
        assertTrue(jsonObjectClass.methods.any { it.name == "Get_ValueType" && it.returnType == "Windows.Data.Json.JsonValueType" && it.vtableIndex == 6 })
        val generated = KotlinBindingGenerator().generate(model).associateBy { it.relativePath.lowercase() }
        val generatedJsonValue = generated.getValue("windows/data/json/IJsonValue.kt".lowercase()).content
        assertTrue(generatedJsonValue.contains("fun stringify(): String"))
        assertTrue(generatedJsonValue.contains("fun getString(): String"))

        val generatedJsonArray = generated.getValue("windows/data/json/IJsonArray.kt".lowercase()).content
        assertTrue(generatedJsonArray.contains("fun getStringAt(index: UInt32): String"))

        val generatedJsonObject = generated.getValue("windows/data/json/IJsonObject.kt".lowercase()).content
        assertTrue(generatedJsonObject.contains("fun getNamedString(name: String): String"))
        assertTrue(generatedJsonObject.contains("IJsonValue.from(Inspectable("))

        val generatedJsonArrayClass = generated.getValue("windows/data/json/JsonArray.kt".lowercase()).content
        assertTrue(generatedJsonArrayClass.contains("override fun getObjectAt(index: UInt32): JsonObject"))
        assertTrue(generatedJsonArrayClass.contains("invokeObjectMethodWithUInt32Arg(pointer, 6,"))
        assertTrue(generatedJsonArrayClass.contains("JsonValueType.fromValue("))
        assertTrue(!generatedJsonArrayClass.contains("windows.foundation.collections.IVector"))
        assertTrue(!generatedJsonArrayClass.contains("windows.foundation.collections.IIterable"))
        assertEquals(1, generatedJsonArrayClass.lineSequence().count { it.contains("fun get_ValueType(): JsonValueType") })

        val generatedJsonObjectClass = generated.getValue("windows/data/json/JsonObject.kt".lowercase()).content
        assertTrue(generatedJsonObjectClass.contains("override fun getNamedValue(name: String): IJsonValue"))
        assertTrue(generatedJsonObjectClass.contains("invokeObjectMethodWithStringArg(pointer,"))
        assertTrue(generatedJsonObjectClass.contains("6, name).getOrThrow()"))
        assertTrue(generatedJsonObjectClass.contains("invokeHStringMethodWithStringArg(pointer, 10,"))
        assertTrue(generatedJsonObjectClass.contains("JsonValueType.fromValue("))
        assertTrue(!generatedJsonObjectClass.contains("windows.foundation.collections.IMap"))
        assertTrue(!generatedJsonObjectClass.contains("IJsonObjectWithDefaultValues"))
        assertEquals(1, generatedJsonObjectClass.lineSequence().count { it.contains("fun getNamedValue(name: String): IJsonValue") })
    }
}
