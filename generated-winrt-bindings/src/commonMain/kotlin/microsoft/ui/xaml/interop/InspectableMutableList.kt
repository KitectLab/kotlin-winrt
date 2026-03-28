package microsoft.ui.xaml.interop

import dev.winrt.core.Inspectable
import dev.winrt.core.UInt32

class InspectableMutableList internal constructor(
    private val vector: IBindableVector,
) {
    val size: Int
        get() = vector.size.value.toInt()

    operator fun get(index: Int): Inspectable {
        require(index >= 0) { "index must be non-negative" }
        return vector.getAt(UInt32(index.toUInt()))
    }

    fun add(value: Inspectable): Int {
        vector.append(value)
        return size - 1
    }

    fun clear() {
        vector.clear()
    }
}
