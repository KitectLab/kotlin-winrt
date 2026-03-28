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
        if (type.baseInterfaces.isEmpty()) {
            return 0
        }
        return type.baseInterfaces.sumOf { baseInterfaceName ->
            val rawName = baseInterfaceName.substringBefore('<')
            val baseInterface = model.namespaces
                .asSequence()
                .flatMap { namespace -> namespace.types.asSequence() }
                .firstOrNull { candidate ->
                    candidate.kind == WinMdTypeKind.Interface &&
                        "${candidate.namespace}.${candidate.name}" == rawName
                } ?: return@sumOf 0
            baseInterface.methods.size + inheritedMethodCount(baseInterface, model)
        }
    }
}
