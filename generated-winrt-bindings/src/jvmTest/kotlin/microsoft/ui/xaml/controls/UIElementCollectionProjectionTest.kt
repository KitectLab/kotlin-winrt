package microsoft.ui.xaml.controls

import dev.winrt.core.JvmWinRtObjectStub
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr
import dev.winrt.kom.HResult
import microsoft.ui.xaml.UIElement
import microsoft.ui.xaml.interop.IBindableVector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UIElementCollectionProjectionTest {
    @Test
    fun append_projects_to_bindable_vector_before_invoking_append_slot() {
        val primaryIid = guidOf("11111111-2222-3333-4444-555555555555")
        val childIid = guidOf("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
        var appendCalled = false
        var appendedPointer = ComPtr.NULL

        JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(iid = childIid),
        ).use { childStub ->
            JvmWinRtObjectStub.create(
                JvmWinRtObjectStub.InterfaceSpec(iid = primaryIid),
                JvmWinRtObjectStub.InterfaceSpec(
                    iid = IBindableVector.iid,
                    objectArgUnitMethods = mapOf(
                        14 to { arg ->
                            appendCalled = true
                            appendedPointer = arg
                            HResult(0)
                        },
                    ),
                ),
            ).use { collectionStub ->
                val collection = UIElementCollection(collectionStub.primaryPointer)

                collection.append(UIElement(childStub.primaryPointer))

                assertTrue(appendCalled)
                assertEquals(childStub.primaryPointer.value.rawValue, appendedPointer.value.rawValue)
            }
        }
    }
}
