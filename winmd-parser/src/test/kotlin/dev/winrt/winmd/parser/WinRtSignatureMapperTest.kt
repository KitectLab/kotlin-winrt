package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel
import dev.winrt.winmd.plugin.WinMdNamespace
import dev.winrt.winmd.plugin.WinMdType
import dev.winrt.winmd.plugin.WinMdTypeKind
import org.junit.Assert.assertEquals
import org.junit.Test

class WinRtSignatureMapperTest {
    private val model = WinMdModel(
        files = emptyList(),
        namespaces = listOf(
            WinMdNamespace(
                name = "Windows.Foundation.Collections",
                types = listOf(
                    WinMdType(
                        namespace = "Windows.Foundation.Collections",
                        name = "IVector`1",
                        kind = WinMdTypeKind.Interface,
                        guid = "913337e9-11a1-4345-a3a2-4e7f956e222d",
                        genericParameters = listOf("T"),
                    ),
                ),
            ),
            WinMdNamespace(
                name = "Microsoft.UI.Xaml",
                types = listOf(
                    WinMdType(
                        namespace = "Microsoft.UI.Xaml",
                        name = "DependencyObject",
                        kind = WinMdTypeKind.RuntimeClass,
                        defaultInterface = "Microsoft.UI.Xaml.IDependencyObject",
                    ),
                    WinMdType(
                        namespace = "Microsoft.UI.Xaml",
                        name = "IDependencyObject",
                        kind = WinMdTypeKind.Interface,
                        guid = "11111111-1111-1111-1111-111111111111",
                    ),
                    WinMdType(
                        namespace = "Microsoft.UI.Xaml",
                        name = "UIElement",
                        kind = WinMdTypeKind.RuntimeClass,
                        baseClass = "Microsoft.UI.Xaml.DependencyObject",
                        defaultInterface = "Microsoft.UI.Xaml.IUIElement",
                    ),
                    WinMdType(
                        namespace = "Microsoft.UI.Xaml",
                        name = "IUIElement",
                        kind = WinMdTypeKind.Interface,
                        guid = "22222222-2222-2222-2222-222222222222",
                    ),
                ),
            ),
        ),
    )

    private val mapper = WinRtSignatureMapper(TypeRegistry(model))
    private val projectionTypeMapper = WinRtProjectionTypeMapper()

    @Test
    fun maps_runtime_class_to_rc_signature_using_default_interface() {
        assertEquals(
            "rc(Microsoft.UI.Xaml.UIElement;{22222222-2222-2222-2222-222222222222})",
            mapper.signatureFor("Microsoft.UI.Xaml.UIElement", "Microsoft.UI.Xaml"),
        )
    }

    @Test
    fun maps_specialized_generic_interface_to_pinterface_signature() {
        assertEquals(
            "pinterface({913337e9-11a1-4345-a3a2-4e7f956e222d};rc(Microsoft.UI.Xaml.UIElement;{22222222-2222-2222-2222-222222222222}))",
            mapper.signatureFor(
                "Windows.Foundation.Collections.IVector`1<Microsoft.UI.Xaml.UIElement>",
                "Microsoft.UI.Xaml.Controls",
            ),
        )
    }

    @Test
    fun computes_parameterized_interface_iid_from_specialized_signature() {
        assertEquals(
            "344089a3-ea12-587e-aa3f-cfefad439688",
            mapper.interfaceIdFor(
                "Windows.Foundation.Collections.IVector`1<Microsoft.UI.Xaml.UIElement>",
                "Microsoft.UI.Xaml.Controls",
            ),
        )
    }

    @Test
    fun maps_bindable_vector_to_cswinrt_projection_type_key() {
        assertEquals(
            "System.Collections.IList",
            projectionTypeMapper.projectionTypeKeyFor(
                "Microsoft.UI.Xaml.Interop.IBindableVector",
                "Microsoft.UI.Xaml.Interop",
            ),
        )
    }

    @Test
    fun maps_specialized_vector_to_generic_list_projection_type_key() {
        assertEquals(
            "System.Collections.Generic.IList<Microsoft.UI.Xaml.UIElement>",
            projectionTypeMapper.projectionTypeKeyFor(
                "Windows.Foundation.Collections.IVector`1<Microsoft.UI.Xaml.UIElement>",
                "Microsoft.UI.Xaml.Controls",
            ),
        )
    }
}
