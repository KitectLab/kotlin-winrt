package dev.winrt.winmd.plugin

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
    }
}
