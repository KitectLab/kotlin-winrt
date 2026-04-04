package dev.winrt.sample.jvm

import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.PlatformComInterop

object WinUiXamlMetadataProvider {
    private val iidIActivationFactory = guidOf("00000035-0000-0000-c000-000000000046")
    private val iidXamlControlsXamlMetaDataProviderStatics = guidOf("2d7eb3fd-ecdb-5084-b7e0-12f9598381ef")
    private const val providerClassId = "Microsoft.UI.Xaml.XamlTypeInfo.XamlControlsXamlMetaDataProvider"

    @Volatile
    private var initialized = false

    fun create(): ComPtr {
        val factory = JvmWinRtRuntime.getActivationFactory(providerClassId, iidIActivationFactory).getOrThrow()
        return try {
            initialize(factory)
            JvmWinRtRuntime.activateInstance(factory).getOrThrow()
        } finally {
            PlatformComInterop.release(factory)
        }
    }

    fun getXamlType(providerPointer: ComPtr, typeNamePointer: ComPtr): ComPtr {
        return PlatformComInterop.invokeObjectMethodWithObjectArg(providerPointer, 6, typeNamePointer).getOrThrow()
    }

    fun getXamlType(providerPointer: ComPtr, fullName: String): ComPtr {
        return PlatformComInterop.invokeObjectMethodWithStringArg(providerPointer, 7, fullName).getOrThrow()
    }

    private fun initialize(factory: ComPtr) {
        if (initialized) {
            return
        }
        synchronized(this) {
            if (initialized) {
                return
            }
            val statics = PlatformComInterop.queryInterface(factory, iidXamlControlsXamlMetaDataProviderStatics).getOrThrow()
            try {
                PlatformComInterop.invokeUnitMethod(statics, 6).getOrThrow()
            } finally {
                PlatformComInterop.release(statics)
            }
            initialized = true
        }
    }
}
