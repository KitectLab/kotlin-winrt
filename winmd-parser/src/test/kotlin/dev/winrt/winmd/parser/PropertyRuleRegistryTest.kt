package dev.winrt.winmd.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class PropertyRuleRegistryTest {
    @Test
    fun classifies_runtime_property_getter_rules() {
        assertEquals(RuntimePropertyGetterRuleFamily.OBJECT, PropertyRuleRegistry.getterRuleFamily("Object"))
        assertEquals(RuntimePropertyGetterRuleFamily.IREFERENCE_STRING, PropertyRuleRegistry.getterRuleFamily("IReference<String>"))
        assertEquals(RuntimePropertyGetterRuleFamily.STRING, PropertyRuleRegistry.getterRuleFamily("String"))
        assertEquals(RuntimePropertyGetterRuleFamily.BOOLEAN, PropertyRuleRegistry.getterRuleFamily("Boolean"))
        assertEquals(RuntimePropertyGetterRuleFamily.GUID, PropertyRuleRegistry.getterRuleFamily("Guid"))
        assertEquals(RuntimePropertyGetterRuleFamily.DATE_TIME, PropertyRuleRegistry.getterRuleFamily("DateTime"))
        assertEquals(RuntimePropertyGetterRuleFamily.TIME_SPAN, PropertyRuleRegistry.getterRuleFamily("TimeSpan"))
        assertEquals(RuntimePropertyGetterRuleFamily.EVENT_REGISTRATION_TOKEN, PropertyRuleRegistry.getterRuleFamily("EventRegistrationToken"))
        assertEquals(RuntimePropertyGetterRuleFamily.INT32, PropertyRuleRegistry.getterRuleFamily("Int32"))
        assertEquals(RuntimePropertyGetterRuleFamily.UINT32, PropertyRuleRegistry.getterRuleFamily("UInt32"))
        assertEquals(RuntimePropertyGetterRuleFamily.INT64, PropertyRuleRegistry.getterRuleFamily("Int64"))
        assertEquals(RuntimePropertyGetterRuleFamily.UINT64, PropertyRuleRegistry.getterRuleFamily("UInt64"))
    }

    @Test
    fun classifies_runtime_property_setter_rules() {
        assertEquals(RuntimePropertySetterRuleFamily.OBJECT, PropertyRuleRegistry.setterRuleFamily("Object"))
        assertEquals(RuntimePropertySetterRuleFamily.STRING, PropertyRuleRegistry.setterRuleFamily("String"))
        assertEquals(RuntimePropertySetterRuleFamily.BOOLEAN, PropertyRuleRegistry.setterRuleFamily("Boolean"))
        assertEquals(RuntimePropertySetterRuleFamily.FLOAT32, PropertyRuleRegistry.setterRuleFamily("Float32"))
        assertEquals(RuntimePropertySetterRuleFamily.FLOAT64, PropertyRuleRegistry.setterRuleFamily("Float64"))
        assertEquals(RuntimePropertySetterRuleFamily.INT32, PropertyRuleRegistry.setterRuleFamily("Int32"))
        assertEquals(RuntimePropertySetterRuleFamily.UINT32, PropertyRuleRegistry.setterRuleFamily("UInt32"))
        assertEquals(RuntimePropertySetterRuleFamily.INT64, PropertyRuleRegistry.setterRuleFamily("Int64"))
        assertEquals(RuntimePropertySetterRuleFamily.UINT64, PropertyRuleRegistry.setterRuleFamily("UInt64"))
    }

    @Test
    fun classifies_interface_property_rules() {
        assertEquals(
            InterfacePropertyRuleFamily.ENUM,
            PropertyRuleRegistry.interfaceGetterRuleFamily("AsyncStatus", isEnumType = true, isObjectType = false),
        )
        assertEquals(
            InterfacePropertyRuleFamily.OBJECT,
            PropertyRuleRegistry.interfaceGetterRuleFamily("JsonObject", isEnumType = false, isObjectType = true),
        )
        assertEquals(
            InterfacePropertyRuleFamily.STRING,
            PropertyRuleRegistry.interfaceGetterRuleFamily("String", isEnumType = false, isObjectType = false),
        )
        assertEquals(
            InterfacePropertyRuleFamily.STRING,
            PropertyRuleRegistry.interfaceSetterRuleFamily("String", isObjectType = false),
        )
        assertEquals(
            InterfacePropertyRuleFamily.OBJECT,
            PropertyRuleRegistry.interfaceSetterRuleFamily("JsonObject", isObjectType = true),
        )
    }
}
