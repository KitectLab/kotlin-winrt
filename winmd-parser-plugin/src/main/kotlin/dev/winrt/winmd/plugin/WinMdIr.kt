package dev.winrt.winmd.plugin

import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable
data class WinMdModel(
    val files: List<WinMdFile>,
    val namespaces: List<WinMdNamespace>,
)

@Serializable
data class WinMdFile(
    val path: String,
    val size: Long,
    val portableExecutable: Boolean,
)

@Serializable
data class WinMdNamespace(
    val name: String,
    val types: List<WinMdType>,
)

@Serializable
data class WinMdType(
    val namespace: String,
    val name: String,
    val kind: WinMdTypeKind,
    val methods: List<WinMdMethod> = emptyList(),
    val properties: List<WinMdProperty> = emptyList(),
)

@Serializable
enum class WinMdTypeKind {
    Interface,
    RuntimeClass,
    Struct,
    Enum,
}

@Serializable
data class WinMdMethod(
    val name: String,
    val returnType: String,
    val parameters: List<WinMdParameter> = emptyList(),
)

@Serializable
data class WinMdParameter(
    val name: String,
    val type: String,
)

@Serializable
data class WinMdProperty(
    val name: String,
    val type: String,
    val mutable: Boolean,
)

object WinMdModelFactory {
    fun minimalModel(sourceFiles: List<Path>): WinMdModel {
        val fileInfo = sourceFiles.map { path ->
            val peInfo = PortableExecutableReader.inspect(path)
            WinMdFile(
                path = path.toString(),
                size = peInfo.size,
                portableExecutable = peInfo.isPortableExecutable,
            )
        }

        return WinMdModel(
            files = fileInfo,
            namespaces = listOf(
                WinMdNamespace(
                    name = "Windows.Foundation",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "IStringable",
                            kind = WinMdTypeKind.Interface,
                            methods = listOf(
                                WinMdMethod(
                                    name = "ToString",
                                    returnType = "String",
                                ),
                            ),
                        ),
                    ),
                ),
                WinMdNamespace(
                    name = "Microsoft.UI.Xaml",
                    types = listOf(
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml",
                            name = "Application",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod("Start", "Unit"),
                            ),
                        ),
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml",
                            name = "Window",
                            kind = WinMdTypeKind.RuntimeClass,
                            methods = listOf(
                                WinMdMethod("Activate", "Unit"),
                            ),
                            properties = listOf(
                                WinMdProperty("Title", "String", mutable = true),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }
}
