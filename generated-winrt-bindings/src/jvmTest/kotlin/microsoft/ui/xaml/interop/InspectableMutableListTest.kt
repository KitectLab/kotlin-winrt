package microsoft.ui.xaml.interop

import dev.winrt.kom.ComPtr
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class InspectableMutableListTest {
    @Test
    fun mutable_list_projection_is_cached_per_bindable_vector_instance() {
        val vector = IBindableVector(ComPtr.NULL)

        val first = vector.asMutableList()
        val second = vector.asMutableList()

        assertSame(first, second)
    }

    @Test
    fun mutable_list_projection_rejects_negative_indices_before_com_invocation() {
        val vector = IBindableVector(ComPtr.NULL)
        val list = vector.asMutableList()

        val error = runCatching { list[-1] }.exceptionOrNull()

        requireNotNull(error)
        assertEquals("index must be non-negative", error.message)
    }
}
