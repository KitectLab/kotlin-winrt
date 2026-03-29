package windows.foundation

import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.kom.ComPtr

public open class AsyncOperationCompletedHandler<TResult>(
  pointer: ComPtr,
) : WinRtInterfaceProjection(pointer)
