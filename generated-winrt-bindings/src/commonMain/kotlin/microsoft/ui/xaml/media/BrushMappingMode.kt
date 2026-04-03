package microsoft.ui.xaml.media

enum class BrushMappingMode(
    val value: Int,
) {
    Absolute(0),
    RelativeToBoundingBox(1);

    companion object {
        fun fromValue(value: Int): BrushMappingMode =
            entries.first { it.value == value }
    }
}
