package dev.winrt.core

import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectedObjectArgumentTest {
    @Test
    fun uses_raw_interface_guid_for_interface_signatures() {
        val expected = guidOf("11111111-2222-3333-4444-555555555555")

        val subject = recordingInspectable()
        projectedObjectArgumentPointer(
            value = subject,
            projectionTypeKey = "Test.IWidget",
            signature = "{11111111-2222-3333-4444-555555555555}",
        )

        assertEquals(expected.toString(), subject.queriedIid?.toString())
    }

    @Test
    fun uses_iinspectable_for_object_signatures() {
        val subject = recordingInspectable()
        projectedObjectArgumentPointer(
            value = subject,
            projectionTypeKey = "Object",
            signature = WinRtTypeSignature.object_(),
        )

        assertEquals(Inspectable.iinspectableIid.toString(), subject.queriedIid?.toString())
    }

    @Test
    fun uses_default_interface_guid_for_runtime_class_signatures() {
        val expected = guidOf("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")

        val subject = recordingInspectable()
        projectedObjectArgumentPointer(
            value = subject,
            projectionTypeKey = "Windows.Foundation.Uri",
            signature = "rc(Windows.Foundation.Uri;{aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee})",
        )

        assertEquals(expected.toString(), subject.queriedIid?.toString())
    }

    @Test
    fun uses_parameterized_hash_for_parameterized_interface_signatures() {
        val signature = WinRtTypeSignature.parameterizedInterface(
            "faa585ea-6214-4217-afda-7f46de5869b3",
            WinRtTypeSignature.string(),
        )
        val expected = ParameterizedInterfaceId.createFromSignature(signature)

        val subject = recordingInspectable()
        projectedObjectArgumentPointer(
            value = subject,
            projectionTypeKey = "kotlin.collections.Iterable<String>",
            signature = signature,
        )

        assertEquals(expected.toString(), subject.queriedIid?.toString())
    }

    private fun recordingInspectable(): RecordingInspectable = RecordingInspectable()

    private class RecordingInspectable : Inspectable(ComPtr.NULL) {
        var queriedIid: Guid? = null

        override fun queryInterface(iid: Guid): ComPtr {
            queriedIid = iid
            return ComPtr.NULL
        }
    }
}
