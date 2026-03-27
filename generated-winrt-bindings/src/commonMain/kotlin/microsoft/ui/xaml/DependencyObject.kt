package microsoft.ui.xaml

import dev.winrt.core.Inspectable
import dev.winrt.kom.ComPtr

open class DependencyObject(
    pointer: ComPtr,
) : Inspectable(pointer)
