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
        "windows/data/json/IJsonObject.kt",
        "windows/data/json/IJsonValue.kt",
        "windows/data/json/JsonObject.kt",
        "windows/data/json/JsonValueType.kt",
        "microsoft/ui/xaml/Application.kt",
        "microsoft/ui/xaml/Window.kt",
    )
    private val trackedTypes = mapOf(
        "Windows.Foundation" to setOf("AsyncStatus", "IStringable", "Point"),
        "Windows.Data.Json" to setOf("IJsonObject", "IJsonValue", "JsonObject", "JsonValueType"),
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
}
