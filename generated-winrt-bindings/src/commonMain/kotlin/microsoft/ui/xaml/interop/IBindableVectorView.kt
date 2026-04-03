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
import kotlin.collections.AbstractCollection

open class IBindableVectorView(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    List<Inspectable> {
    fun getAt(index: UInt32): Inspectable =
        Inspectable(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 7, index.value).getOrThrow())

    val winRtSize: UInt32
        get() = UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())

    fun first(): IBindableIterator =
        IBindableIterator(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

    override val size: Int
        get() = winRtSize.value.toInt()

    override fun contains(element: Inspectable): Boolean = indexOf(element) >= 0

    override fun containsAll(elements: Collection<Inspectable>): Boolean = elements.all { contains(it) }

    override fun get(index: Int): Inspectable {
        require(index >= 0) { "index must be non-negative" }
        return getAt(UInt32(index.toUInt()))
    }

    override fun indexOf(element: Inspectable): Int {
        for (index in 0 until size) {
            if (get(index) == element) {
                return index
            }
        }
        return -1
    }

    override fun isEmpty(): Boolean = size == 0

    override fun iterator(): Iterator<Inspectable> = object : Iterator<Inspectable> {
        private var index = 0
        override fun hasNext(): Boolean = index < size
        override fun next(): Inspectable = get(index++)
    }

    override fun lastIndexOf(element: Inspectable): Int {
        for (index in size - 1 downTo 0) {
            if (get(index) == element) {
                return index
            }
        }
        return -1
    }

    override fun listIterator(): ListIterator<Inspectable> = listIterator(0)

    override fun listIterator(index: Int): ListIterator<Inspectable> = object : ListIterator<Inspectable> {
        private var current = index
        override fun hasNext(): Boolean = current < size
        override fun next(): Inspectable = get(current++)
        override fun hasPrevious(): Boolean = current > 0
        override fun previous(): Inspectable = get(--current)
        override fun nextIndex(): Int = current
        override fun previousIndex(): Int = current - 1
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<Inspectable> =
        (fromIndex until toIndex).map { get(it) }

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Interop.IBindableVectorView"
        override val projectionTypeKey: String = "kotlin.collections.List"
        override val iid: Guid = guidOf("346dd6e7-976e-4bc3-815d-ece243bc0f33")

        fun from(inspectable: Inspectable): IBindableVectorView =
            inspectable.projectInterface(this, ::IBindableVectorView)
    }
}
