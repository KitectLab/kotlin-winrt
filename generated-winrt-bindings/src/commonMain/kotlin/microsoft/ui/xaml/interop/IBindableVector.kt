package microsoft.ui.xaml.interop

import dev.winrt.core.Inspectable
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop
import kotlin.collections.MutableList

open class IBindableVector(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    MutableList<Inspectable> {
    fun getAt(index: UInt32): Inspectable =
        Inspectable(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 7, index.value).getOrThrow())

    override val size: Int
        get() = winRtSize.value.toInt()

    val winRtSize: UInt32
        get() =
        UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())

    fun getView(): IBindableVectorView =
        IBindableVectorView(PlatformComInterop.invokeObjectMethod(pointer, 9).getOrThrow())

    fun append(value: Inspectable) {
        PlatformComInterop.invokeObjectSetter(pointer, 14, value.pointer).getOrThrow()
    }

    fun removeAtEnd() {
        PlatformComInterop.invokeUnitMethod(pointer, 15).getOrThrow()
    }

    override fun clear() {
        PlatformComInterop.invokeUnitMethod(pointer, 16).getOrThrow()
    }

    fun first(): IBindableIterator =
        IBindableIterator(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

    private fun readAll(): MutableList<Inspectable> =
        MutableList(size) { index -> get(index) }

    private fun rewrite(items: List<Inspectable>) {
        clear()
        items.forEach { append(it) }
    }

    override fun add(element: Inspectable): Boolean {
        append(element)
        return true
    }

    override fun add(index: Int, element: Inspectable) {
        require(index in 0..size) { "index must be between 0 and size" }
        if (index == size) {
            append(element)
            return
        }
        val items = readAll()
        items.add(index, element)
        rewrite(items)
    }

    override fun addAll(index: Int, elements: Collection<Inspectable>): Boolean {
        require(index in 0..size) { "index must be between 0 and size" }
        if (elements.isEmpty()) return false
        val items = readAll()
        items.addAll(index, elements)
        rewrite(items)
        return true
    }

    override fun addAll(elements: Collection<Inspectable>): Boolean {
        if (elements.isEmpty()) return false
        elements.forEach { append(it) }
        return true
    }

    override fun contains(element: Inspectable): Boolean = indexOf(element) >= 0

    override fun containsAll(elements: Collection<Inspectable>): Boolean = elements.all { contains(it) }

    override fun get(index: Int): Inspectable {
        require(index >= 0) { "index must be non-negative" }
        return getAt(UInt32(index.toUInt()))
    }

    override fun isEmpty(): Boolean = size == 0

    override fun indexOf(element: Inspectable): Int {
        for (index in 0 until size) {
            if (get(index) == element) {
                return index
            }
        }
        return -1
    }

    override fun lastIndexOf(element: Inspectable): Int {
        for (index in size - 1 downTo 0) {
            if (get(index) == element) {
                return index
            }
        }
        return -1
    }

    override fun iterator(): MutableIterator<Inspectable> = listIterator()

    override fun listIterator(): MutableListIterator<Inspectable> = listIterator(0)

    override fun listIterator(index: Int): MutableListIterator<Inspectable> {
        require(index in 0..size) { "index must be between 0 and size" }
        return object : MutableListIterator<Inspectable> {
            private var current = index
            private var lastReturned = -1
            override fun hasNext(): Boolean = current < size
            override fun next(): Inspectable {
                check(hasNext()) { "No next element" }
                lastReturned = current
                return get(current++)
            }
            override fun hasPrevious(): Boolean = current > 0
            override fun previous(): Inspectable {
                check(hasPrevious()) { "No previous element" }
                current--
                lastReturned = current
                return get(current)
            }
            override fun nextIndex(): Int = current
            override fun previousIndex(): Int = current - 1
            override fun remove() {
                check(lastReturned >= 0) { "No element to remove" }
                this@IBindableVector.removeAt(lastReturned)
                if (lastReturned < current) current--
                lastReturned = -1
            }
            override fun set(element: Inspectable) {
                check(lastReturned >= 0) { "No element to set" }
                this@IBindableVector[lastReturned] = element
            }
            override fun add(element: Inspectable) {
                this@IBindableVector.add(current++, element)
                lastReturned = -1
            }
        }
    }

    override fun remove(element: Inspectable): Boolean {
        val index = indexOf(element)
        if (index < 0) return false
        removeAt(index)
        return true
    }

    override fun removeAll(elements: Collection<Inspectable>): Boolean {
        if (elements.isEmpty()) return false
        val items = readAll().filterNot { elements.contains(it) }
        if (items.size == size) return false
        rewrite(items)
        return true
    }

    override fun removeAt(index: Int): Inspectable {
        require(index in 0 until size) { "index must be between 0 and size" }
        val items = readAll()
        val removed = items.removeAt(index)
        rewrite(items)
        return removed
    }

    override fun retainAll(elements: Collection<Inspectable>): Boolean {
        val items = readAll().filter { elements.contains(it) }
        if (items.size == size) return false
        rewrite(items)
        return true
    }

    override fun set(index: Int, element: Inspectable): Inspectable {
        require(index in 0 until size) { "index must be between 0 and size" }
        val items = readAll()
        val previous = items[index]
        items[index] = element
        rewrite(items)
        return previous
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<Inspectable> {
        require(fromIndex in 0..toIndex && toIndex <= size) { "invalid subList range" }
        return readAll().subList(fromIndex, toIndex)
    }

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Interop.IBindableVector"
        override val projectionTypeKey: String = "System.Collections.IList"
        override val iid: Guid = guidOf("393de7de-6fd0-4c0d-bb71-47244a113e93")

        fun from(inspectable: Inspectable): IBindableVector =
            inspectable.projectInterface(this, ::IBindableVector)
    }
}
