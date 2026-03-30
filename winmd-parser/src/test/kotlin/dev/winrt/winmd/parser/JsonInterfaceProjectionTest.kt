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

        val generated = KotlinBindingGenerator().generate(model).associateBy { it.relativePath.lowercase() }
        val generatedJsonValue = generated.getValue("windows/data/json/IJsonValue.kt".lowercase()).content
        assertTrue(generatedJsonValue.contains("fun stringify(): String"))
        assertTrue(generatedJsonValue.contains("fun getString(): String"))

        val generatedJsonArray = generated.getValue("windows/data/json/IJsonArray.kt".lowercase()).content
        assertTrue(generatedJsonArray.contains("fun getStringAt(index: UInt32): String"))

        val generatedJsonObject = generated.getValue("windows/data/json/IJsonObject.kt".lowercase()).content
        assertTrue(generatedJsonObject.contains("fun getNamedString(name: String): String"))
        assertTrue(generatedJsonObject.contains("IJsonValue.from(Inspectable("))
    }
}
