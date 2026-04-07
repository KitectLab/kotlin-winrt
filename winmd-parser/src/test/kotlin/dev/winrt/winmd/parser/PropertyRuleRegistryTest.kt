package dev.winrt.winmd.parser

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PropertyRuleRegistryTest {
    @Test
    fun resolves_runtime_property_getter_descriptors() {
        assertNotNull(PropertyRuleRegistry.runtimeGetterDescriptor("Object"))
        assertNull(PropertyRuleRegistry.runtimeGetterDescriptor("IReference<String>"))
        assertNotNull(PropertyRuleRegistry.runtimeGetterDescriptor("String"))
        assertNotNull(PropertyRuleRegistry.runtimeGetterDescriptor("Boolean"))
        assertNotNull(PropertyRuleRegistry.runtimeGetterDescriptor("Windows.Foundation.HResult"))
        assertNotNull(PropertyRuleRegistry.runtimeGetterDescriptor("Guid"))
        assertNotNull(PropertyRuleRegistry.runtimeGetterDescriptor("DateTime"))
        assertNotNull(PropertyRuleRegistry.runtimeGetterDescriptor("TimeSpan"))
        assertNotNull(PropertyRuleRegistry.runtimeGetterDescriptor("EventRegistrationToken"))
        assertNotNull(PropertyRuleRegistry.runtimeGetterDescriptor("Int32"))
        assertNotNull(PropertyRuleRegistry.runtimeGetterDescriptor("UInt32"))
        assertNotNull(PropertyRuleRegistry.runtimeGetterDescriptor("Int64"))
        assertNotNull(PropertyRuleRegistry.runtimeGetterDescriptor("UInt64"))
    }

    @Test
    fun resolves_runtime_property_setter_descriptors() {
        assertNotNull(PropertyRuleRegistry.runtimeSetterDescriptor("Object"))
        assertNotNull(PropertyRuleRegistry.runtimeSetterDescriptor("String"))
        assertNotNull(PropertyRuleRegistry.runtimeSetterDescriptor("Boolean"))
        assertNotNull(PropertyRuleRegistry.runtimeSetterDescriptor("Float32"))
        assertNotNull(PropertyRuleRegistry.runtimeSetterDescriptor("Float64"))
        assertNotNull(PropertyRuleRegistry.runtimeSetterDescriptor("Windows.Foundation.HResult"))
        assertNotNull(PropertyRuleRegistry.runtimeSetterDescriptor("Int32"))
        assertNotNull(PropertyRuleRegistry.runtimeSetterDescriptor("UInt32"))
        assertNotNull(PropertyRuleRegistry.runtimeSetterDescriptor("Int64"))
        assertNotNull(PropertyRuleRegistry.runtimeSetterDescriptor("UInt64"))
    }

    @Test
    fun resolves_interface_scalar_property_descriptors() {
        assertNull(PropertyRuleRegistry.interfaceScalarGetterDescriptor("JsonObject"))
        assertNotNull(PropertyRuleRegistry.interfaceScalarGetterDescriptor("String"))
        assertNotNull(PropertyRuleRegistry.interfaceScalarSetterDescriptor("String"))
        assertNotNull(PropertyRuleRegistry.interfaceScalarGetterDescriptor("Windows.Foundation.HResult"))
        assertNotNull(PropertyRuleRegistry.interfaceScalarSetterDescriptor("Windows.Foundation.HResult"))
        assertNull(PropertyRuleRegistry.interfaceScalarSetterDescriptor("JsonObject"))
    }
}
