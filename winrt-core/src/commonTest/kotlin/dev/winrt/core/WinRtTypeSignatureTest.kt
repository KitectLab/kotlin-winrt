package dev.winrt.core

import kotlin.test.Test
import kotlin.test.assertEquals

class WinRtTypeSignatureTest {
    @Test
    fun builds_runtime_class_signature_using_default_interface_signature() {
        assertEquals(
            "rc(Microsoft.UI.Xaml.Controls.UIElementCollection;{23050cb1-db88-54ed-9083-5ecfb12512fd})",
            WinRtTypeSignature.runtimeClass(
                "Microsoft.UI.Xaml.Controls.UIElementCollection",
                WinRtTypeSignature.guid("23050cb1-db88-54ed-9083-5ecfb12512fd"),
            ),
        )
    }

    @Test
    fun builds_parameterized_interface_signature() {
        assertEquals(
            "pinterface({913337e9-11a1-4345-a3a2-4e7f956e222d};string)",
            WinRtTypeSignature.parameterizedInterface(
                "913337e9-11a1-4345-a3a2-4e7f956e222d",
                WinRtTypeSignature.string(),
            ),
        )
    }

    @Test
    fun builds_parameterized_interface_signature_for_runtime_class_argument() {
        assertEquals(
            "pinterface({61c17706-2d65-11e0-9ae8-d48564015472};rc(Microsoft.UI.Xaml.UIElement;{00000000-0000-0000-0000-000000000000}))",
            WinRtTypeSignature.parameterizedInterface(
                "61c17706-2d65-11e0-9ae8-d48564015472",
                WinRtTypeSignature.runtimeClass(
                    "Microsoft.UI.Xaml.UIElement",
                    WinRtTypeSignature.guid("00000000-0000-0000-0000-000000000000"),
                ),
            ),
        )
    }
}
