package microsoft.ui.xaml

import dev.winrt.kom.ComPtr

open class UIElement(
    pointer: ComPtr,
) : DependencyObject(pointer)
