package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdProperty

internal fun synthesizePropertiesFromAccessorMethods(
    methods: List<WinMdMethod>,
    declaredPropertyNames: Set<String>,
): List<WinMdProperty> {
    val gettersByPropertyName = linkedMapOf<String, WinMdMethod>()
    val settersByPropertyName = linkedMapOf<String, WinMdMethod>()

    methods.asReversed().forEach { method ->
        when {
            method.name.startsWith("get_") && method.parameters.isEmpty() && method.vtableIndex != null ->
                gettersByPropertyName.putIfAbsent(method.name.removePrefix("get_"), method)
            method.name.startsWith("put_") && method.parameters.size == 1 && method.vtableIndex != null ->
                settersByPropertyName.putIfAbsent(method.name.removePrefix("put_"), method)
        }
    }

    return gettersByPropertyName.asSequence()
        .filterNot { (propertyName, _) -> propertyName in declaredPropertyNames }
        .mapNotNull { (propertyName, getterMethod) ->
            val setterMethod = settersByPropertyName[propertyName]
            if (setterMethod != null && setterMethod.parameters.single().type != getterMethod.returnType) {
                return@mapNotNull null
            }
            WinMdProperty(
                name = propertyName,
                type = getterMethod.returnType,
                mutable = setterMethod != null,
                getterVtableIndex = getterMethod.vtableIndex,
                setterVtableIndex = setterMethod?.vtableIndex,
            )
        }
        .toList()
}
