package dev.winrt.winmd.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun maps_observable_collection_interfaces_to_kotlin_collection_types() {
        assertEquals(
            "kotlin.collections.MutableList<microsoft.ui.xaml.UIElement>",
            mapper.mapTypeName(
                "Windows.Foundation.Collections.IObservableVector`1<Microsoft.UI.Xaml.UIElement>",
                "Windows.Foundation.Collections",
            ).toString(),
        )
        assertEquals(
            "kotlin.collections.MutableMap<kotlin.String, microsoft.ui.xaml.UIElement>",
            mapper.mapTypeName(
                "Windows.Foundation.Collections.IObservableMap`2<String, Microsoft.UI.Xaml.UIElement>",
                "Windows.Foundation.Collections",
            ).toString(),
        )
    }

    @Test
    fun maps_nested_dictionary_interfaces_to_kotlin_map_types() {
        assertEquals(
            "kotlin.collections.Map<kotlin.String, windows.foundation.collections.IVectorView<microsoft.ui.xaml.UIElement>>",
            mapper.mapTypeName(
                "Windows.Foundation.Collections.IMapView`2<String, Windows.Foundation.Collections.IVectorView`1<Microsoft.UI.Xaml.UIElement>>",
                "Windows.Foundation.Collections",
            ).toString(),
        )
    }

    @Test
    fun maps_ireference_to_nullable_kotlin_type() {
        assertEquals(
            "kotlin.String?",
            mapper.mapTypeName(
                "Windows.Foundation.IReference`1<String>",
                "Windows.Foundation",
            ).toString(),
        )
        assertEquals(
            "dev.winrt.core.Inspectable?",
            mapper.mapTypeName(
                "Windows.Foundation.IReference`1<Object>",
                "Windows.Foundation",
            ).toString(),
        )
    }

    @Test
    fun maps_hresult_to_nullable_exception_type() {
        assertTrue(
            mapper.mapTypeName(
                "Windows.Foundation.HResult",
                "Windows.Foundation",
            ).toString().endsWith("Exception?"),
        )
        assertTrue(
            mapper.mapTypeName(
                "Windows.Foundation.IReference`1<Windows.Foundation.HResult>",
                "Windows.Foundation",
            ).toString().endsWith("Exception?"),
        )
    }
}
