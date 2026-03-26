package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModelFactory
import dev.winrt.winmd.plugin.WinMdNamespace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path
import kotlin.io.path.readText

class CheckedInBindingsParityTest {
    private val trackedRelativePaths = listOf(
        "windows/foundation/AsyncStatus.kt",
        "windows/foundation/IStringable.kt",
        "windows/foundation/Point.kt",
        "windows/data/json/IJsonArray.kt",
        "windows/data/json/IJsonObject.kt",
        "windows/data/json/IJsonValue.kt",
        "windows/data/json/JsonObject.kt",
        "windows/data/json/JsonValueType.kt",
        "windows/globalization/Calendar.kt",
        "windows/globalization/DayOfWeek.kt",
        "windows/globalization/ICalendar.kt",
        "microsoft/ui/xaml/Application.kt",
        "microsoft/ui/xaml/Window.kt",
    )
    private val foundationRelativePaths = listOf(
        "windows/foundation/AsyncStatus.kt",
        "windows/foundation/IStringable.kt",
        "windows/foundation/Point.kt",
    )
    private val exactParityRelativePaths = listOf(
        "windows/foundation/AsyncStatus.kt",
        "windows/foundation/IStringable.kt",
        "windows/foundation/Point.kt",
        "windows/data/json/IJsonArray.kt",
        "windows/data/json/JsonValueType.kt",
    )
    private val trackedTypes = mapOf(
        "Windows.Foundation" to setOf("AsyncStatus", "IStringable", "Point"),
        "Windows.Data.Json" to setOf("IJsonArray", "IJsonObject", "IJsonValue", "JsonObject", "JsonValueType"),
        "Windows.Globalization" to setOf("Calendar", "DayOfWeek", "ICalendar"),
        "Microsoft.UI.Xaml" to setOf("Application", "Window"),
    )

    @Test
    fun generates_tracked_subset_from_real_metadata_without_special_name_failures() {
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
        val trackedModel = model.copy(
            namespaces = model.namespaces.mapNotNull { namespace ->
                val allowedTypes = trackedTypes[namespace.name] ?: return@mapNotNull null
                val types = namespace.types.filter { it.name in allowedTypes }
                if (types.isEmpty()) {
                    null
                } else {
                    WinMdNamespace(namespace.name, types)
                }
            },
        )
        val generatedFiles = KotlinBindingGenerator().generate(trackedModel)
            .associateBy { it.relativePath.lowercase() }
        val checkedInRoot = Path.of("../generated-winrt-bindings/src/commonMain/kotlin")

        assertEquals(trackedRelativePaths.size, generatedFiles.size)
        trackedRelativePaths.forEach { relativePath ->
            val generated = generatedFiles[relativePath.replace('\\', '/').lowercase()]
            assertTrue("Missing generated file: $relativePath", generated != null)
            val checkedIn = checkedInRoot.resolve(relativePath).readText()
            assertTrue("Generated content should not be blank: $relativePath", generated!!.content.isNotBlank())
            assertTrue("Checked-in content should not be blank: $relativePath", checkedIn.isNotBlank())
        }
    }

    @Test
    fun generated_foundation_subset_matches_checked_in_content() {
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
        val trackedModel = model.copy(
            namespaces = model.namespaces.mapNotNull { namespace ->
                val allowedTypes = trackedTypes[namespace.name] ?: return@mapNotNull null
                val types = namespace.types.filter { it.name in allowedTypes }
                if (types.isEmpty()) null else WinMdNamespace(namespace.name, types)
            },
        )
        val generatedFiles = KotlinBindingGenerator().generate(trackedModel)
            .associateBy { it.relativePath.lowercase() }
        val checkedInRoot = Path.of("../generated-winrt-bindings/src/commonMain/kotlin")

        val mismatches = exactParityRelativePaths.mapNotNull { relativePath ->
            val generated = generatedFiles[relativePath.lowercase()]
                ?: return@mapNotNull "Missing generated file: $relativePath"
            val checkedIn = checkedInRoot.resolve(relativePath).readText()
            if (checkedIn != generated.content) "Generated file differs: $relativePath" else null
        }

        assertTrue(
            buildString {
                appendLine("Foundation checked-in bindings are out of date.")
                mismatches.forEach { appendLine(it) }
            },
            mismatches.isEmpty(),
        )
    }

    @Test
    fun checked_in_json_object_retains_verified_runtime_surface() {
        val checkedIn = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/data/json/IJsonObject.kt").readText()

        assertTrue(checkedIn.contains("fun getNamedValue(name: String): IJsonValue"))
        assertTrue(checkedIn.contains("invokeObjectMethodWithStringArg(pointer, 6, name).getOrThrow()"))
        assertTrue(checkedIn.contains("fun getNamedString(name: String): String"))
        assertTrue(checkedIn.contains("invokeHStringMethodWithStringArg(pointer, 10, name).getOrThrow()"))
        assertTrue(checkedIn.contains("fun getNamedObject(name: String): JsonObject"))
        assertTrue(checkedIn.contains("invokeObjectMethodWithStringArg(pointer, 8, name).getOrThrow()"))
        assertTrue(checkedIn.contains("fun getNamedArray(name: String): JsonArray"))
        assertTrue(checkedIn.contains("invokeObjectMethodWithStringArg(pointer, 9, name).getOrThrow()"))
        assertTrue(checkedIn.contains("fun getNamedNumber(name: String): Float64"))
        assertTrue(checkedIn.contains("invokeFloat64MethodWithStringArg(pointer, 11, name).getOrThrow()"))
        assertTrue(checkedIn.contains("fun getNamedBoolean(name: String): WinRtBoolean"))
        assertTrue(checkedIn.contains("invokeBooleanMethodWithStringArg(pointer, 12,"))
        assertTrue(!checkedIn.contains("invokeObjectMethodWithStringArg(pointer, 15, name).getOrThrow()"))
        assertTrue(!checkedIn.contains("invokeObjectMethodWithStringArg(pointer, 16, name).getOrThrow()"))
        assertTrue(!checkedIn.contains("invokeHStringMethodWithStringArg(pointer, 17, name).getOrThrow()"))
    }

