package windows.foundation

import dev.winrt.core.Inspectable
import dev.winrt.kom.ComPtr

open class Uri(
    pointer: ComPtr,
) : Inspectable(pointer)
