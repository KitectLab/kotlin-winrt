package dev.winrt.core

object WinRtTypeSignature {
    fun string(): String = "string"

    fun object_(): String = "cinterface(IInspectable)"

    fun guid(iid: String): String = "{${iid.lowercase()}}"

    fun runtimeClass(qualifiedName: String, defaultInterfaceSignature: String): String =
        "rc($qualifiedName;$defaultInterfaceSignature)"

    fun struct(qualifiedName: String, vararg fieldSignatures: String): String =
        "struct($qualifiedName;${fieldSignatures.joinToString(";")})"

    fun parameterizedInterface(interfaceIid: String, vararg argumentSignatures: String): String =
        "pinterface(${guid(interfaceIid)};${argumentSignatures.joinToString(";")})"
}
