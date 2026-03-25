package dev.winrt.winmd.plugin

object InterfaceVtableResolver {
    private const val inspectableMethodCount = 6

    fun inferMethodSlot(type: WinMdType, model: WinMdModel, method: WinMdMethod): Int? {
        if (type.kind != WinMdTypeKind.Interface) {
            return null
        }
        val ownMethodIndex = type.methods.indexOfFirst { it.signatureKey == method.signatureKey }
        if (ownMethodIndex < 0) {
            return null
        }
        return inspectableMethodCount + ownMethodIndex
    }
}
