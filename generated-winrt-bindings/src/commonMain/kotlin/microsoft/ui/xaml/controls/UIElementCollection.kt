package microsoft.ui.xaml.controls

import dev.winrt.core.RuntimeClassId
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtActivationKind
import dev.winrt.core.WinRtRuntime
import dev.winrt.core.WinRtRuntimeClassMetadata
import dev.winrt.kom.ComPtr
import microsoft.ui.xaml.UIElement
import microsoft.ui.xaml.interop.IBindableVector
import kotlin.collections.MutableList

open class UIElementCollection(
    pointer: ComPtr,
) : dev.winrt.core.Inspectable(pointer),
    MutableList<UIElement> {
    constructor() : this(Companion.activate().pointer)

    // WinUI exposes collection mutators through IBindableVector on this runtime class.
    private val bindableVector: IBindableVector
        get() = IBindableVector.from(this)

    fun getAt(index: UInt32): UIElement =
        UIElement(bindableVector.getAt(index).pointer)

    fun append(value: UIElement) {
        bindableVector.append(value)
    }

    override val size: Int
        get() = bindableVector.size

    override fun get(index: Int): UIElement {
        require(index >= 0) { "index must be non-negative" }
        return getAt(UInt32(index.toUInt()))
    }

    private fun readAll(): MutableList<UIElement> =
        MutableList(size) { index -> get(index) }

    private fun rewrite(items: List<UIElement>) {
        clear()
        items.forEach { append(it) }
    }

    override fun add(element: UIElement): Boolean {
        append(element)
        return true
    }

    override fun add(index: Int, element: UIElement) {
        require(index in 0..size) { "index must be between 0 and size" }
        if (index == size) {
            append(element)
            return
        }
        val items = readAll()
        items.add(index, element)
        rewrite(items)
    }

    override fun addAll(index: Int, elements: Collection<UIElement>): Boolean {
        require(index in 0..size) { "index must be between 0 and size" }
        if (elements.isEmpty()) return false
        val items = readAll()
        items.addAll(index, elements)
        rewrite(items)
        return true
    }

    override fun addAll(elements: Collection<UIElement>): Boolean {
        if (elements.isEmpty()) return false
        elements.forEach { append(it) }
        return true
    }

    override fun clear() {
        bindableVector.clear()
    }

    override fun contains(element: UIElement): Boolean = indexOf(element) >= 0

    override fun containsAll(elements: Collection<UIElement>): Boolean = elements.all { contains(it) }

    override fun indexOf(element: UIElement): Int {
        for (index in 0 until size) {
            if (get(index) == element) {
                return index
            }
        }
        return -1
    }

    override fun isEmpty(): Boolean = size == 0

    override fun lastIndexOf(element: UIElement): Int {
        for (index in size - 1 downTo 0) {
            if (get(index) == element) {
                return index
            }
        }
        return -1
    }

    override fun iterator(): MutableIterator<UIElement> = listIterator()

    override fun listIterator(): MutableListIterator<UIElement> = listIterator(0)

    override fun listIterator(index: Int): MutableListIterator<UIElement> {
        require(index in 0..size) { "index must be between 0 and size" }
        return object : MutableListIterator<UIElement> {
            private var current = index
            private var lastReturned = -1
            override fun hasNext(): Boolean = current < size
            override fun next(): UIElement {
                check(hasNext()) { "No next element" }
                lastReturned = current
                return get(current++)
            }
            override fun hasPrevious(): Boolean = current > 0
            override fun previous(): UIElement {
                check(hasPrevious()) { "No previous element" }
                current--
                lastReturned = current
                return get(current)
            }
            override fun nextIndex(): Int = current
            override fun previousIndex(): Int = current - 1
            override fun remove() {
                check(lastReturned >= 0) { "No element to remove" }
                this@UIElementCollection.removeAt(lastReturned)
                if (lastReturned < current) current--
                lastReturned = -1
            }
            override fun set(element: UIElement) {
                check(lastReturned >= 0) { "No element to set" }
                this@UIElementCollection[lastReturned] = element
            }
            override fun add(element: UIElement) {
                this@UIElementCollection.add(current++, element)
                lastReturned = -1
            }
        }
    }

    override fun remove(element: UIElement): Boolean {
        val index = indexOf(element)
        if (index < 0) return false
        removeAt(index)
        return true
    }

    override fun removeAll(elements: Collection<UIElement>): Boolean {
        if (elements.isEmpty()) return false
        val items = readAll().filterNot { elements.contains(it) }
        if (items.size == size) return false
        rewrite(items)
        return true
    }

    override fun removeAt(index: Int): UIElement {
        require(index in 0 until size) { "index must be between 0 and size" }
        val items = readAll()
        val removed = items.removeAt(index)
        rewrite(items)
        return removed
    }

    override fun retainAll(elements: Collection<UIElement>): Boolean {
        val items = readAll().filter { elements.contains(it) }
        if (items.size == size) return false
        rewrite(items)
        return true
    }

    override fun set(index: Int, element: UIElement): UIElement {
        require(index in 0 until size) { "index must be between 0 and size" }
        val items = readAll()
        val previous = items[index]
        items[index] = element
        rewrite(items)
        return previous
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<UIElement> {
        require(fromIndex in 0..toIndex && toIndex <= size) { "invalid subList range" }
        return readAll().subList(fromIndex, toIndex)
    }

    companion object : WinRtRuntimeClassMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Controls.UIElementCollection"
        override val classId: RuntimeClassId = RuntimeClassId("Microsoft.UI.Xaml.Controls", "UIElementCollection")
        override val defaultInterfaceName: String? = "Microsoft.UI.Xaml.Controls.IUIElementCollection"
        override val activationKind: WinRtActivationKind = WinRtActivationKind.Factory

        fun activate(): UIElementCollection = WinRtRuntime.activate(this, ::UIElementCollection)
    }
}
