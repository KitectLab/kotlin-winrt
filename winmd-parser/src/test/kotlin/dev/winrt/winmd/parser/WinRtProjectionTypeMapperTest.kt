package dev.winrt.winmd.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class WinRtProjectionTypeMapperTest {
    private val mapper = WinRtProjectionTypeMapper()

    @Test
    fun maps_runtime_class_projection_key_to_identity_by_default() {
        assertEquals(
            "Microsoft.UI.Xaml.UIElement",
            mapper.projectionTypeKeyFor("Microsoft.UI.Xaml.UIElement", "Microsoft.UI.Xaml"),
        )
    }

    @Test
    fun maps_generic_collection_projection_key_with_nested_runtime_class_argument() {
        assertEquals(
            "kotlin.collections.MutableList<Microsoft.UI.Xaml.UIElement>",
            mapper.projectionTypeKeyFor(
                "Windows.Foundation.Collections.IVector`1<Microsoft.UI.Xaml.UIElement>",
                "Microsoft.UI.Xaml.Controls",
            ),
        )
    }

    @Test
    fun maps_generic_collection_projection_keys_without_backtick_raw_names() {
        assertEquals(
            "kotlin.collections.Iterable<String>",
            mapper.projectionTypeKeyFor(
                "Windows.Foundation.Collections.IIterable<String>",
                "Windows.Foundation.Collections",
            ),
        )
        assertEquals(
            "kotlin.collections.List<String>",
            mapper.projectionTypeKeyFor(
                "Windows.Foundation.Collections.IVectorView<String>",
                "Windows.Foundation.Collections",
            ),
        )
    }

    @Test
    fun preserves_scalar_arguments_in_generic_projection_keys() {
        assertEquals(
            "kotlin.collections.List<String>",
            mapper.projectionTypeKeyFor(
                "Windows.Foundation.Collections.IVectorView`1<String>",
                "Windows.Foundation.Collections",
            ),
        )
    }

    @Test
    fun maps_dictionary_projection_keys_to_kotlin_collection_interfaces() {
        assertEquals(
            "kotlin.collections.MutableMap<String, Microsoft.UI.Xaml.UIElement>",
            mapper.projectionTypeKeyFor(
                "Windows.Foundation.Collections.IMap`2<String, Microsoft.UI.Xaml.UIElement>",
                "Windows.Foundation.Collections",
            ),
        )
        assertEquals(
            "kotlin.collections.Map<String, Microsoft.UI.Xaml.UIElement>",
            mapper.projectionTypeKeyFor(
                "Windows.Foundation.Collections.IMapView`2<String, Microsoft.UI.Xaml.UIElement>",
                "Windows.Foundation.Collections",
            ),
        )
    }

    @Test
    fun maps_observable_collection_projection_keys_to_kotlin_collection_interfaces() {
        assertEquals(
            "kotlin.collections.MutableList<Microsoft.UI.Xaml.UIElement>",
            mapper.projectionTypeKeyFor(
                "Windows.Foundation.Collections.IObservableVector`1<Microsoft.UI.Xaml.UIElement>",
                "Windows.Foundation.Collections",
            ),
        )
        assertEquals(
            "kotlin.collections.MutableMap<String, Microsoft.UI.Xaml.UIElement>",
            mapper.projectionTypeKeyFor(
                "Windows.Foundation.Collections.IObservableMap`2<String, Microsoft.UI.Xaml.UIElement>",
                "Windows.Foundation.Collections",
            ),
        )
    }

    @Test
    fun preserves_nested_generic_arguments_in_dictionary_projection_keys() {
        assertEquals(
            "kotlin.collections.MutableMap<String, kotlin.collections.List<Microsoft.UI.Xaml.UIElement>>",
            mapper.projectionTypeKeyFor(
                "Windows.Foundation.Collections.IMap`2<String, Windows.Foundation.Collections.IVectorView`1<Microsoft.UI.Xaml.UIElement>>",
                "Windows.Foundation.Collections",
            ),
        )
        assertEquals(
            "kotlin.collections.Map<String, kotlin.collections.Map.Entry<String, Microsoft.UI.Xaml.UIElement>>",
            mapper.projectionTypeKeyFor(
                "Windows.Foundation.Collections.IMapView`2<String, Windows.Foundation.Collections.IKeyValuePair`2<String, Microsoft.UI.Xaml.UIElement>>",
                "Windows.Foundation.Collections",
            ),
        )
    }

    @Test
    fun maps_key_value_pair_projection_keys_to_kotlin_map_entry() {
        assertEquals(
            "kotlin.collections.Map.Entry<String, Microsoft.UI.Xaml.UIElement>",
            mapper.projectionTypeKeyFor(
                "Windows.Foundation.Collections.IKeyValuePair`2<String, Microsoft.UI.Xaml.UIElement>",
                "Windows.Foundation.Collections",
            ),
        )
    }

    @Test
    fun maps_hresult_projection_keys_to_exception() {
        assertEquals(
            "Exception",
            mapper.projectionTypeKeyFor("Windows.Foundation.HResult", "Windows.Foundation"),
        )
        assertEquals(
            "Windows.Foundation.IReference`1<Exception>",
            mapper.projectionTypeKeyFor(
                "Windows.Foundation.IReference`1<Windows.Foundation.HResult>",
                "Windows.Foundation",
            ),
        )
    }
}
