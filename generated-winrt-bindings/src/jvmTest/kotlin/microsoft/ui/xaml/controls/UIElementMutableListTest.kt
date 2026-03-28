package microsoft.ui.xaml.controls

import dev.winrt.kom.ComPtr
import microsoft.ui.xaml.UIElement
import kotlin.test.Test
import kotlin.test.assertSame

class UIElementMutableListTest {
    @Test
    fun ui_element_collection_implements_mutable_list() {
        val collection = UIElementCollection(ComPtr.NULL)

        val first: MutableList<UIElement> = collection
        val second: MutableList<UIElement> = collection

        assertSame(first, second)
    }
}
