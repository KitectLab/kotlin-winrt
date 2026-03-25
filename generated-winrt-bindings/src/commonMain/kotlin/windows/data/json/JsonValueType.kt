package windows.data.json

enum class JsonValueType(val value: Int) {
    Null(0),
    Boolean(1),
    Number(2),
    String(3),
    Array(4),
    Object(5),
}
