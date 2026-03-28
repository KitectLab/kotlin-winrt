package microsoft.ui.xaml.interop

import dev.winrt.core.Inspectable
import dev.winrt.core.UInt32
import dev.winrt.kom.ComPtr
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class InspectableMutableListTest {
    @Test
    fun bindable_vector_implements_mutable_list() {
        val vector = IBindableVector(ComPtr.NULL)

        val first: MutableList<Inspectable> = vector
        val second: MutableList<Inspectable> = vector

        assertSame(first, second)
    }

    @Test
    fun mutable_list_projection_rejects_negative_indices_before_com_invocation() {
        val vector = IBindableVector(ComPtr.NULL)
        val list: MutableList<Inspectable> = vector

        val error = runCatching { list[-1] }.exceptionOrNull()

        requireNotNull(error)
        assertEquals("index must be non-negative", error.message)
    }

    @Test
    fun custom_typed_projection_uses_shared_bindable_vector_factory() {
        val vector = IBindableVector(ComPtr.NULL)

        val list: MutableList<UInt32> = vector.projectMutableList(
            cacheKey = "test.UInt32List",
            getter = { UInt32(it.toUInt()) },
            append = {},
        )

        assertSame(list, vector.projectMutableList("test.UInt32List", { UInt32(it.toUInt()) }, {}))
        assertEquals(UInt32(0u), list[0])
    }
}
