package microsoft.ui.xaml.controls

import dev.winrt.kom.ComPtr
import microsoft.ui.xaml.UIElement
import kotlin.test.Test
import kotlin.test.assertSame

class UIElementMutableListTest {
    @Test
    fun ui_element_collection_projects_to_cached_mutable_list() {
        val collection = UIElementCollection(ComPtr.NULL)

        val first: MutableList<UIElement> = collection.asMutableList()
        val second = collection.asMutableList()

        assertSame(first, second)
    }
}
