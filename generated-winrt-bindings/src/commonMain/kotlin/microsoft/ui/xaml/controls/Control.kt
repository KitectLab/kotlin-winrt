package microsoft.ui.xaml.controls

import dev.winrt.kom.ComPtr
import microsoft.ui.xaml.FrameworkElement

open class Control(
    pointer: ComPtr,
) : FrameworkElement(pointer)
