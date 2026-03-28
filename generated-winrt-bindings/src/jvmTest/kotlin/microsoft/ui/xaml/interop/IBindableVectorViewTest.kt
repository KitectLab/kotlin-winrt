package microsoft.ui.xaml.interop

import dev.winrt.core.Inspectable
import dev.winrt.kom.ComPtr
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class IBindableVectorViewTest {
    @Test
    fun bindable_vector_view_implements_list() {
        val view = IBindableVectorView(ComPtr.NULL)

        val first: List<Inspectable> = view
        val second: List<Inspectable> = view

        assertSame(first, second)
    }

    @Test
    fun list_projection_rejects_negative_indices_before_com_invocation() {
        val view = IBindableVectorView(ComPtr.NULL)
        val list: List<Inspectable> = view

        val error = runCatching { list[-1] }.exceptionOrNull()

        requireNotNull(error)
        assertEquals("index must be non-negative", error.message)
    }
}
