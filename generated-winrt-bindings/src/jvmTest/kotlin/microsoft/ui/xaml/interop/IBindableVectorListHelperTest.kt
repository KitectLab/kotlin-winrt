package microsoft.ui.xaml.interop

import dev.winrt.kom.ComPtr
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class IBindableVectorListHelperTest {
    @Test
    fun list_helper_is_cached_per_bindable_vector_instance() {
        val vector = IBindableVector(ComPtr.NULL)

        val first = vector.asListHelper()
        val second = vector.asListHelper()

        assertSame(first, second)
    }

    @Test
    fun list_helper_rejects_negative_indices_before_com_invocation() {
        val vector = IBindableVector(ComPtr.NULL)
        val helper = vector.asListHelper()

        val error = runCatching { helper[-1] }.exceptionOrNull()

        requireNotNull(error)
        assertEquals("index must be non-negative", error.message)
    }
}
