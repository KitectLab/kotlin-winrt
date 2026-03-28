package microsoft.ui.xaml.interop

import dev.winrt.core.Inspectable
import dev.winrt.core.UInt32

class InspectableMutableList internal constructor(
    private val vector: IBindableVector,
) : AbstractMutableList<Inspectable>() {
    override val size: Int
        get() = vector.size.value.toInt()

    override fun get(index: Int): Inspectable {
        require(index >= 0) { "index must be non-negative" }
        return vector.getAt(UInt32(index.toUInt()))
    }

    override fun add(index: Int, element: Inspectable) {
        if (index != size) {
            throw UnsupportedOperationException("insert at arbitrary index is not implemented yet")
        }
        vector.append(element)
    }

    override fun set(index: Int, element: Inspectable): Inspectable {
        throw UnsupportedOperationException("set is not implemented yet")
    }

    override fun removeAt(index: Int): Inspectable {
        throw UnsupportedOperationException("removeAt is not implemented yet")
    }

    override fun clear() {
        vector.clear()
    }
}
