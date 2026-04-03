package microsoft.ui.xaml.controls

enum class Orientation(
    val value: Int,
) {
    Horizontal(0),
    Vertical(1);

    companion object {
        fun fromValue(value: Int): Orientation =
            entries.first { it.value == value }
    }
}
