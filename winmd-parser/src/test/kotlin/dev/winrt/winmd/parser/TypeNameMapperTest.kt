package dev.winrt.winmd.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class TypeNameMapperTest {
    private val mapper = TypeNameMapper()

    @Test
    fun maps_key_value_pair_to_kotlin_map_entry_type() {
        assertEquals(
            "kotlin.collections.Map.Entry<kotlin.String, microsoft.ui.xaml.UIElement>",
            mapper.mapTypeName(
                "Windows.Foundation.Collections.IKeyValuePair`2<String, Microsoft.UI.Xaml.UIElement>",
                "Windows.Foundation.Collections",
            ).toString(),
        )
    }

    @Test
    fun maps_dictionary_interfaces_to_kotlin_map_types() {
        assertEquals(
            "kotlin.collections.MutableMap<kotlin.String, microsoft.ui.xaml.UIElement>",
            mapper.mapTypeName(
                "Windows.Foundation.Collections.IMap`2<String, Microsoft.UI.Xaml.UIElement>",
                "Windows.Foundation.Collections",
            ).toString(),
        )
        assertEquals(
            "kotlin.collections.Map<kotlin.String, microsoft.ui.xaml.UIElement>",
            mapper.mapTypeName(
                "Windows.Foundation.Collections.IMapView`2<String, Microsoft.UI.Xaml.UIElement>",
                "Windows.Foundation.Collections",
            ).toString(),
        )
    }
}