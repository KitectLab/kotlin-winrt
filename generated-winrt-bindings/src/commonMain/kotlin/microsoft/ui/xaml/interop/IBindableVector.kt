package microsoft.ui.xaml.interop

import dev.winrt.core.Inspectable
import dev.winrt.core.UInt32
import dev.winrt.core.WinRtInterfaceMetadata
import dev.winrt.core.WinRtInterfaceProjection
import dev.winrt.core.guidOf
import dev.winrt.core.projectInterface
import dev.winrt.projection.WinRtMutableListProjection
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.PlatformComInterop

open class IBindableVector(
    pointer: ComPtr,
) : WinRtInterfaceProjection(pointer),
    MutableList<Inspectable> by createMutableListDelegate(pointer) {
    fun getAt(index: UInt32): Inspectable =
        Inspectable(PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 7, index.value).getOrThrow())

    override val size: Int
        get() = winRtSize.value.toInt()

    val winRtSize: UInt32
        get() =
        UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow())

    fun getView(): IBindableVectorView =
        IBindableVectorView(PlatformComInterop.invokeObjectMethod(pointer, 9).getOrThrow())

    fun append(value: Inspectable) {
        PlatformComInterop.invokeObjectSetter(pointer, 14, value.pointer).getOrThrow()
    }

    fun removeAtEnd() {
        PlatformComInterop.invokeUnitMethod(pointer, 15).getOrThrow()
    }

    override fun clear() {
        PlatformComInterop.invokeUnitMethod(pointer, 16).getOrThrow()
    }

    fun first(): IBindableIterator =
        IBindableIterator(PlatformComInterop.invokeObjectMethod(pointer, 6).getOrThrow())

    internal fun <T> projectMutableList(
        cacheKey: String,
        getter: (Int) -> T,
        append: (T) -> Unit,
    ): MutableList<T> =
        getOrPutHelperWrapper(cacheKey) { mutableListProjection(getter, append) }

    internal fun <T> mutableListProjection(
        getter: (Int) -> T,
        append: (T) -> Unit,
    ): MutableList<T> =
        Companion.createMutableListProjection(
            sizeProvider = { size },
            getter = getter,
            append = append,
            clearer = ::clear,
        )

    companion object : WinRtInterfaceMetadata {
        override val qualifiedName: String = "Microsoft.UI.Xaml.Interop.IBindableVector"
        override val projectionTypeKey: String = "System.Collections.IList"
        override val iid: Guid = guidOf("393de7de-6fd0-4c0d-bb71-47244a113e93")

        private fun createMutableListDelegate(pointer: ComPtr): MutableList<Inspectable> {
            val rawVector = object : WinRtInterfaceProjection(pointer) {}
            return rawVector.getOrPutHelperWrapper("kotlin.collections.MutableList") {
                createMutableListProjection(
                    sizeProvider = {
                        UInt32(PlatformComInterop.invokeUInt32Method(pointer, 8).getOrThrow()).value.toInt()
                    },
                    getter = { index: Int ->
                        Inspectable(
                            PlatformComInterop.invokeObjectMethodWithUInt32Arg(pointer, 7, index.toUInt()).getOrThrow(),
                        )
                    },
                    append = { value: Inspectable ->
                        PlatformComInterop.invokeObjectSetter(pointer, 14, value.pointer).getOrThrow()
                    },
                    clearer = {
                        PlatformComInterop.invokeUnitMethod(pointer, 16).getOrThrow()
                    },
                )
            }
        }

        internal fun <T> createMutableListProjection(
            sizeProvider: () -> Int,
            getter: (Int) -> T,
            append: (T) -> Unit,
            clearer: () -> Unit,
        ): MutableList<T> =
            WinRtMutableListProjection(
                sizeProvider = sizeProvider,
                getter = getter,
                append = append,
                clearer = clearer,
            )

        fun from(inspectable: Inspectable): IBindableVector =
            inspectable.projectInterface(this, ::IBindableVector)
    }
}
