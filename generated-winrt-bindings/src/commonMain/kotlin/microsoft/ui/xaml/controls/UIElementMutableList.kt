package microsoft.ui.xaml.controls

import microsoft.ui.xaml.UIElement

class UIElementMutableList internal constructor(
    private val collection: UIElementCollection,
) : AbstractMutableList<UIElement>() {
    override val size: Int
        get() = collection.asIBindableVector().size.value.toInt()

    override fun get(index: Int): UIElement {
        require(index >= 0) { "index must be non-negative" }
        return collection.getAt(dev.winrt.core.UInt32(index.toUInt()))
    }

    override fun add(index: Int, element: UIElement) {
        if (index != size) {
            throw UnsupportedOperationException("insert at arbitrary index is not implemented yet")
        }
        collection.append(element)
    }

    override fun set(index: Int, element: UIElement): UIElement {
        throw UnsupportedOperationException("set is not implemented yet")
    }

    override fun removeAt(index: Int): UIElement {
        throw UnsupportedOperationException("removeAt is not implemented yet")
    }

    override fun clear() {
        collection.asIBindableVector().clear()
    }
}
