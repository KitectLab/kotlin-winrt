package dev.winrt.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ParameterizedInterfaceIdTest {
    @Test
    fun creates_winrt_pinterface_guid_from_signature() {
        assertEquals(
            "a4d92b05-f965-5e3c-b9e1-401ff306ba93",
            ParameterizedInterfaceId.createFromSignature(
                "pinterface({913337e9-11a1-4345-a3a2-4e7f956e222d};string)",
            ).toString(),
        )
    }

    @Test
    fun creates_winrt_runtime_class_signature_guid() {
        assertEquals(
            "d0faae99-fb94-5270-97ab-7f78af83ebf1",
            ParameterizedInterfaceId.createFromSignature(
                "rc(Microsoft.UI.Xaml.Controls.UIElementCollection;{23050cb1-db88-54ed-9083-5ecfb12512fd})",
            ).toString(),
        )
    }
}