    @Test
    fun checked_in_json_value_keeps_verified_runtime_surface() {
        val checkedIn = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/data/json/IJsonValue.kt").readText()

        assertTrue(checkedIn.contains("val valueType: JsonValueType"))
        assertTrue(checkedIn.contains("fun get_ValueType(): JsonValueType"))
        assertTrue(checkedIn.contains("invokeUInt32Method(pointer, 6).getOrThrow().toInt()"))
        assertTrue(checkedIn.contains("fun stringify(): String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 7).getOrThrow()"))
        assertTrue(checkedIn.contains("fun getString(): String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 8).getOrThrow()"))
        assertTrue(checkedIn.contains("fun getNumber(): Float64"))
        assertTrue(checkedIn.contains("invokeFloat64Method(pointer, 9).getOrThrow()"))
        assertTrue(checkedIn.contains("fun getBoolean(): WinRtBoolean"))
        assertTrue(checkedIn.contains("invokeBooleanGetter(pointer, 10).getOrThrow()"))
        assertTrue(checkedIn.contains("fun getArray(): JsonArray"))
        assertTrue(checkedIn.contains("invokeObjectMethod(pointer, 11)"))
        assertTrue(checkedIn.contains("fun getObject(): JsonObject"))
        assertTrue(checkedIn.contains("invokeObjectMethod(pointer,"))
        assertTrue(checkedIn.contains("12).getOrThrow()"))
    }

    @Test
    fun checked_in_json_array_keeps_verified_runtime_surface() {
        val checkedIn = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/data/json/IJsonArray.kt").readText()

        assertTrue(checkedIn.contains("fun getObjectAt(index: UInt32): JsonObject"))
        assertTrue(checkedIn.contains("invokeObjectMethodWithUInt32Arg(pointer, 6,"))
        assertTrue(checkedIn.contains("fun getArrayAt(index: UInt32): JsonArray"))
        assertTrue(checkedIn.contains("invokeObjectMethodWithUInt32Arg(pointer, 7,"))
        assertTrue(checkedIn.contains("fun getStringAt(index: UInt32): String"))
        assertTrue(checkedIn.contains("invokeHStringMethodWithUInt32Arg(pointer, 8,"))
        assertTrue(checkedIn.contains("fun getNumberAt(index: UInt32): Float64"))
        assertTrue(checkedIn.contains("invokeFloat64MethodWithUInt32Arg(pointer, 9,"))
        assertTrue(checkedIn.contains("fun getBooleanAt(index: UInt32): WinRtBoolean"))
        assertTrue(checkedIn.contains("invokeBooleanMethodWithUInt32Arg(pointer, 10,"))
    }

    @Test
    fun checked_in_calendar_keeps_verified_runtime_surface() {
        val checkedIn = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/globalization/ICalendar.kt").readText()

        assertTrue(checkedIn.contains("fun clone(): Calendar"))
        assertTrue(checkedIn.contains("invokeObjectMethod(pointer, 6).getOrThrow()"))
        assertTrue(checkedIn.contains("var numeralSystem: String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 10).getOrThrow()"))
        assertTrue(checkedIn.contains("invokeStringSetter(pointer, 11, value).getOrThrow()"))
        assertTrue(checkedIn.contains("var calendarSystem: String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 12).getOrThrow()"))
        assertTrue(checkedIn.contains("invokeStringSetter(pointer, 13, value).getOrThrow()"))
        assertTrue(checkedIn.contains("var clock: String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 14).getOrThrow()"))
        assertTrue(checkedIn.contains("invokeStringSetter(pointer, 15, value).getOrThrow()"))
        assertTrue(checkedIn.contains("var month: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Setter(pointer, 40, value.value).getOrThrow()"))
        assertTrue(checkedIn.contains("var day: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Setter(pointer, 53, value.value).getOrThrow()"))
        assertTrue(checkedIn.contains("fun get_Year(): Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 30).getOrThrow()"))
        assertTrue(checkedIn.contains("fun yearAsString(): String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 33).getOrThrow()"))
        assertTrue(checkedIn.contains("fun yearAsPaddedString(minDigits: Int32): String"))
        assertTrue(checkedIn.contains("invokeHStringMethodWithInt32Arg(pointer, 35,"))
        assertTrue(checkedIn.contains("fun get_DayOfWeek(): DayOfWeek"))
        assertTrue(checkedIn.contains("invokeUInt32Method(pointer, 57).getOrThrow().toInt()"))
        assertTrue(checkedIn.contains("val resolvedLanguage: String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 102).getOrThrow()"))
        assertTrue(checkedIn.contains("fun get_IsDaylightSavingTime(): WinRtBoolean"))
        assertTrue(checkedIn.contains("invokeBooleanGetter(pointer, 103).getOrThrow()"))
    }

}
