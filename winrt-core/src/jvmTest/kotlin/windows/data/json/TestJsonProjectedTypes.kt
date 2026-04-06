package windows.data.json

import dev.winrt.core.Inspectable
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr

interface IJsonValue {
    companion object {
        val iid = guidOf("a3219ecb-f0b3-4dcd-beee-19d48cd3ed1e")

        fun from(inspectable: Inspectable): IJsonValue = IJsonValueProjection(inspectable.pointer)
    }
}

open class JsonValue(
    pointer: ComPtr,
) : Inspectable(pointer)

private class IJsonValueProjection(
    pointer: ComPtr,
) : Inspectable(pointer),
    IJsonValue
