package dev.winrt.winmd.parser

import dev.winrt.core.ParameterizedInterfaceId
import dev.winrt.core.WinRtTypeSignature
import dev.winrt.winmd.plugin.stripValueTypeNameMarker
import dev.winrt.winmd.plugin.WinMdTypeKind

internal class WinRtSignatureMapper(
    private val typeRegistry: TypeRegistry,
) {
    fun signatureFor(typeName: String, currentNamespace: String): String {
        val normalizedTypeName = normalizeTypeName(typeName, currentNamespace)
        if ('<' in normalizedTypeName && normalizedTypeName.endsWith(">")) {
            return signatureForGenericType(normalizedTypeName, currentNamespace)
        }

        scalarSignature(normalizedTypeName)?.let { return it }

        val type = typeRegistry.findType(normalizedTypeName)
            ?: error("Unknown WinRT type: $typeName")
        val qualifiedName = "${type.namespace}.${type.name}"
        return when (type.kind) {
            WinMdTypeKind.Interface -> WinRtTypeSignature.guid(type.guid ?: error("Missing GUID for interface $qualifiedName"))
            WinMdTypeKind.Delegate -> "delegate(${WinRtTypeSignature.guid(type.guid ?: error("Missing GUID for delegate $qualifiedName"))})"
            WinMdTypeKind.RuntimeClass -> {
                val defaultInterfaceName = type.defaultInterface
                    ?: error("Missing default interface for runtime class $qualifiedName")
                WinRtTypeSignature.runtimeClass(
                    qualifiedName = qualifiedName,
                    defaultInterfaceSignature = signatureFor(defaultInterfaceName, type.namespace),
                )
            }
            WinMdTypeKind.Enum -> WinRtTypeSignature.enum(
                qualifiedName,
                wellKnownTypeSignatures[enumSignatureType(typeRegistry, qualifiedName, type.namespace)]
                    ?: error("Unsupported enum signature type for $qualifiedName"),
            )
            WinMdTypeKind.Struct -> {
                val fieldSignatures = type.fields
                    .map { field -> signatureFor(field.type, type.namespace) }
                    .toTypedArray()
                WinRtTypeSignature.struct(qualifiedName, *fieldSignatures)
            }
        }
    }

    fun interfaceIdFor(typeName: String, currentNamespace: String): String {
        val signature = signatureFor(typeName, currentNamespace)
        return when {
            signature.startsWith("pinterface(") -> {
                ParameterizedInterfaceId.createFromSignature(signature).toString()
            }
            signature.startsWith("{") && signature.endsWith("}") -> {
                signature.removePrefix("{").removeSuffix("}")
            }
            else -> error("Type $typeName does not project to an interface IID")
        }
    }

    private fun signatureForGenericType(typeName: String, currentNamespace: String): String {
        val genericStart = typeName.indexOf('<')
        val rawTypeName = normalizeTypeName(typeName.substring(0, genericStart), currentNamespace)
        val argumentSource = typeName.substring(genericStart + 1, typeName.length - 1)
        val interfaceGuid = typeRegistry.findType(rawTypeName)?.guid
            ?: wellKnownGenericInterfaceGuids[rawTypeName]
            ?: error("Unknown generic WinRT type: $rawTypeName")
        val argumentSignatures = splitGenericArguments(argumentSource)
            .map { signatureFor(it, currentNamespace) }
            .toTypedArray()
        return WinRtTypeSignature.parameterizedInterface(interfaceGuid, *argumentSignatures)
    }

    private fun normalizeTypeName(typeName: String, currentNamespace: String): String {
        val unwrappedTypeName = stripValueTypeNameMarker(typeName.removeSuffix("[]"))
        scalarSignature(unwrappedTypeName)?.let { return unwrappedTypeName }
        return when {
            '.' in unwrappedTypeName -> unwrappedTypeName
            else -> "$currentNamespace.$unwrappedTypeName"
        }
    }

    private fun scalarSignature(typeName: String): String? =
        wellKnownTypeSignatures[canonicalWinRtSpecialType(typeName)]
            ?: wellKnownTypeSignatures[typeName]

    private fun splitGenericArguments(source: String): List<String> {
        if (source.isBlank()) {
            return emptyList()
        }
        val arguments = mutableListOf<String>()
        var depth = 0
        var start = 0
        source.forEachIndexed { index, char ->
            when (char) {
                '<' -> depth++
                '>' -> depth--
                ',' -> if (depth == 0) {
                    arguments += source.substring(start, index).trim()
                    start = index + 1
                }
            }
        }
        arguments += source.substring(start).trim()
        return arguments
    }

    private companion object {
        val wellKnownTypeSignatures = mapOf(
            "String" to WinRtTypeSignature.string(),
            "Object" to WinRtTypeSignature.object_(),
            "Boolean" to "b1",
            "Int8" to "i1",
            "UInt8" to "u1",
            "Int16" to "i2",
            "UInt16" to "u2",
            "Char16" to "c2",
            "HResult" to "struct(Windows.Foundation.HResult;i4)",
            "Windows.Foundation.HResult" to "struct(Windows.Foundation.HResult;i4)",
            "Int32" to "i4",
            "UInt32" to "u4",
            "Int64" to "i8",
            "UInt64" to "u8",
            "Float32" to "f4",
            "Float64" to "f8",
            "Guid" to "g16",
            "DateTime" to "struct(Windows.Foundation.DateTime;i8)",
            "TimeSpan" to "struct(Windows.Foundation.TimeSpan;i8)",
            "EventRegistrationToken" to "struct(Windows.Foundation.EventRegistrationToken;i8)",
            "WindowId" to "struct(Microsoft.UI.WindowId;u8)",
            "Microsoft.UI.WindowId" to "struct(Microsoft.UI.WindowId;u8)",
            "Point" to "struct(Windows.Foundation.Point;f4;f4)",
            "Windows.Foundation.Point" to "struct(Windows.Foundation.Point;f4;f4)",
            "Size" to "struct(Windows.Foundation.Size;f4;f4)",
            "Windows.Foundation.Size" to "struct(Windows.Foundation.Size;f4;f4)",
            "Rect" to "struct(Windows.Foundation.Rect;f4;f4;f4;f4)",
            "Windows.Foundation.Rect" to "struct(Windows.Foundation.Rect;f4;f4;f4;f4)",
            "Vector2" to "struct(Windows.Foundation.Numerics.Vector2;f4;f4)",
            "Windows.Foundation.Numerics.Vector2" to "struct(Windows.Foundation.Numerics.Vector2;f4;f4)",
            "Vector3" to "struct(Windows.Foundation.Numerics.Vector3;f4;f4;f4)",
            "Windows.Foundation.Numerics.Vector3" to "struct(Windows.Foundation.Numerics.Vector3;f4;f4;f4)",
            "Vector4" to "struct(Windows.Foundation.Numerics.Vector4;f4;f4;f4;f4)",
            "Windows.Foundation.Numerics.Vector4" to "struct(Windows.Foundation.Numerics.Vector4;f4;f4;f4;f4)",
            "Matrix3x2" to "struct(Windows.Foundation.Numerics.Matrix3x2;f4;f4;f4;f4;f4;f4)",
            "Windows.Foundation.Numerics.Matrix3x2" to "struct(Windows.Foundation.Numerics.Matrix3x2;f4;f4;f4;f4;f4;f4)",
            "Matrix4x4" to "struct(Windows.Foundation.Numerics.Matrix4x4;f4;f4;f4;f4;f4;f4;f4;f4;f4;f4;f4;f4;f4;f4;f4;f4)",
            "Windows.Foundation.Numerics.Matrix4x4" to "struct(Windows.Foundation.Numerics.Matrix4x4;f4;f4;f4;f4;f4;f4;f4;f4;f4;f4;f4;f4;f4;f4;f4;f4)",
            "Plane" to "struct(Windows.Foundation.Numerics.Plane;struct(Windows.Foundation.Numerics.Vector3;f4;f4;f4);f4)",
            "Windows.Foundation.Numerics.Plane" to "struct(Windows.Foundation.Numerics.Plane;struct(Windows.Foundation.Numerics.Vector3;f4;f4;f4);f4)",
            "Quaternion" to "struct(Windows.Foundation.Numerics.Quaternion;f4;f4;f4;f4)",
            "Windows.Foundation.Numerics.Quaternion" to "struct(Windows.Foundation.Numerics.Quaternion;f4;f4;f4;f4)",
            "Rational" to "struct(Windows.Foundation.Numerics.Rational;u4;u4)",
            "Windows.Foundation.Numerics.Rational" to "struct(Windows.Foundation.Numerics.Rational;u4;u4)",
            "Color" to "struct(Windows.UI.Color;u1;u1;u1;u1)",
            "Windows.UI.Color" to "struct(Windows.UI.Color;u1;u1;u1;u1)",
            "CorePhysicalKeyStatus" to "struct(Windows.UI.Core.CorePhysicalKeyStatus;u4;u4;b1;b1;b1;b1)",
            "Windows.UI.Core.CorePhysicalKeyStatus" to "struct(Windows.UI.Core.CorePhysicalKeyStatus;u4;u4;b1;b1;b1;b1)",
            "FontWeight" to "struct(Windows.UI.Text.FontWeight;u2)",
            "Windows.UI.Text.FontWeight" to "struct(Windows.UI.Text.FontWeight;u2)",
            "TypeName" to "struct(Windows.UI.Xaml.Interop.TypeName;string;enum(Windows.UI.Xaml.Interop.TypeKind;i4))",
            "Windows.UI.Xaml.Interop.TypeName" to "struct(Windows.UI.Xaml.Interop.TypeName;string;enum(Windows.UI.Xaml.Interop.TypeKind;i4))",
        )

        val wellKnownGenericInterfaceGuids = mapOf(
            "Windows.Foundation.Collections.IIterable" to "faa585ea-6214-4217-afda-7f46de5869b3",
            "Windows.Foundation.Collections.IIterable`1" to "faa585ea-6214-4217-afda-7f46de5869b3",
            "Windows.Foundation.Collections.IIterator" to "6a79e863-4300-459a-9966-cbb660963ee1",
            "Windows.Foundation.Collections.IIterator`1" to "6a79e863-4300-459a-9966-cbb660963ee1",
            "Windows.Foundation.Collections.IVector" to "913337e9-11a1-4345-a3a2-4e7f956e222d",
            "Windows.Foundation.Collections.IVector`1" to "913337e9-11a1-4345-a3a2-4e7f956e222d",
            "Windows.Foundation.Collections.IVectorView" to "bbe1fa4c-b0e3-4583-baef-1f1b2e483e56",
            "Windows.Foundation.Collections.IVectorView`1" to "bbe1fa4c-b0e3-4583-baef-1f1b2e483e56",
            "Windows.Foundation.Collections.IMap" to "3c2925fe-8519-45c1-aa79-197b6718c1c1",
            "Windows.Foundation.Collections.IMap`2" to "3c2925fe-8519-45c1-aa79-197b6718c1c1",
            "Windows.Foundation.Collections.IMapView" to "e480ce40-a338-4ada-adcf-272272e48cb9",
            "Windows.Foundation.Collections.IMapView`2" to "e480ce40-a338-4ada-adcf-272272e48cb9",
            "Windows.Foundation.Collections.IKeyValuePair" to "02b51929-c1c4-4a7e-8940-0312b5c18500",
            "Windows.Foundation.Collections.IKeyValuePair`2" to "02b51929-c1c4-4a7e-8940-0312b5c18500",
            "Windows.Foundation.Collections.IObservableVector" to "5917eb53-50b4-4a0d-b309-65862b3f1dbc",
            "Windows.Foundation.Collections.IObservableVector`1" to "5917eb53-50b4-4a0d-b309-65862b3f1dbc",
            "Windows.Foundation.Collections.IObservableMap" to "65df2bf5-bf39-41b5-aebc-5a9d865e472b",
            "Windows.Foundation.Collections.IObservableMap`2" to "65df2bf5-bf39-41b5-aebc-5a9d865e472b",
            "Windows.Foundation.Collections.IMapChangedEventArgs" to "9939f4df-050a-4c0f-aa60-77075f9c4777",
            "Windows.Foundation.Collections.IMapChangedEventArgs`1" to "9939f4df-050a-4c0f-aa60-77075f9c4777",
            "Windows.Foundation.Collections.MapChangedEventHandler" to "179517f3-94ee-41f8-bddc-768a895544f3",
            "Windows.Foundation.Collections.MapChangedEventHandler`2" to "179517f3-94ee-41f8-bddc-768a895544f3",
            "Windows.Foundation.Collections.VectorChangedEventHandler" to "0c051752-9fbf-4c70-aa0c-0e4c82d9a761",
            "Windows.Foundation.Collections.VectorChangedEventHandler`1" to "0c051752-9fbf-4c70-aa0c-0e4c82d9a761",
            "Windows.Foundation.IReference" to "61c17706-2d65-11e0-9ae8-d48564015472",
            "Windows.Foundation.IReference`1" to "61c17706-2d65-11e0-9ae8-d48564015472",
            "Windows.Foundation.IReferenceArray" to "61c17707-2d65-11e0-9ae8-d48564015472",
            "Windows.Foundation.IReferenceArray`1" to "61c17707-2d65-11e0-9ae8-d48564015472",
            "Windows.Foundation.IAsyncActionWithProgress" to "1f6db258-e803-48a1-9546-eb7353398884",
            "Windows.Foundation.IAsyncActionWithProgress`1" to "1f6db258-e803-48a1-9546-eb7353398884",
            "Windows.Foundation.IAsyncOperation" to "9fc2b0bb-e446-44e2-aa61-9cab8f636af2",
            "Windows.Foundation.IAsyncOperation`1" to "9fc2b0bb-e446-44e2-aa61-9cab8f636af2",
            "Windows.Foundation.IAsyncOperationWithProgress" to "b5d036d7-e297-498f-ba60-0289e76e23dd",
            "Windows.Foundation.IAsyncOperationWithProgress`2" to "b5d036d7-e297-498f-ba60-0289e76e23dd",
            "Windows.Foundation.EventHandler" to "9de1c535-6ae1-11e0-84e1-18a905bcc53f",
            "Windows.Foundation.EventHandler`1" to "9de1c535-6ae1-11e0-84e1-18a905bcc53f",
            "Windows.Foundation.TypedEventHandler" to "9de1c534-6ae1-11e0-84e1-18a905bcc53f",
            "Windows.Foundation.TypedEventHandler`2" to "9de1c534-6ae1-11e0-84e1-18a905bcc53f",
            "Windows.Foundation.AsyncActionProgressHandler" to "6d844858-0cff-4590-ae89-95a5a5c8b4b8",
            "Windows.Foundation.AsyncActionProgressHandler`1" to "6d844858-0cff-4590-ae89-95a5a5c8b4b8",
            "Windows.Foundation.AsyncActionWithProgressCompletedHandler" to "9c029f91-cc84-44fd-ac26-0a6c4e555281",
            "Windows.Foundation.AsyncActionWithProgressCompletedHandler`1" to "9c029f91-cc84-44fd-ac26-0a6c4e555281",
            "Windows.Foundation.AsyncOperationCompletedHandler" to "fcdcf02c-e5d8-4478-915a-4d90b74b83a5",
            "Windows.Foundation.AsyncOperationCompletedHandler`1" to "fcdcf02c-e5d8-4478-915a-4d90b74b83a5",
            "Windows.Foundation.AsyncOperationProgressHandler" to "55690902-0aab-421a-8778-f8ce5026d758",
            "Windows.Foundation.AsyncOperationProgressHandler`2" to "55690902-0aab-421a-8778-f8ce5026d758",
            "Windows.Foundation.AsyncOperationWithProgressCompletedHandler" to "e85df41d-6aa7-46e3-a8e2-f009d840c627",
            "Windows.Foundation.AsyncOperationWithProgressCompletedHandler`2" to "e85df41d-6aa7-46e3-a8e2-f009d840c627",
        )
    }
}
