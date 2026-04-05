package dev.winrt.winmd.plugin

private val supportedWinRtEnumUnderlyingTypes = setOf("Int32", "UInt32")

fun isSupportedWinRtEnumUnderlyingType(underlyingType: String): Boolean =
    underlyingType in supportedWinRtEnumUnderlyingTypes

fun requireSupportedWinRtEnumUnderlyingType(underlyingType: String, enumTypeName: String? = null): String {
    require(isSupportedWinRtEnumUnderlyingType(underlyingType)) {
        buildString {
            append("Unsupported WinRT enum underlying type: ")
            append(underlyingType)
            enumTypeName?.let {
                append(" for ")
                append(it)
            }
            append(". WinRT enums must use Int32 or UInt32.")
        }
    }
    return underlyingType
}
