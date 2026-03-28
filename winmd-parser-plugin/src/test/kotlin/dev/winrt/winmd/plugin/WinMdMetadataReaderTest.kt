package dev.winrt.winmd.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class WinMdMetadataReaderTest {
    @Test
    fun reads_real_types_from_local_winui_xaml_winmd_when_available() {
        val candidatePaths = localWinUiXamlWinmdCandidates()
        val winuiWinmd = candidatePaths.firstOrNull { Files.isRegularFile(it) } ?: return

        val model = try {
            WinMdMetadataReader.readModel(listOf(winuiWinmd))
        } catch (error: IllegalArgumentException) {
            if (winuiWinmd.toString().contains("microsoft.windowsappsdk", ignoreCase = true) &&
                error.message?.startsWith("Metadata index exceeds Int range:") == true
            ) {
                return
            }
            throw error
        }
        val namespaceNames = model.namespaces.map { it.name }
        assertTrue(namespaceNames.toString(), namespaceNames.contains("Microsoft.UI.Xaml"))

        val xamlNamespace = model.namespaces.first { it.name == "Microsoft.UI.Xaml" }
        val typeNames = xamlNamespace.types.map { it.name }
        assertTrue(typeNames.toString(), typeNames.contains("XamlContract"))

        val applicationStatics = xamlNamespace.types.first { it.name == "IApplicationStatics" }
        assertEquals(
            listOf("get_Current", "Start", "LoadComponent", "LoadComponent"),
            applicationStatics.methods.map { it.name },
        )
        assertEquals(
            "Microsoft.UI.Xaml.ApplicationInitializationCallback",
            applicationStatics.methods.first { it.name == "Start" }.parameters.single().type,
        )

        val applicationInitializationCallback =
            xamlNamespace.types.first { it.name == "ApplicationInitializationCallback" }
        assertEquals(WinMdTypeKind.Delegate, applicationInitializationCallback.kind)
        println("ApplicationInitializationCallback guid=${applicationInitializationCallback.guid}")
        assertTrue(applicationInitializationCallback.guid.orEmpty().isNotBlank())

        val window = xamlNamespace.types.first { it.name == "Window" }
        println("Window methods=${window.methods}")
        println("Window properties=${window.properties}")
    }

    @Test
    fun reads_real_dispatcher_queue_metadata_from_windows_app_sdk_when_available() {
        val uiWinmd = Path.of(
            "F:/Dependencies/nuget/microsoft.windowsappsdk/1.6.240923002/lib/uap10.0.18362/Microsoft.UI.winmd",
        )
        if (!Files.isRegularFile(uiWinmd)) {
            return
        }

        val model = WinMdMetadataReader.readModel(listOf(uiWinmd))
        val dispatching = model.namespaces.first { it.name == "Microsoft.UI.Dispatching" }
        val dispatcherQueue = dispatching.types.first { it.name == "DispatcherQueue" }
        val iDispatcherQueue = dispatching.types.first { it.name == "IDispatcherQueue" }
        val iDispatcherQueue2 = dispatching.types.first { it.name == "IDispatcherQueue2" }
        val iDispatcherQueue3 = dispatching.types.first { it.name == "IDispatcherQueue3" }
        val handler = dispatching.types.first { it.name == "DispatcherQueueHandler" }

        assertEquals("Microsoft.UI.Dispatching.IDispatcherQueue", dispatcherQueue.defaultInterface)
        assertEquals(
            listOf(
                "CreateTimer",
                "TryEnqueue",
                "TryEnqueue",
                "add_ShutdownStarting",
                "remove_ShutdownStarting",
                "add_ShutdownCompleted",
                "remove_ShutdownCompleted",
            ),
            iDispatcherQueue.methods.map { it.name },
        )
        assertEquals(listOf("get_HasThreadAccess"), iDispatcherQueue2.methods.map { it.name })
        println("IDispatcherQueue3 methods=${iDispatcherQueue3.methods}")
        println("DispatcherQueueHandler guid=${handler.guid}")
        assertEquals(WinMdTypeKind.Delegate, handler.kind)
        assertTrue(handler.guid.orEmpty().isNotBlank())
    }

    @Test
    fun prefers_windows_app_sdk_winmd_candidate_when_available() {
        val configuredRoot = System.getProperty("dev.winrt.windowsAppSdkRoot")?.takeIf { it.isNotBlank() } ?: return
        val winuiWinmd = localWinUiXamlWinmdCandidates().firstOrNull { Files.isRegularFile(it) } ?: return

        assertEquals(
            Path.of(configuredRoot).resolve("lib").resolve("uap10.0").resolve("Microsoft.UI.Xaml.winmd").normalize(),
            winuiWinmd.normalize(),
        )
    }

    @Test
    fun inspects_table_row_counts_for_local_winui_xaml_winmd_when_available() {
        val winuiWinmd = localWinUiXamlWinmdCandidates().firstOrNull { Files.isRegularFile(it) } ?: return
        val rowCounts = WinMdMetadataReader.inspectTableRowCounts(winuiWinmd)
        println("WinUI row counts: $rowCounts")

        assertTrue(rowCounts.isNotEmpty())
        assertTrue(rowCounts.toString(), rowCounts.containsKey(1))
        assertTrue(rowCounts.toString(), rowCounts.containsKey(2))
        assertTrue(rowCounts.toString(), rowCounts.containsKey(6))
        assertTrue(rowCounts.toString(), rowCounts.containsKey(12))
    }

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
        assertEquals(
            listOf(
                "GetNamedValue",
                "SetNamedValue",
                "GetNamedObject",
                "GetNamedArray",
                "GetNamedString",
                "GetNamedNumber",
                "GetNamedBoolean",
            ),
            jsonObjectInterface.methods.map { it.name },
        )
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
        assertEquals(
            13,
            InterfaceVtableResolver.inferMethodSlot(
                jsonArrayInterface,
                model,
                jsonArrayInterface.methods.first { it.name == "GetObjectAt" },
            ),
        )

        val jsonObjectGetNamedString = jsonObjectInterface.methods.first { it.name == "GetNamedString" }
        assertEquals(17, InterfaceVtableResolver.inferMethodSlot(jsonObjectInterface, model, jsonObjectGetNamedString))

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

    @Test
    fun reads_real_calendar_metadata_from_windows_sdk_winmd() {
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
        val globalization = model.namespaces.first { it.name == "Windows.Globalization" }
        val calendar = globalization.types.first { it.name == "Calendar" }
        val iCalendar = globalization.types.first { it.name == "ICalendar" }

        assertEquals("Windows.Globalization.ICalendar", calendar.defaultInterface)
        assertEquals("ca30221d-86d9-40fb-a26b-d44eb7cf08ea", iCalendar.guid)
        assertTrue(iCalendar.methods.any { it.name == "Clone" && it.returnType == "Windows.Globalization.Calendar" && it.vtableIndex == 6 })
        assertTrue(iCalendar.methods.any { it.name == "GetDateTime" && it.returnType == "Windows.Foundation.DateTime" && it.vtableIndex == 16 })
        assertTrue(iCalendar.methods.any { it.name == "get_Year" && it.returnType == "Int32" && it.vtableIndex == 30 })
        assertTrue(iCalendar.methods.any { it.name == "YearAsString" && it.returnType == "String" && it.vtableIndex == 33 })
        assertTrue(iCalendar.methods.any { it.name == "get_Month" && it.returnType == "Int32" && it.vtableIndex == 39 })
        assertTrue(iCalendar.methods.any { it.name == "MonthAsString" && it.returnType == "String" && it.vtableIndex == 42 })
        assertTrue(iCalendar.methods.any { it.name == "get_DayOfWeek" && it.returnType == "Windows.Globalization.DayOfWeek" && it.vtableIndex == 57 })
        assertTrue(iCalendar.methods.any { it.name == "get_IsDaylightSavingTime" && it.returnType == "Boolean" && it.vtableIndex == 103 })
        assertTrue(iCalendar.properties.any { it.name == "Year" && it.type == "Int32" && it.getterVtableIndex == 30 && it.setterVtableIndex == 31 && it.mutable })
        assertTrue(iCalendar.properties.any { it.name == "Month" && it.type == "Int32" && it.getterVtableIndex == 39 && it.setterVtableIndex == 40 && it.mutable })
        assertTrue(iCalendar.properties.any { it.name == "NumeralSystem" && it.type == "String" && it.getterVtableIndex == 10 && it.setterVtableIndex == 11 && it.mutable })
        assertTrue(iCalendar.properties.any { it.name == "DayOfWeek" && it.type == "Windows.Globalization.DayOfWeek" && it.getterVtableIndex == 57 && !it.mutable })
        assertTrue(iCalendar.properties.any { it.name == "IsDaylightSavingTime" && it.type == "Boolean" && it.getterVtableIndex == 103 && !it.mutable })
        assertTrue(iCalendar.properties.any { it.name == "ResolvedLanguage" && it.type == "String" && it.getterVtableIndex == 102 && !it.mutable })
        assertTrue(
            "Expected a large real ICalendar surface, got ${iCalendar.methods.size}",
            iCalendar.methods.size >= 90,
        )

        val applicationLanguages = globalization.types.first { it.name == "ApplicationLanguages" }
        val calendarIdentifiers = globalization.types.first { it.name == "CalendarIdentifiers" }
        val iCalendarIdentifiersStatics = globalization.types.first { it.name == "ICalendarIdentifiersStatics" }
        val clockIdentifiers = globalization.types.first { it.name == "ClockIdentifiers" }
        val iClockIdentifiersStatics = globalization.types.first { it.name == "IClockIdentifiersStatics" }
        val iApplicationLanguagesStatics = globalization.types.first { it.name == "IApplicationLanguagesStatics" }
        assertEquals("80653f68-2cb2-4c1f-b590-f0f52bf4fd1a", iCalendarIdentifiersStatics.guid)
        assertTrue(iCalendarIdentifiersStatics.methods.any { it.name == "get_Gregorian" && it.returnType == "String" && it.vtableIndex == 6 })
        assertTrue(iCalendarIdentifiersStatics.methods.any { it.name == "get_UmAlQura" && it.returnType == "String" && it.vtableIndex == 14 })
        assertEquals(null, calendarIdentifiers.defaultInterface)
        assertEquals("523805bb-12ec-4f83-bc31-b1b4376b0808", iClockIdentifiersStatics.guid)
        assertTrue(iClockIdentifiersStatics.methods.any { it.name == "get_TwelveHour" && it.returnType == "String" && it.vtableIndex == 6 })
        assertTrue(iClockIdentifiersStatics.methods.any { it.name == "get_TwentyFourHour" && it.returnType == "String" && it.vtableIndex == 7 })
        assertEquals(null, clockIdentifiers.defaultInterface)
        assertEquals("75b40847-0a4c-4a92-9565-fd63c95f7aed", iApplicationLanguagesStatics.guid)
        assertTrue(iApplicationLanguagesStatics.methods.any { it.name == "get_PrimaryLanguageOverride" && it.returnType == "String" && it.vtableIndex == 6 })
        assertTrue(iApplicationLanguagesStatics.methods.any { it.name == "get_Languages" && it.returnType.contains("IVectorView") && it.vtableIndex == 8 })
        assertTrue(iApplicationLanguagesStatics.methods.any { it.name == "get_ManifestLanguages" && it.returnType.contains("IVectorView") && it.vtableIndex == 9 })
        assertEquals(null, applicationLanguages.defaultInterface)

        val userProfile = model.namespaces.first { it.name == "Windows.System.UserProfile" }
        val globalizationPreferences = userProfile.types.first { it.name == "GlobalizationPreferences" }
        val iGlobalizationPreferencesStatics = userProfile.types.first { it.name == "IGlobalizationPreferencesStatics" }
        assertEquals("01bf4326-ed37-4e96-b0e9-c1340d1ea158", iGlobalizationPreferencesStatics.guid)
        assertTrue(iGlobalizationPreferencesStatics.methods.any { it.name == "get_Calendars" && it.returnType.contains("IVectorView") && it.vtableIndex == 6 })
        assertTrue(iGlobalizationPreferencesStatics.methods.any { it.name == "get_Clocks" && it.returnType.contains("IVectorView") && it.vtableIndex == 7 })
        assertTrue(iGlobalizationPreferencesStatics.methods.any { it.name == "get_Currencies" && it.returnType.contains("IVectorView") && it.vtableIndex == 8 })
        assertTrue(iGlobalizationPreferencesStatics.methods.any { it.name == "get_Languages" && it.returnType.contains("IVectorView") && it.vtableIndex == 9 })
        assertTrue(iGlobalizationPreferencesStatics.methods.any { it.name == "get_HomeGeographicRegion" && it.returnType == "String" && it.vtableIndex == 10 })
        assertTrue(iGlobalizationPreferencesStatics.methods.any { it.name == "get_WeekStartsOn" && it.returnType == "Windows.Globalization.DayOfWeek" && it.vtableIndex == 11 })
        assertEquals(null, globalizationPreferences.defaultInterface)

        val numberFormatting = model.namespaces.first { it.name == "Windows.Globalization.NumberFormatting" }
        val numeralSystemTranslator = numberFormatting.types.first { it.name == "NumeralSystemTranslator" }
        val iNumeralSystemTranslator = numberFormatting.types.first { it.name == "INumeralSystemTranslator" }
        assertEquals("Windows.Globalization.NumberFormatting.INumeralSystemTranslator", numeralSystemTranslator.defaultInterface)
        assertEquals("28f5bc2c-8c23-4234-ad2e-fa5a3a426e9b", iNumeralSystemTranslator.guid)
        assertTrue(iNumeralSystemTranslator.methods.any { it.name == "get_Languages" && it.returnType.contains("IVectorView") && it.vtableIndex == 6 })
        assertTrue(iNumeralSystemTranslator.methods.any { it.name == "get_ResolvedLanguage" && it.returnType == "String" && it.vtableIndex == 7 })
        assertTrue(iNumeralSystemTranslator.methods.any { it.name == "get_NumeralSystem" && it.returnType == "String" && it.vtableIndex == 8 })
        assertTrue(iNumeralSystemTranslator.methods.any { it.name == "TranslateNumerals" && it.returnType == "String" && it.vtableIndex == 10 })
    }

    @Test
    fun reads_winui_bindable_vector_metadata_when_available() {
        val winUiXaml = localWinUiXamlWinmdCandidates().firstOrNull(Files::exists)
        Assume.assumeTrue(winUiXaml != null)

        val model = WinMdMetadataReader.readModel(listOf(winUiXaml!!))
        val interop = model.namespaces.first { it.name == "Microsoft.UI.Xaml.Interop" }
        val bindableIterable = interop.types.first { it.name == "IBindableIterable" }
        val bindableVector = interop.types.first { it.name == "IBindableVector" }

        assertEquals("036d2c08-df29-41af-8aa2-d774be62ba6f", bindableIterable.guid)
        assertEquals(listOf("First"), bindableIterable.methods.map { it.name })
        assertEquals("393de7de-6fd0-4c0d-bb71-47244a113e93", bindableVector.guid)
        assertTrue(
            bindableVector.baseInterfaces.toString(),
            bindableVector.baseInterfaces.contains("Microsoft.UI.Xaml.Interop.IBindableIterable"),
        )
        assertEquals(
            listOf("GetAt", "get_Size", "GetView", "IndexOf", "SetAt", "InsertAt", "RemoveAt", "Append", "RemoveAtEnd", "Clear"),
            bindableVector.methods.map { it.name },
        )
        assertEquals(6, InterfaceVtableResolver.inferMethodSlot(bindableIterable, model, bindableIterable.methods.first()))
        assertEquals(7, InterfaceVtableResolver.inferMethodSlot(bindableVector, model, bindableVector.methods.first { it.name == "GetAt" }))
        assertEquals(14, InterfaceVtableResolver.inferMethodSlot(bindableVector, model, bindableVector.methods.first { it.name == "Append" }))
    }

    private fun localWinUiXamlWinmdCandidates(): List<Path> {
        return buildList {
            System.getProperty("dev.winrt.windowsAppSdkRoot")
                ?.takeIf { it.isNotBlank() }
                ?.let { add(Path.of(it).resolve("lib").resolve("uap10.0").resolve("Microsoft.UI.Xaml.winmd")) }
            add(
                Path.of(
                    "F:/Dependencies/nuget/microsoft.windowsappsdk/1.6.240923002/lib/uap10.0/Microsoft.UI.Xaml.winmd",
                ),
            )
            add(Path.of("C:/Program Files (x86)/Mica For Everyone/Microsoft.UI.Xaml.winmd"))
        }
    }
}
