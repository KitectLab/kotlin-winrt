package dev.winrt.kom

actual fun invokeGuidReceiveArrayMethod(instance: ComPtr, vtableIndex: Int): Result<Array<Guid>> =
    invokeGuidReceiveArrayMethod(instance, vtableIndex, *emptyArray<Any>())

actual fun invokeGuidReceiveArrayMethod(instance: ComPtr, vtableIndex: Int, vararg arguments: Any): Result<Array<Guid>> {
    return invokeReceiveArrayMethod(
        instance = instance,
        vtableIndex = vtableIndex,
        operation = "invokeGuidReceiveArrayMethod",
        arguments = arguments,
        emptyResult = { emptyArray() },
    ) { dataPointer, size ->
        val guidByteSize = Jdk22Foreign.structLayout(
            ComStructLayout.of(ComStructFieldKind.GUID),
        ).byteSize()
        Array(size) { index ->
            Jdk22Foreign.guidFromSegment(
                dataPointer.asSlice(index.toLong() * guidByteSize, guidByteSize),
            )
        }
    }
}
