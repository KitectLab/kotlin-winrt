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
        return inspectableMethodCount + inheritedMethodCount(type, model) + ownMethodIndex
    }

    private fun inheritedMethodCount(type: WinMdType, model: WinMdModel): Int {
        return type.baseInterfaces.sumOf { baseInterfaceName ->
            val baseType = model.findType(baseInterfaceName) ?: return@sumOf 0
            baseType.methods.size + inheritedMethodCount(baseType, model)
        }
    }

    private fun WinMdModel.findType(qualifiedName: String): WinMdType? {
        val lastDot = qualifiedName.lastIndexOf('.')
        if (lastDot <= 0 || lastDot >= qualifiedName.length - 1) {
            return null
        }
        val namespace = qualifiedName.substring(0, lastDot)
        val name = qualifiedName.substring(lastDot + 1)
        return namespaces.firstOrNull { it.name == namespace }?.types?.firstOrNull { it.name == name }
    }
}
