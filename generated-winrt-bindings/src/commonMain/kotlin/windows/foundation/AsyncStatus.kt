package windows.foundation

enum class AsyncStatus(val value: Int) {
    Started(0),
    Completed(1),
    Canceled(2),
    Error(3)
}
