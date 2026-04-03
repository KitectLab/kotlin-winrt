package windows.ui.xaml.interop

import dev.winrt.core.Inspectable
import dev.winrt.kom.ComPtr

open class TypeName(
    pointer: ComPtr,
) : Inspectable(pointer)
