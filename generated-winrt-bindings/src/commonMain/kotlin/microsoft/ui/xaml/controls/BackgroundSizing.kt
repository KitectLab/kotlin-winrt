package microsoft.ui.xaml.controls

enum class BackgroundSizing(
    val value: Int,
) {
    InnerBorderEdge(0),
    OuterBorderEdge(1);

    companion object {
        fun fromValue(value: Int): BackgroundSizing =
            entries.first { it.value == value }
    }
}
