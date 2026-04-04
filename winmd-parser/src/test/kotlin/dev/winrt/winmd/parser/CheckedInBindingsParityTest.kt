package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModelFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
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
        "windows/globalization/ApplicationLanguages.kt",
        "windows/globalization/CalendarIdentifiers.kt",
        "windows/globalization/ClockIdentifiers.kt",
        "windows/globalization/IApplicationLanguagesStatics.kt",
        "windows/globalization/ICalendarIdentifiersStatics.kt",
        "windows/globalization/ICalendar.kt",
        "windows/globalization/IClockIdentifiersStatics.kt",
        "windows/system/userprofile/GlobalizationPreferences.kt",
        "windows/system/userprofile/IGlobalizationPreferencesStatics.kt",
        "windows/globalization/numberformatting/NumeralSystemTranslator.kt",
        "windows/globalization/numberformatting/INumeralSystemTranslator.kt",
        "microsoft/ui/xaml/Application.kt",
        "microsoft/ui/xaml/ApplicationRequiresPointerMode.kt",
        "microsoft/ui/xaml/Window.kt",
    )
    private val foundationRelativePaths = listOf(
        "windows/foundation/AsyncStatus.kt",
        "windows/foundation/IStringable.kt",
        "windows/foundation/Point.kt",
    )
    private val exactParityRelativePaths = listOf(
        "windows/foundation/AsyncStatus.kt",
        "windows/foundation/Point.kt",
        "windows/data/json/JsonValueType.kt",
    )
    private val trackedTypes = mapOf(
        "Windows.Foundation" to setOf("AsyncStatus", "IStringable", "Point"),
        "Windows.Data.Json" to setOf("IJsonArray", "IJsonObject", "IJsonValue", "JsonObject", "JsonValueType"),
        "Windows.Globalization" to setOf("ApplicationLanguages", "Calendar", "CalendarIdentifiers", "ClockIdentifiers", "DayOfWeek", "IApplicationLanguagesStatics", "ICalendar", "ICalendarIdentifiersStatics", "IClockIdentifiersStatics"),
        "Windows.Globalization.NumberFormatting" to setOf("INumeralSystemTranslator", "NumeralSystemTranslator"),
        "Windows.System.UserProfile" to setOf("GlobalizationPreferences", "IGlobalizationPreferencesStatics"),
        "Microsoft.UI.Xaml" to setOf("Application", "ApplicationRequiresPointerMode", "Window"),
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
        val trackedModel = WinMdProjectionModelClosure.retainTypesWithHiddenProjectionDependencies(model, trackedTypes)
        val generatedFiles = KotlinBindingGenerator().generate(trackedModel)
            .associateBy { it.relativePath.lowercase() }
        val checkedInRoot = Path.of("../generated-winrt-bindings/src/commonMain/kotlin")
        val generatedTrackedFiles = generatedFiles.filterKeys { it in trackedRelativePaths.map { path -> path.lowercase() }.toSet() }
        val missingFiles = mutableListOf<String>()

        trackedRelativePaths.forEach { relativePath ->
            val generated = generatedTrackedFiles[relativePath.replace('\\', '/').lowercase()]
            if (generated == null) {
                missingFiles += relativePath
                return@forEach
            }
            val checkedIn = checkedInRoot.resolve(relativePath).readText()
            assertTrue("Generated content should not be blank: $relativePath", generated.content.isNotBlank())
            assertTrue("Checked-in content should not be blank: $relativePath", checkedIn.isNotBlank())
        }

        if (missingFiles.isNotEmpty()) {
            File("build/winui-parity-missing.txt").writeText(missingFiles.joinToString("\n"))
            System.err.println("Missing generated WinUI3 files: ${missingFiles.joinToString(", ")}")
        }
        assertTrue("Missing generated WinUI3 files: ${missingFiles.joinToString(", ")}", missingFiles.isEmpty())
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
        val trackedModel = WinMdProjectionModelClosure.retainTypesWithHiddenProjectionDependencies(model, trackedTypes)
        val generatedFiles = KotlinBindingGenerator().generate(trackedModel)
            .associateBy { it.relativePath.lowercase() }
        val checkedInRoot = Path.of("../generated-winrt-bindings/src/commonMain/kotlin")

        val mismatches = exactParityRelativePaths.mapNotNull { relativePath ->
            val generated = generatedFiles[relativePath.lowercase()]
                ?: return@mapNotNull "Missing generated file: $relativePath"
            val checkedIn = checkedInRoot.resolve(relativePath).readText().replace("\r\n", "\n")
            val generatedContent = generated.content.replace("\r\n", "\n")
            if (checkedIn != generatedContent) {
                val mismatchIndex = checkedIn.zip(generatedContent).indexOfFirst { (left, right) -> left != right }
                val detail = if (mismatchIndex >= 0) {
                    " at index $mismatchIndex: expected '${checkedIn.getOrNull(mismatchIndex)}' but was '${generatedContent.getOrNull(mismatchIndex)}'"
                } else if (checkedIn.length != generatedContent.length) {
                    " with lengths ${checkedIn.length} vs ${generatedContent.length}"
                } else {
                    ""
                }
                "Generated file differs: $relativePath$detail"
            } else null
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
    fun projection_closure_retains_versioned_runtime_helpers_as_hidden_dependencies() {
        val model = WinMdModelFactory.sampleSupplementalModel().copy(
            namespaces = listOf(
                dev.winrt.winmd.plugin.WinMdNamespace(
                    name = "Example.Xaml",
                    types = listOf(
                        dev.winrt.winmd.plugin.WinMdType(
                            namespace = "Example.Xaml",
                            name = "Widget",
                            kind = dev.winrt.winmd.plugin.WinMdTypeKind.RuntimeClass,
                            implementedInterfaces = listOf(
                                "Example.Xaml.IWidgetOverrides",
                                "Example.Xaml.IWidgetOverrides2",
                            ),
                        ),
                        dev.winrt.winmd.plugin.WinMdType(
                            namespace = "Example.Xaml",
                            name = "IWidgetOverrides",
                            kind = dev.winrt.winmd.plugin.WinMdTypeKind.Interface,
                            guid = "11111111-1111-1111-1111-111111111111",
                        ),
                        dev.winrt.winmd.plugin.WinMdType(
                            namespace = "Example.Xaml",
                            name = "IWidgetOverrides2",
                            kind = dev.winrt.winmd.plugin.WinMdTypeKind.Interface,
                            guid = "22222222-2222-2222-2222-222222222222",
                        ),
                    ),
                ),
            ),
        )

        val trackedModel = WinMdProjectionModelClosure.retainTypesWithHiddenProjectionDependencies(
            model,
            mapOf("Example.Xaml" to setOf("Widget")),
        )
        val exampleTypes = trackedModel.namespaces.first { it.name == "Example.Xaml" }.types.map { it.name }.toSet()

        assertTrue(exampleTypes.contains("Widget"))
        assertTrue(exampleTypes.contains("IWidgetOverrides"))
        assertTrue(exampleTypes.contains("IWidgetOverrides2"))
    }

    @Test
    fun projection_closure_retains_versioned_runtime_statics_as_hidden_dependencies() {
        val model = WinMdModelFactory.sampleSupplementalModel().copy(
            namespaces = listOf(
                dev.winrt.winmd.plugin.WinMdNamespace(
                    name = "Example.Xaml",
                    types = listOf(
                        dev.winrt.winmd.plugin.WinMdType(
                            namespace = "Example.Xaml",
                            name = "Widget",
                            kind = dev.winrt.winmd.plugin.WinMdTypeKind.RuntimeClass,
                        ),
                        dev.winrt.winmd.plugin.WinMdType(
                            namespace = "Example.Xaml",
                            name = "IWidgetStatics",
                            kind = dev.winrt.winmd.plugin.WinMdTypeKind.Interface,
                            guid = "33333333-3333-3333-3333-333333333331",
                        ),
                        dev.winrt.winmd.plugin.WinMdType(
                            namespace = "Example.Xaml",
                            name = "IWidgetStatics2",
                            kind = dev.winrt.winmd.plugin.WinMdTypeKind.Interface,
                            guid = "33333333-3333-3333-3333-333333333332",
                        ),
                    ),
                ),
            ),
        )

        val trackedModel = WinMdProjectionModelClosure.retainTypesWithHiddenProjectionDependencies(
            model,
            mapOf("Example.Xaml" to setOf("Widget")),
        )
        val exampleTypes = trackedModel.namespaces.first { it.name == "Example.Xaml" }.types.map { it.name }.toSet()

        assertTrue(exampleTypes.contains("Widget"))
        assertTrue(exampleTypes.contains("IWidgetStatics"))
        assertTrue(exampleTypes.contains("IWidgetStatics2"))
    }

    @Test
    fun projection_closure_retains_versioned_runtime_factories_as_hidden_dependencies() {
        val model = WinMdModelFactory.sampleSupplementalModel().copy(
            namespaces = listOf(
                dev.winrt.winmd.plugin.WinMdNamespace(
                    name = "Example.Xaml",
                    types = listOf(
                        dev.winrt.winmd.plugin.WinMdType(
                            namespace = "Example.Xaml",
                            name = "Widget",
                            kind = dev.winrt.winmd.plugin.WinMdTypeKind.RuntimeClass,
                        ),
                        dev.winrt.winmd.plugin.WinMdType(
                            namespace = "Example.Xaml",
                            name = "IWidgetFactory",
                            kind = dev.winrt.winmd.plugin.WinMdTypeKind.Interface,
                            guid = "33333333-3333-3333-3333-333333333341",
                        ),
                        dev.winrt.winmd.plugin.WinMdType(
                            namespace = "Example.Xaml",
                            name = "IWidgetFactory2",
                            kind = dev.winrt.winmd.plugin.WinMdTypeKind.Interface,
                            guid = "33333333-3333-3333-3333-333333333342",
                        ),
                    ),
                ),
            ),
        )

        val trackedModel = WinMdProjectionModelClosure.retainTypesWithHiddenProjectionDependencies(
            model,
            mapOf("Example.Xaml" to setOf("Widget")),
        )
        val exampleTypes = trackedModel.namespaces.first { it.name == "Example.Xaml" }.types.map { it.name }.toSet()

        assertTrue(exampleTypes.contains("Widget"))
        assertTrue(exampleTypes.contains("IWidgetFactory"))
        assertTrue(exampleTypes.contains("IWidgetFactory2"))
    }

    @Test
    fun checked_in_json_object_retains_verified_runtime_surface() {
        val checkedIn = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/data/json/IJsonObject.kt").readText()

        assertTrue(checkedIn.contains("fun getNamedValue(name: String): IJsonValue"))
        assertTrue(checkedIn.contains("invokeObjectMethodWithStringArg(pointer, 6,"))
        assertTrue(checkedIn.contains("name).getOrThrow()"))
        assertTrue(checkedIn.contains("fun getNamedString(name: String): String"))
        assertTrue(checkedIn.contains("invokeHStringMethodWithStringArg(pointer, 10,"))
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
    fun checked_in_json_object_runtime_class_keeps_verified_activation_surface() {
        val runtimeClass = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/data/json/JsonObject.kt").readText()

        assertTrue(runtimeClass.contains("Windows.Data.Json.JsonObject"))
        assertTrue(runtimeClass.contains("override val defaultInterfaceName: String? = \"Windows.Data.Json.IJsonObject\""))
        assertTrue(runtimeClass.contains("override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory"))
        assertTrue(runtimeClass.contains("private val statics: IJsonObjectStatics by lazy"))
        assertTrue(runtimeClass.contains("WinRtRuntime.projectActivationFactory(this,"))
        assertTrue(runtimeClass.contains("public fun parse(input: String): JsonObject = statics.parse(input)"))
    }

    @Test
    fun checked_in_json_array_runtime_class_keeps_verified_activation_surface() {
        val runtimeClass = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/data/json/JsonArray.kt").readText()

        assertTrue(runtimeClass.contains("Windows.Data.Json.JsonArray"))
        assertTrue(runtimeClass.contains("override val defaultInterfaceName: String? = \"Windows.Data.Json.IJsonArray\""))
        assertTrue(runtimeClass.contains("override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory"))
        assertTrue(runtimeClass.contains("private val statics: IJsonArrayStatics by lazy"))
        assertTrue(runtimeClass.contains("WinRtRuntime.projectActivationFactory(this,"))
        assertTrue(runtimeClass.contains("public fun parse(input: String): JsonArray = statics.parse(input)"))
    }

    @Test
    fun checked_in_application_runtime_class_keeps_verified_activation_surface() {
        val runtimeClass = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/microsoft/ui/xaml/Application.kt").readText()

        assertTrue(runtimeClass.contains("Microsoft.UI.Xaml.Application"))
        assertTrue(runtimeClass.contains("override val defaultInterfaceName: String? = \"Microsoft.UI.Xaml.IApplication\""))
        assertTrue(runtimeClass.contains("override val activationKind = WinRtActivationKind.Composable"))
        assertTrue(runtimeClass.contains("val current: Application"))
        assertTrue(runtimeClass.contains("private val statics: IApplicationStatics by lazy"))
        assertTrue(runtimeClass.contains("WinRtRuntime.projectActivationFactory(this, IApplicationStatics"))
        assertTrue(runtimeClass.contains("fun start(callback: ApplicationInitializationCallback)"))
        assertTrue(runtimeClass.contains("fun start(callback: (IApplicationInitializationCallbackParams) -> Unit): WinRtDelegateHandle"))
    }

    @Test
    fun checked_in_globalization_preferences_runtime_class_keeps_verified_activation_surface() {
        val runtimeClass = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/system/userprofile/GlobalizationPreferences.kt").readText()

        assertTrue(runtimeClass.contains("Windows.System.UserProfile.GlobalizationPreferences"))
        assertTrue(runtimeClass.contains("override val defaultInterfaceName: String? = null"))
        assertTrue(runtimeClass.contains("override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory"))
        assertTrue(runtimeClass.contains("private val statics: IGlobalizationPreferencesStatics by lazy"))
        assertTrue(runtimeClass.contains("private val statics2: IGlobalizationPreferencesStatics2 by lazy"))
        assertTrue(runtimeClass.contains("WinRtRuntime.projectActivationFactory(this, IGlobalizationPreferencesStatics"))
        assertTrue(runtimeClass.contains("WinRtRuntime.projectActivationFactory(this, IGlobalizationPreferencesStatics2"))
        assertTrue(runtimeClass.contains("public val homeGeographicRegion: String"))
        assertTrue(runtimeClass.contains("public fun trySetHomeGeographicRegion(region: String): WinRtBoolean"))
    }

    @Test
    fun checked_in_calendar_identifiers_runtime_class_keeps_verified_activation_surface() {
        val runtimeClass = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/globalization/CalendarIdentifiers.kt").readText()

        assertTrue(runtimeClass.contains("Windows.Globalization.CalendarIdentifiers"))
        assertTrue(runtimeClass.contains("override val defaultInterfaceName: String? = null"))
        assertTrue(runtimeClass.contains("override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory"))
        assertTrue(runtimeClass.contains("private val statics: ICalendarIdentifiersStatics by lazy"))
        assertTrue(runtimeClass.contains("WinRtRuntime.projectActivationFactory(this, ICalendarIdentifiersStatics"))
        assertTrue(runtimeClass.contains("public val gregorian: String"))
        assertTrue(runtimeClass.contains("public val umAlQura: String"))
    }

    @Test
    fun checked_in_clock_identifiers_runtime_class_keeps_verified_activation_surface() {
        val runtimeClass = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/globalization/ClockIdentifiers.kt").readText()

        assertTrue(runtimeClass.contains("Windows.Globalization.ClockIdentifiers"))
        assertTrue(runtimeClass.contains("override val defaultInterfaceName: String? = null"))
        assertTrue(runtimeClass.contains("override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory"))
        assertTrue(runtimeClass.contains("private val statics: IClockIdentifiersStatics by lazy"))
        assertTrue(runtimeClass.contains("WinRtRuntime.projectActivationFactory(this, IClockIdentifiersStatics"))
        assertTrue(runtimeClass.contains("public val twelveHour: String"))
        assertTrue(runtimeClass.contains("public val twentyFourHour: String"))
    }

    @Test
    fun checked_in_json_value_runtime_class_keeps_verified_activation_surface() {
        val runtimeClass = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/data/json/JsonValue.kt").readText()

        assertTrue(runtimeClass.contains("Windows.Data.Json.JsonValue"))
        assertTrue(runtimeClass.contains("override val defaultInterfaceName: String? = \"Windows.Data.Json.IJsonValue\""))
        assertTrue(runtimeClass.contains("override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory"))
        assertTrue(runtimeClass.contains("private val statics: IJsonValueStatics by lazy"))
        assertTrue(runtimeClass.contains("private val statics2: IJsonValueStatics2 by lazy"))
        assertTrue(runtimeClass.contains("WinRtRuntime.projectActivationFactory(this,"))
        assertTrue(runtimeClass.contains("public fun parse(input: String): JsonValue = statics.parse(input)"))
        assertTrue(runtimeClass.contains("public fun createNullValue(): JsonValue = statics2.createNullValue()"))
    }

    @Test
    fun checked_in_json_value_keeps_verified_runtime_surface() {
        val checkedIn = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/data/json/IJsonValue.kt").readText()

        assertTrue(checkedIn.contains("val valueType: JsonValueType"))
        assertTrue(checkedIn.contains("fun get_ValueType(): JsonValueType"))
        assertTrue(checkedIn.contains("invokeUInt32Method(pointer,"))
        assertTrue(checkedIn.contains("6).getOrThrow().toInt()"))
        assertTrue(checkedIn.contains("fun stringify(): String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer,"))
        assertTrue(checkedIn.contains("7).getOrThrow()"))
        assertTrue(checkedIn.contains("fun getString(): String"))
        assertTrue(checkedIn.contains("8).getOrThrow()"))
        assertTrue(checkedIn.contains("fun getNumber(): Float64"))
        assertTrue(checkedIn.contains("invokeFloat64Method(pointer,"))
        assertTrue(checkedIn.contains("9).getOrThrow())"))
        assertTrue(checkedIn.contains("fun getBoolean(): WinRtBoolean"))
        assertTrue(checkedIn.contains("invokeBooleanGetter(pointer, 10).getOrThrow()"))
        assertTrue(checkedIn.contains("fun getArray(): JsonArray"))
        assertTrue(checkedIn.contains("invokeObjectMethod(pointer,"))
        assertTrue(checkedIn.contains("11).getOrThrow()"))
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
        assertTrue(checkedIn.contains("val languages: StringVectorView"))
        assertTrue(checkedIn.contains("fun get_Languages(): StringVectorView"))
        assertTrue(checkedIn.contains("invokeObjectMethod(pointer, 9).getOrThrow()"))
        assertTrue(checkedIn.contains("val dateTime: Instant"))
        assertTrue(checkedIn.contains("invokeInt64Getter(pointer, 16).getOrThrow()"))
        assertTrue(checkedIn.contains("invokeUnitMethodWithInt64Arg("))
        assertTrue(checkedIn.contains("17"))
        assertTrue(checkedIn.contains("Instant.fromEpochSeconds((PlatformComInterop.invokeInt64Getter(pointer, 16).getOrThrow() - 116444736000000000) / 10000000L, ((PlatformComInterop.invokeInt64Getter(pointer, 16).getOrThrow() - 116444736000000000) % 10000000L * 100).toInt())"))
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
        assertTrue(checkedIn.contains("var hour: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Setter(pointer, 74, value.value).getOrThrow()"))
        assertTrue(checkedIn.contains("var minute: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Setter(pointer, 79, value.value).getOrThrow()"))
        assertTrue(checkedIn.contains("var second: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Setter(pointer, 84, value.value).getOrThrow()"))
        assertTrue(checkedIn.contains("var nanosecond: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Setter(pointer, 89, value.value).getOrThrow()"))
        assertTrue(checkedIn.contains("fun get_Year(): Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 30).getOrThrow()"))
        assertTrue(checkedIn.contains("fun addYears(years: Int32)"))
        assertTrue(checkedIn.contains("invokeUnitMethodWithInt32Arg(pointer, 32, years.value).getOrThrow()"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 73).getOrThrow()"))
        assertTrue(checkedIn.contains("fun addHours(hours: Int32)"))
        assertTrue(checkedIn.contains("invokeUnitMethodWithInt32Arg(pointer, 75, hours.value).getOrThrow()"))
        assertTrue(checkedIn.contains("fun hourAsString(): String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 76).getOrThrow()"))
        assertTrue(checkedIn.contains("fun hourAsPaddedString(minDigits: Int32): String"))
        assertTrue(checkedIn.contains("invokeHStringMethodWithInt32Arg(pointer, 77,"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 78).getOrThrow()"))
        assertTrue(checkedIn.contains("fun addMinutes(minutes: Int32)"))
        assertTrue(checkedIn.contains("invokeUnitMethodWithInt32Arg(pointer, 80, minutes.value).getOrThrow()"))
        assertTrue(checkedIn.contains("fun minuteAsString(): String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 81).getOrThrow()"))
        assertTrue(checkedIn.contains("fun minuteAsPaddedString(minDigits: Int32): String"))
        assertTrue(checkedIn.contains("invokeHStringMethodWithInt32Arg(pointer, 82,"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 83).getOrThrow()"))
        assertTrue(checkedIn.contains("fun addSeconds(seconds: Int32)"))
        assertTrue(checkedIn.contains("invokeUnitMethodWithInt32Arg(pointer, 85, seconds.value).getOrThrow()"))
        assertTrue(checkedIn.contains("fun secondAsString(): String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 86).getOrThrow()"))
        assertTrue(checkedIn.contains("fun secondAsPaddedString(minDigits: Int32): String"))
        assertTrue(checkedIn.contains("invokeHStringMethodWithInt32Arg(pointer, 87,"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 88).getOrThrow()"))
        assertTrue(checkedIn.contains("fun addNanoseconds(nanoseconds: Int32)"))
        assertTrue(checkedIn.contains("invokeUnitMethodWithInt32Arg(pointer, 90, nanoseconds.value).getOrThrow()"))
        assertTrue(checkedIn.contains("fun nanosecondAsString(): String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 91).getOrThrow()"))
        assertTrue(checkedIn.contains("fun nanosecondAsPaddedString(minDigits: Int32): String"))
        assertTrue(checkedIn.contains("invokeHStringMethodWithInt32Arg(pointer, 92,"))
        assertTrue(checkedIn.contains("fun yearAsString(): String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 33).getOrThrow()"))
        assertTrue(checkedIn.contains("fun yearAsTruncatedString(remainingDigits: Int32): String"))
        assertTrue(checkedIn.contains("invokeHStringMethodWithInt32Arg(pointer, 34,"))
        assertTrue(checkedIn.contains("fun monthAsString(): String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 42).getOrThrow()"))
        assertTrue(checkedIn.contains("fun monthAsString(idealLength: Int32): String"))
        assertTrue(checkedIn.contains("invokeHStringMethodWithInt32Arg(pointer, 43,"))
        assertTrue(checkedIn.contains("fun monthAsSoloString(): String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 44).getOrThrow()"))
        assertTrue(checkedIn.contains("fun monthAsSoloString(idealLength: Int32): String"))
        assertTrue(checkedIn.contains("invokeHStringMethodWithInt32Arg(pointer, 45,"))
        assertTrue(checkedIn.contains("fun monthAsNumericString(): String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 46).getOrThrow()"))
        assertTrue(checkedIn.contains("fun monthAsPaddedNumericString(minDigits: Int32): String"))
        assertTrue(checkedIn.contains("invokeHStringMethodWithInt32Arg(pointer, 47,"))
        assertTrue(checkedIn.contains("fun addMonths(months: Int32)"))
        assertTrue(checkedIn.contains("invokeUnitMethodWithInt32Arg(pointer, 41, months.value).getOrThrow()"))
        assertTrue(checkedIn.contains("fun addWeeks(weeks: Int32)"))
        assertTrue(checkedIn.contains("invokeUnitMethodWithInt32Arg(pointer, 48, weeks.value).getOrThrow()"))
        assertTrue(checkedIn.contains("fun dayAsString(): String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 55).getOrThrow()"))
        assertTrue(checkedIn.contains("fun dayAsPaddedString(minDigits: Int32): String"))
        assertTrue(checkedIn.contains("invokeHStringMethodWithInt32Arg(pointer, 56,"))
        assertTrue(checkedIn.contains("fun addDays(days: Int32)"))
        assertTrue(checkedIn.contains("invokeUnitMethodWithInt32Arg(pointer, 54, days.value).getOrThrow()"))
        assertTrue(checkedIn.contains("fun dayOfWeekAsString(): String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 58).getOrThrow()"))
        assertTrue(checkedIn.contains("fun dayOfWeekAsString(idealLength: Int32): String"))
        assertTrue(checkedIn.contains("invokeHStringMethodWithInt32Arg(pointer, 59,"))
        assertTrue(checkedIn.contains("fun dayOfWeekAsSoloString(): String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 60).getOrThrow()"))
        assertTrue(checkedIn.contains("fun dayOfWeekAsSoloString(idealLength: Int32): String"))
        assertTrue(checkedIn.contains("invokeHStringMethodWithInt32Arg(pointer, 61,"))
        assertTrue(checkedIn.contains("fun yearAsPaddedString(minDigits: Int32): String"))
        assertTrue(checkedIn.contains("invokeHStringMethodWithInt32Arg(pointer, 35,"))
        assertTrue(checkedIn.contains("fun setToNow()"))
        assertTrue(checkedIn.contains("invokeUnitMethod(pointer, 18).getOrThrow()"))
        assertTrue(checkedIn.contains("fun get_DayOfWeek(): DayOfWeek"))
        assertTrue(checkedIn.contains("invokeUInt32Method(pointer, 57).getOrThrow().toInt()"))
        assertTrue(checkedIn.contains("val resolvedLanguage: String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 102).getOrThrow()"))
        assertTrue(checkedIn.contains("fun get_IsDaylightSavingTime(): WinRtBoolean"))
        assertTrue(checkedIn.contains("invokeBooleanGetter(pointer, 103).getOrThrow()"))
        assertTrue(checkedIn.contains("var era: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Setter(pointer, 23, value.value).getOrThrow()"))
        assertTrue(checkedIn.contains("fun get_FirstEra(): Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 19).getOrThrow()"))
        assertTrue(checkedIn.contains("fun get_LastEra(): Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 20).getOrThrow()"))
        assertTrue(checkedIn.contains("fun get_NumberOfEras(): Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 21).getOrThrow()"))
        assertTrue(checkedIn.contains("fun get_Era(): Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 22).getOrThrow()"))
        assertTrue(checkedIn.contains("fun addEras(eras: Int32)"))
        assertTrue(checkedIn.contains("invokeUnitMethodWithInt32Arg(pointer, 24, eras.value).getOrThrow()"))
        assertTrue(checkedIn.contains("fun eraAsString(): String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 25).getOrThrow()"))
        assertTrue(checkedIn.contains("fun eraAsString(idealLength: Int32): String"))
        assertTrue(checkedIn.contains("invokeHStringMethodWithInt32Arg(pointer, 26,"))
        assertTrue(checkedIn.contains("val firstYearInThisEra: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 27).getOrThrow()"))
        assertTrue(checkedIn.contains("val lastYearInThisEra: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 28).getOrThrow()"))
        assertTrue(checkedIn.contains("val numberOfYearsInThisEra: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 29).getOrThrow()"))
        assertTrue(checkedIn.contains("val firstMonthInThisYear: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 36).getOrThrow()"))
        assertTrue(checkedIn.contains("val lastMonthInThisYear: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 37).getOrThrow()"))
        assertTrue(checkedIn.contains("val numberOfMonthsInThisYear: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 38).getOrThrow()"))
        assertTrue(checkedIn.contains("val firstDayInThisMonth: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 49).getOrThrow()"))
        assertTrue(checkedIn.contains("val lastDayInThisMonth: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 50).getOrThrow()"))
        assertTrue(checkedIn.contains("val numberOfDaysInThisMonth: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 51).getOrThrow()"))
        assertTrue(checkedIn.contains("var period: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Setter(pointer, 66, value.value).getOrThrow()"))
        assertTrue(checkedIn.contains("val firstPeriodInThisDay: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 62).getOrThrow()"))
        assertTrue(checkedIn.contains("val lastPeriodInThisDay: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 63).getOrThrow()"))
        assertTrue(checkedIn.contains("val numberOfPeriodsInThisDay: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 64).getOrThrow()"))
        assertTrue(checkedIn.contains("fun get_Period(): Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 65).getOrThrow()"))
        assertTrue(checkedIn.contains("fun addPeriods(periods: Int32)"))
        assertTrue(checkedIn.contains("invokeUnitMethodWithInt32Arg(pointer, 67, periods.value).getOrThrow()"))
        assertTrue(checkedIn.contains("fun periodAsString(): String"))
        assertTrue(checkedIn.contains("invokeHStringMethod(pointer, 68).getOrThrow()"))
        assertTrue(checkedIn.contains("fun periodAsString(idealLength: Int32): String"))
        assertTrue(checkedIn.contains("invokeHStringMethodWithInt32Arg(pointer, 69,"))
        assertTrue(checkedIn.contains("val firstHourInThisPeriod: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 70).getOrThrow()"))
        assertTrue(checkedIn.contains("val lastHourInThisPeriod: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 71).getOrThrow()"))
        assertTrue(checkedIn.contains("val numberOfHoursInThisPeriod: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 72).getOrThrow()"))
        assertTrue(checkedIn.contains("val firstMinuteInThisHour: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 96).getOrThrow()"))
        assertTrue(checkedIn.contains("val lastMinuteInThisHour: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 97).getOrThrow()"))
        assertTrue(checkedIn.contains("val numberOfMinutesInThisHour: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 98).getOrThrow()"))
        assertTrue(checkedIn.contains("val firstSecondInThisMinute: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 99).getOrThrow()"))
        assertTrue(checkedIn.contains("val lastSecondInThisMinute: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 100).getOrThrow()"))
        assertTrue(checkedIn.contains("val numberOfSecondsInThisMinute: Int32"))
        assertTrue(checkedIn.contains("invokeInt32Method(pointer, 101).getOrThrow()"))
    }

    @Test
    fun checked_in_string_vector_view_keeps_verified_runtime_surface() {
        val checkedIn = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/foundation/collections/StringVectorView.kt").readText()

        assertTrue(checkedIn.contains("class StringVectorView"))
        assertTrue(checkedIn.contains("val winRtSize: UInt32"))
        assertTrue(checkedIn.contains("invokeUInt32Method(pointer, 7).getOrThrow()"))
        assertTrue(checkedIn.contains("fun getAt(index: UInt32): String"))
        assertTrue(checkedIn.contains("invokeHStringMethodWithUInt32Arg(pointer, 6, index.value).getOrThrow()"))
    }

    @Test
    fun checked_in_application_languages_keeps_verified_runtime_surface() {
        val runtimeClass = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/globalization/ApplicationLanguages.kt").readText()
        val statics = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/globalization/IApplicationLanguagesStatics.kt").readText()

        assertTrue(runtimeClass.contains("qualifiedName: String = \"Windows.Globalization.ApplicationLanguages\""))
        assertTrue(runtimeClass.contains("defaultInterfaceName: String? = null"))
        assertTrue(runtimeClass.contains("private val statics: IApplicationLanguagesStatics by lazy"))
        assertTrue(runtimeClass.contains("WinRtRuntime.projectActivationFactory(this, IApplicationLanguagesStatics"))
        assertTrue(runtimeClass.contains("::IApplicationLanguagesStatics)"))
        assertTrue(statics.contains("val primaryLanguageOverride: String"))
        assertTrue(statics.contains("invokeHStringMethod(pointer, 6).getOrThrow()"))
        assertTrue(statics.contains("val languages: StringVectorView"))
        assertTrue(statics.contains("invokeObjectMethod(pointer, 8).getOrThrow()"))
        assertTrue(statics.contains("val manifestLanguages: StringVectorView"))
        assertTrue(statics.contains("invokeObjectMethod(pointer, 9).getOrThrow()"))
        assertTrue(statics.contains("qualifiedName: String = \"Windows.Globalization.IApplicationLanguagesStatics\""))
        assertTrue(statics.contains("75b40847-0a4c-4a92-9565-fd63c95f7aed"))
        assertFalse(runtimeClass.contains("languageFactory"))
    }

    @Test
    fun checked_in_globalization_preferences_keeps_verified_runtime_surface() {
        val runtimeClass = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/system/userprofile/GlobalizationPreferences.kt").readText()
        val statics = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/system/userprofile/IGlobalizationPreferencesStatics.kt").readText()

        assertTrue(runtimeClass.contains("qualifiedName: String = \"Windows.System.UserProfile.GlobalizationPreferences\""))
        assertTrue(runtimeClass.contains("defaultInterfaceName: String? = null"))
        assertTrue(runtimeClass.contains("private val statics: IGlobalizationPreferencesStatics by lazy"))
        assertTrue(runtimeClass.contains("WinRtRuntime.projectActivationFactory(this, IGlobalizationPreferencesStatics"))
        assertTrue(runtimeClass.contains("val calendars: List<String>"))
        assertTrue(runtimeClass.contains("get() = statics.calendars"))
        assertTrue(runtimeClass.contains("val clocks: List<String>"))
        assertTrue(runtimeClass.contains("get() = statics.clocks"))
        assertTrue(runtimeClass.contains("val currencies: List<String>"))
        assertTrue(runtimeClass.contains("get() = statics.currencies"))
        assertTrue(runtimeClass.contains("val languages: List<String>"))
        assertTrue(runtimeClass.contains("get() = statics.languages"))
        assertTrue(runtimeClass.contains("val homeGeographicRegion: String"))
        assertTrue(runtimeClass.contains("get() = statics.homeGeographicRegion"))
        assertTrue(runtimeClass.contains("val weekStartsOn: DayOfWeek"))
        assertTrue(runtimeClass.contains("get() = statics.weekStartsOn"))
        assertTrue(runtimeClass.contains("private val statics2: IGlobalizationPreferencesStatics2 by lazy"))
        assertTrue(runtimeClass.contains("fun trySetHomeGeographicRegion(region: String): WinRtBoolean"))
        assertTrue(runtimeClass.contains("statics2.trySetHomeGeographicRegion(region)"))
        assertTrue(runtimeClass.contains("fun trySetLanguages(languageTags: Iterable<String>): WinRtBoolean"))
        assertTrue(runtimeClass.contains("statics2.trySetLanguages(languageTags)"))
        assertTrue(statics.contains("val calendars: StringVectorView"))
        assertTrue(statics.contains("invokeObjectMethod(pointer, 6).getOrThrow()"))
        assertTrue(statics.contains("val clocks: StringVectorView"))
        assertTrue(statics.contains("invokeObjectMethod(pointer, 7).getOrThrow()"))
        assertTrue(statics.contains("val currencies: StringVectorView"))
        assertTrue(statics.contains("invokeObjectMethod(pointer, 8).getOrThrow()"))
        assertTrue(statics.contains("val languages: StringVectorView"))
        assertTrue(statics.contains("invokeObjectMethod(pointer, 9).getOrThrow()"))
        assertTrue(statics.contains("val homeGeographicRegion: String"))
        assertTrue(statics.contains("invokeHStringMethod(pointer, 10).getOrThrow()"))
        assertTrue(statics.contains("val weekStartsOn: DayOfWeek"))
        assertTrue(statics.contains("invokeUInt32Method(pointer, 11).getOrThrow().toInt()"))
        assertTrue(statics.contains("Windows.System.UserProfile.IGlobalizationPreferencesStatics"))
        assertTrue(statics.contains("01bf4326-ed37-4e96-b0e9-c1340d1ea158"))
    }

    @Test
    fun checked_in_numeral_system_translator_keeps_verified_runtime_surface() {
        val runtimeClass = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/globalization/numberformatting/NumeralSystemTranslator.kt").readText()
        val translator = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/globalization/numberformatting/INumeralSystemTranslator.kt").readText()

        assertTrue(runtimeClass.contains("Windows.Globalization.NumberFormatting.NumeralSystemTranslator"))
        assertTrue(runtimeClass.contains("defaultInterfaceName: String? ="))
        assertTrue(runtimeClass.contains("Windows.Globalization.NumberFormatting.INumeralSystemTranslator"))
        assertTrue(translator.contains("val languages: StringVectorView"))
        assertTrue(translator.contains("invokeObjectMethod(pointer, 6).getOrThrow()"))
        assertTrue(translator.contains("val resolvedLanguage: String"))
        assertTrue(translator.contains("invokeHStringMethod(pointer, 7).getOrThrow()"))
        assertTrue(translator.contains("val numeralSystem: String"))
        assertTrue(translator.contains("invokeHStringMethod(pointer, 8).getOrThrow()"))
        assertTrue(translator.contains("fun translateNumerals(value: String): String"))
        assertTrue(translator.contains("invokeHStringMethodWithStringArg(pointer, 10,"))
        assertTrue(translator.contains("28f5bc2c-8c23-4234-ad2e-fa5a3a426e9b"))
    }

    @Test
    fun checked_in_json_error_keeps_verified_runtime_surface() {
        val runtimeClass = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/data/json/JsonError.kt").readText()

        assertTrue(runtimeClass.contains("Windows.Data.Json.JsonError"))
        assertTrue(runtimeClass.contains("override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory"))
        assertTrue(runtimeClass.contains("private val statics2: IJsonErrorStatics2 by lazy"))
        assertTrue(runtimeClass.contains("WinRtRuntime.projectActivationFactory(this,"))
        assertTrue(runtimeClass.contains("IJsonErrorStatics2"))
        assertTrue(runtimeClass.contains("fun getJsonStatus(hresult: Int32): JsonErrorStatus = statics2.getJsonStatus(hresult)"))
    }

    @Test
    fun checked_in_window_optional_title_is_nullable_string() {
        val checkedIn = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/microsoft/ui/xaml/Window.kt").readText()

        assertTrue(checkedIn.contains("private val backingOptionalTitle = RuntimeProperty<String?>(null)"))
        assertTrue(checkedIn.contains("val optionalTitle: String?"))
        assertTrue(checkedIn.contains("if (pointer.isNull) return backingOptionalTitle.get()"))
        assertTrue(checkedIn.contains("if (value.isNull) null else value.toKotlinString()"))
    }

    @Test
    fun checked_in_identifiers_keeps_verified_runtime_surface() {
        val calendarRuntimeClass = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/globalization/CalendarIdentifiers.kt").readText()
        val calendarStatics = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/globalization/ICalendarIdentifiersStatics.kt").readText()
        val clockRuntimeClass = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/globalization/ClockIdentifiers.kt").readText()
        val clockStatics = Path.of("../generated-winrt-bindings/src/commonMain/kotlin/windows/globalization/IClockIdentifiersStatics.kt").readText()

        assertTrue(calendarRuntimeClass.contains("Windows.Globalization.CalendarIdentifiers"))
        assertTrue(calendarRuntimeClass.contains("defaultInterfaceName: String? = null"))
        assertTrue(calendarRuntimeClass.contains("private val statics: ICalendarIdentifiersStatics by lazy"))
        assertTrue(calendarRuntimeClass.contains("WinRtRuntime.projectActivationFactory(this, ICalendarIdentifiersStatics"))
        assertTrue(calendarStatics.contains("val gregorian: String"))
        assertTrue(calendarStatics.contains("get_Gregorian(): String = readString(6)"))
        assertTrue(calendarStatics.contains("val umAlQura: String"))
        assertTrue(calendarStatics.contains("get_UmAlQura(): String = readString(14)"))
        assertTrue(calendarStatics.contains("80653f68-2cb2-4c1f-b590-f0f52bf4fd1a"))
        assertTrue(clockRuntimeClass.contains("Windows.Globalization.ClockIdentifiers"))
        assertTrue(clockRuntimeClass.contains("defaultInterfaceName: String? = null"))
        assertTrue(clockRuntimeClass.contains("private val statics: IClockIdentifiersStatics by lazy"))
        assertTrue(clockRuntimeClass.contains("WinRtRuntime.projectActivationFactory(this, IClockIdentifiersStatics"))
        assertTrue(clockStatics.contains("val twelveHour: String"))
        assertTrue(clockStatics.contains("get_TwelveHour(): String = readString(6)"))
        assertTrue(clockStatics.contains("val twentyFourHour: String"))
        assertTrue(clockStatics.contains("get_TwentyFourHour(): String = readString(7)"))
        assertTrue(clockStatics.contains("523805bb-12ec-4f83-bc31-b1b4376b0808"))
    }

}
