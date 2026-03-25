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
    val guid: String? = null,
    val defaultInterface: String? = null,
    val fields: List<WinMdField> = emptyList(),
    val enumMembers: List<WinMdEnumMember> = emptyList(),
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
    val vtableIndex: Int? = null,
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
    val getterVtableIndex: Int? = null,
    val setterVtableIndex: Int? = null,
)

@Serializable
data class WinMdField(
    val name: String,
    val type: String,
)

@Serializable
data class WinMdEnumMember(
    val name: String,
    val value: Int,
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
                            guid = "96369f54-8eb6-48f0-abce-c1b211e627c3",
                            methods = listOf(
                                WinMdMethod(
                                    name = "ToString",
                                    returnType = "String",
                                    vtableIndex = 6,
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "Point",
                            kind = WinMdTypeKind.Struct,
                            fields = listOf(
                                WinMdField("X", "Float64"),
                                WinMdField("Y", "Float64"),
                            ),
                        ),
                        WinMdType(
                            namespace = "Windows.Foundation",
                            name = "AsyncStatus",
                            kind = WinMdTypeKind.Enum,
                            enumMembers = listOf(
                                WinMdEnumMember("Started", 0),
                                WinMdEnumMember("Completed", 1),
                                WinMdEnumMember("Canceled", 2),
                                WinMdEnumMember("Error", 3),
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
                            defaultInterface = "Windows.Foundation.IStringable",
                            methods = listOf(
                                WinMdMethod("Start", "Unit"),
                                WinMdMethod("GetLaunchCount", "UInt32", vtableIndex = 7),
                            ),
                        ),
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml",
                            name = "Window",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.IStringable",
                            methods = listOf(
                                WinMdMethod("Activate", "Unit"),
                            ),
                            properties = listOf(
                                WinMdProperty("Title", "String", mutable = true, getterVtableIndex = 6, setterVtableIndex = 7),
                                WinMdProperty("IsVisible", "Boolean", mutable = false, getterVtableIndex = 8),
                                WinMdProperty("CreatedAt", "DateTime", mutable = false, getterVtableIndex = 10),
                                WinMdProperty("Lifetime", "TimeSpan", mutable = false, getterVtableIndex = 11),
                                WinMdProperty("LastToken", "EventRegistrationToken", mutable = false),
                                WinMdProperty("StableId", "Guid", mutable = false, getterVtableIndex = 9),
                                WinMdProperty("OptionalTitle", "IReference<String>", mutable = false),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }
}
