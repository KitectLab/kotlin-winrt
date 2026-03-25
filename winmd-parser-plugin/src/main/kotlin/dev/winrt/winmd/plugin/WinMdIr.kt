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
    val baseInterfaces: List<String> = emptyList(),
    val activationKind: WinMdActivationKind = WinMdActivationKind.Factory,
    val activationFunctionName: String = "activate",
    val fields: List<WinMdField> = emptyList(),
    val enumMembers: List<WinMdEnumMember> = emptyList(),
    val methods: List<WinMdMethod> = emptyList(),
    val properties: List<WinMdProperty> = emptyList(),
)

@Serializable
enum class WinMdActivationKind {
    Factory,
}

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
                            activationKind = WinMdActivationKind.Factory,
                            methods = listOf(
                                WinMdMethod("Start", "Unit", vtableIndex = 6),
                                WinMdMethod("GetLaunchCount", "UInt32", vtableIndex = 7),
                            ),
                        ),
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml",
                            name = "Window",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.IStringable",
                            activationKind = WinMdActivationKind.Factory,
                            activationFunctionName = "activateInstance",
                            methods = listOf(
                                WinMdMethod("Activate", "Unit", vtableIndex = 13),
                            ),
                            properties = listOf(
                                WinMdProperty("Title", "String", mutable = true, getterVtableIndex = 6, setterVtableIndex = 7),
                                WinMdProperty("IsVisible", "Boolean", mutable = false, getterVtableIndex = 8),
                                WinMdProperty("CreatedAt", "DateTime", mutable = false, getterVtableIndex = 10),
                                WinMdProperty("Lifetime", "TimeSpan", mutable = false, getterVtableIndex = 11),
                                WinMdProperty("LastToken", "EventRegistrationToken", mutable = false, getterVtableIndex = 12),
                                WinMdProperty("StableId", "Guid", mutable = false, getterVtableIndex = 9),
                                WinMdProperty("OptionalTitle", "IReference<String>", mutable = false, getterVtableIndex = 14),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }

    fun metadataModel(sourceFiles: List<Path>): WinMdModel {
        return inferInterfaceSlots(WinMdMetadataReader.readModel(sourceFiles))
    }

    fun sampleSupplementalModel(): WinMdModel {
        return WinMdModel(
            files = emptyList(),
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
                    name = "Windows.Data.Json",
                    types = listOf(
                        WinMdType(
                            namespace = "Windows.Data.Json",
                            name = "IJsonValue",
                            kind = WinMdTypeKind.Interface,
                            guid = "a3219a91-eccd-42e5-b553-261d0aefde37",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetNumber",
                                    returnType = "Float64",
                                    vtableIndex = 9,
                                ),
                                WinMdMethod(
                                    name = "GetBoolean",
                                    returnType = "Boolean",
                                    vtableIndex = 10,
                                ),
                                WinMdMethod(
                                    name = "GetObject",
                                    returnType = "Windows.Data.Json.JsonObject",
                                    vtableIndex = 12,
                                ),
                                WinMdMethod(
                                    name = "GetArray",
                                    returnType = "Windows.Data.Json.JsonArray",
                                    vtableIndex = 11,
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Windows.Data.Json",
                            name = "IJsonArray",
                            kind = WinMdTypeKind.Interface,
                            guid = "08c1ddb6-0cbd-4a9a-b5d3-2f852dc37e81",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetObjectAt",
                                    returnType = "Windows.Data.Json.JsonObject",
                                    vtableIndex = 13,
                                    parameters = listOf(
                                        WinMdParameter("index", "UInt32"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Windows.Data.Json",
                            name = "IJsonObject",
                            kind = WinMdTypeKind.Interface,
                            guid = "064e24dd-29c2-4f83-9ac1-9ee11578beb3",
                            methods = listOf(
                                WinMdMethod(
                                    name = "GetNamedString",
                                    returnType = "String",
                                    vtableIndex = 10,
                                    parameters = listOf(
                                        WinMdParameter("name", "String"),
                                    ),
                                ),
                                WinMdMethod(
                                    name = "GetNamedObject",
                                    returnType = "Windows.Data.Json.JsonObject",
                                    vtableIndex = 8,
                                    parameters = listOf(
                                        WinMdParameter("name", "String"),
                                    ),
                                ),
                                WinMdMethod(
                                    name = "GetNamedArray",
                                    returnType = "Windows.Data.Json.JsonArray",
                                    vtableIndex = 9,
                                    parameters = listOf(
                                        WinMdParameter("name", "String"),
                                    ),
                                ),
                                WinMdMethod(
                                    name = "GetNamedNumber",
                                    returnType = "Float64",
                                    vtableIndex = 11,
                                    parameters = listOf(
                                        WinMdParameter("name", "String"),
                                    ),
                                ),
                                WinMdMethod(
                                    name = "GetNamedBoolean",
                                    returnType = "Boolean",
                                    vtableIndex = 12,
                                    parameters = listOf(
                                        WinMdParameter("name", "String"),
                                    ),
                                ),
                            ),
                        ),
                        WinMdType(
                            namespace = "Windows.Data.Json",
                            name = "JsonValueType",
                            kind = WinMdTypeKind.Enum,
                            enumMembers = listOf(
                                WinMdEnumMember("Null", 0),
                                WinMdEnumMember("Boolean", 1),
                                WinMdEnumMember("Number", 2),
                                WinMdEnumMember("String", 3),
                                WinMdEnumMember("Array", 4),
                                WinMdEnumMember("Object", 5),
                            ),
                        ),
                        WinMdType(
                            namespace = "Windows.Data.Json",
                            name = "JsonArray",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Data.Json.IJsonArray",
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
                            activationKind = WinMdActivationKind.Factory,
                            methods = listOf(
                                WinMdMethod("Start", "Unit", vtableIndex = 6),
                                WinMdMethod("GetLaunchCount", "UInt32", vtableIndex = 7),
                            ),
                        ),
                        WinMdType(
                            namespace = "Microsoft.UI.Xaml",
                            name = "Window",
                            kind = WinMdTypeKind.RuntimeClass,
                            defaultInterface = "Windows.Foundation.IStringable",
                            activationKind = WinMdActivationKind.Factory,
                            activationFunctionName = "activateInstance",
                            methods = listOf(
                                WinMdMethod("Activate", "Unit", vtableIndex = 13),
                            ),
                            properties = listOf(
                                WinMdProperty("Title", "String", mutable = true, getterVtableIndex = 6, setterVtableIndex = 7),
                                WinMdProperty("IsVisible", "Boolean", mutable = false, getterVtableIndex = 8),
                                WinMdProperty("CreatedAt", "DateTime", mutable = false, getterVtableIndex = 10),
                                WinMdProperty("Lifetime", "TimeSpan", mutable = false, getterVtableIndex = 11),
                                WinMdProperty("LastToken", "EventRegistrationToken", mutable = false, getterVtableIndex = 12),
                                WinMdProperty("StableId", "Guid", mutable = false, getterVtableIndex = 9),
                                WinMdProperty("OptionalTitle", "IReference<String>", mutable = false, getterVtableIndex = 14),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }

    fun merge(primary: WinMdModel, supplemental: WinMdModel): WinMdModel {
        val mergedFiles = primary.files + supplemental.files
        val mergedNamespaces = (primary.namespaces + supplemental.namespaces)
            .groupBy(WinMdNamespace::name)
            .toSortedMap()
            .map { (namespaceName, namespaceGroup) ->
                val mergedTypes = namespaceGroup
                    .flatMap(WinMdNamespace::types)
                    .groupBy { "${it.namespace}.${it.name}" }
                    .map { (_, types) ->
                        types.drop(1).fold(types.first(), ::mergeType)
                    }
                    .sortedBy(WinMdType::name)
                WinMdNamespace(
                    name = namespaceName,
                    types = mergedTypes,
                )
            }

        return WinMdModel(
            files = mergedFiles,
            namespaces = mergedNamespaces,
        )
    }

    private fun inferInterfaceSlots(model: WinMdModel): WinMdModel {
        return model.copy(
            namespaces = model.namespaces.map { namespace ->
                namespace.copy(
                    types = namespace.types.map { type ->
                        if (type.kind != WinMdTypeKind.Interface) {
                            type
                        } else {
                            type.copy(
                                methods = type.methods.map { method ->
                                    if (method.vtableIndex != null) {
                                        method
                                    } else {
                                        method.copy(
                                            vtableIndex = InterfaceVtableResolver.inferMethodSlot(type, model, method.name),
                                        )
                                    }
                                },
                                properties = type.properties.map { property ->
                                    property.copy(
                                        getterVtableIndex = property.getterVtableIndex ?: type.methods
                                            .firstOrNull { it.name == "get_${property.name}" }
                                            ?.let { method -> InterfaceVtableResolver.inferMethodSlot(type, model, method.name) },
                                        setterVtableIndex = property.setterVtableIndex ?: type.methods
                                            .firstOrNull { it.name == "put_${property.name}" }
                                            ?.let { method -> InterfaceVtableResolver.inferMethodSlot(type, model, method.name) },
                                    )
                                },
                            )
                        }
                    },
                )
            },
        )
    }

    private fun mergeType(primary: WinMdType, supplemental: WinMdType): WinMdType {
        require(primary.namespace == supplemental.namespace && primary.name == supplemental.name) {
            "Cannot merge different types: ${primary.namespace}.${primary.name} vs ${supplemental.namespace}.${supplemental.name}"
        }

        return primary.copy(
            guid = primary.guid ?: supplemental.guid,
            defaultInterface = primary.defaultInterface ?: supplemental.defaultInterface,
            baseInterfaces = if (primary.baseInterfaces.isNotEmpty()) primary.baseInterfaces else supplemental.baseInterfaces,
            activationKind = primary.activationKind,
            activationFunctionName = primary.activationFunctionName.takeIf { it != "activate" } ?: supplemental.activationFunctionName,
            fields = if (primary.fields.isNotEmpty()) primary.fields else supplemental.fields,
            enumMembers = if (primary.enumMembers.isNotEmpty()) primary.enumMembers else supplemental.enumMembers,
            methods = mergeMethods(primary.methods, supplemental.methods),
            properties = mergeProperties(primary.properties, supplemental.properties),
        )
    }

    private fun mergeMethods(primary: List<WinMdMethod>, supplemental: List<WinMdMethod>): List<WinMdMethod> {
        if (primary.isEmpty()) return supplemental
        if (supplemental.isEmpty()) return primary
        val supplementalByName = supplemental.associateBy(WinMdMethod::name)
        val mergedPrimary = primary.map { method ->
            supplementalByName[method.name]?.let { mergeMethod(method, it) } ?: method
        }
        val existingNames = primary.mapTo(linkedSetOf(), WinMdMethod::name)
        val appended = supplemental.filterNot { it.name in existingNames }
        return mergedPrimary + appended
    }

    private fun mergeMethod(primary: WinMdMethod, supplemental: WinMdMethod): WinMdMethod {
        return primary.copy(
            returnType = primary.returnType.takeIf { it != "UnknownType" } ?: supplemental.returnType,
            vtableIndex = primary.vtableIndex ?: supplemental.vtableIndex,
            parameters = if (primary.parameters.isNotEmpty()) primary.parameters else supplemental.parameters,
        )
    }

    private fun mergeProperties(primary: List<WinMdProperty>, supplemental: List<WinMdProperty>): List<WinMdProperty> {
        if (primary.isEmpty()) return supplemental
        if (supplemental.isEmpty()) return primary
        val supplementalByName = supplemental.associateBy(WinMdProperty::name)
        val mergedPrimary = primary.map { property ->
            supplementalByName[property.name]?.let { mergeProperty(property, it) } ?: property
        }
        val existingNames = primary.mapTo(linkedSetOf(), WinMdProperty::name)
        val appended = supplemental.filterNot { it.name in existingNames }
        return mergedPrimary + appended
    }

    private fun mergeProperty(primary: WinMdProperty, supplemental: WinMdProperty): WinMdProperty {
        return primary.copy(
            type = primary.type.takeIf { it != "UnknownType" } ?: supplemental.type,
            mutable = primary.mutable || supplemental.mutable,
            getterVtableIndex = primary.getterVtableIndex ?: supplemental.getterVtableIndex,
            setterVtableIndex = primary.setterVtableIndex ?: supplemental.setterVtableIndex,
        )
    }
}
