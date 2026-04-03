package microsoft.ui.xaml.controls

import dev.winrt.core.JvmWinRtObjectStub
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr
import dev.winrt.kom.HResult
import microsoft.ui.xaml.UIElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UIElementCollectionProjectionTest {
    @Test
    fun append_invokes_ui_element_collection_append_slot_on_primary_pointer() {
        val primaryIid = guidOf("11111111-2222-3333-4444-555555555555")
        val childIid = guidOf("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
        var appendCalled = false
        var appendedPointer = ComPtr.NULL

        JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(
                iid = primaryIid,
                objectArgUnitMethods = mapOf(
                    13 to { arg ->
                        appendCalled = true
                        appendedPointer = arg
                        HResult(0)
                    },
                ),
            ),
        ).use { collectionStub ->
            JvmWinRtObjectStub.create(
                JvmWinRtObjectStub.InterfaceSpec(iid = childIid),
            ).use { childStub ->
                val collection = UIElementCollection(collectionStub.primaryPointer)

                collection.append(UIElement(childStub.primaryPointer))

                assertTrue(appendCalled)
                assertEquals(childStub.primaryPointer.value.rawValue, appendedPointer.value.rawValue)
            }
        }
    }
}
