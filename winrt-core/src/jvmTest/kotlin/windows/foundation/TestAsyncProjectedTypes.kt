package windows.foundation

import dev.winrt.core.Inspectable
import dev.winrt.kom.ComPtr

open class IAsyncOperation<TResult>(
    pointer: ComPtr,
    val resultSignature: String,
) : Inspectable(pointer)
