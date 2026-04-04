package dev.winrt.sample.jvm

import dev.winrt.core.JvmWinRtObjectStub
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.PlatformComInterop
import microsoft.ui.xaml.Application
import microsoft.ui.xaml.controls.XamlControlsResources
import java.nio.file.Path

class SampleWinUiApp private constructor(
    val application: Application,
    val pointer: ComPtr,
    private val innerPointer: ComPtr,
    private val authoringStub: JvmWinRtObjectStub,
    private val metadataProviderPointer: ComPtr,
) : AutoCloseable {
    private var resourceManagerRegistration: WinUiResourceManagerSupport.Registration? = null

    fun attachControlResources() {
        resourceManagerRegistration?.close()
        resourceManagerRegistration = null
        val runtimeRoot = System.getProperty("dev.winrt.windowsAppSdkRoot")
            ?.takeIf { it.isNotBlank() }
            ?.let(Path::of)
        if (runtimeRoot != null) {
            resourceManagerRegistration = WinUiResourceManagerSupport.register(application, runtimeRoot)
        }

        val controlResources = runCatching { XamlControlsResources() }
            .onFailure { error ->
                println("winui: control resources create failed: ${error::class.simpleName}: ${error.message}")
            }
            .onSuccess {
                println("winui: control resources created")
            }
            .getOrNull()
            ?: return

        runCatching {
            application.resources = controlResources
        }.onFailure { error ->
            println("winui: app resources set failed: ${error::class.simpleName}: ${error.message}")
        }.onSuccess {
            println("winui: app resources set")
        }
    }

    fun releaseControlResources() {
        resourceManagerRegistration?.close()
        resourceManagerRegistration = null
    }

    override fun close() {
        releaseControlResources()
        releasePointers(
            listOf(
                application.pointer,
                innerPointer,
                pointer,
                metadataProviderPointer,
            ),
        )
        authoringStub.close()
    }

    companion object {
        private val iidIXamlMetadataProvider = guidOf("a96251f0-2214-5d53-8746-ce99a2593cd7")
        private val iidApplicationFactory = guidOf("9fd96657-5294-5a65-a1db-4fea143597da")
        private val iidApplicationDefault = guidOf("06a8f4e7-1146-55af-820d-ebd55643b021")
        private const val applicationClassId = "Microsoft.UI.Xaml.Application"

        fun create(): SampleWinUiApp {
            val metadataProviderPointer = WinUiXamlMetadataProvider.create()
            var authoringStub: JvmWinRtObjectStub? = null
            var instancePointer = ComPtr.NULL
            var innerPointer = ComPtr.NULL
            var defaultPointer = ComPtr.NULL

            try {
                authoringStub = JvmWinRtObjectStub.createWithRuntimeClassName(
                    runtimeClassName = applicationClassId,
                    JvmWinRtObjectStub.InterfaceSpec(
                        iid = iidIXamlMetadataProvider,
                        objectArgObjectMethods = mapOf(
                            6 to { typeName ->
                                WinUiXamlMetadataProvider.getXamlType(metadataProviderPointer, typeName)
                            },
                        ),
                        stringArgObjectMethods = mapOf(
                            7 to { fullName ->
                                WinUiXamlMetadataProvider.getXamlType(metadataProviderPointer, fullName)
                            },
                        ),
                    ),
                )

                val factory = JvmWinRtRuntime.getActivationFactory(applicationClassId, iidApplicationFactory).getOrThrow()
                try {
                    val composed = PlatformComInterop.invokeComposableMethod(
                        factory,
                        6,
                        authoringStub.primaryPointer,
                    ).getOrThrow()
                    instancePointer = composed.instance
                    innerPointer = composed.inner
                } finally {
                    PlatformComInterop.release(factory)
                }

                defaultPointer = PlatformComInterop.queryInterface(innerPointer, iidApplicationDefault).getOrThrow()
                authoringStub.setQueryInterfaceFallback { iid ->
                    PlatformComInterop.queryInterface(innerPointer, iid).getOrNull()
                }

                return SampleWinUiApp(
                    application = Application(defaultPointer),
                    pointer = instancePointer,
                    innerPointer = innerPointer,
                    authoringStub = authoringStub,
                    metadataProviderPointer = metadataProviderPointer,
                )
            } catch (t: Throwable) {
                releasePointers(listOf(defaultPointer, innerPointer, instancePointer, metadataProviderPointer))
                authoringStub?.close()
                throw t
            }
        }

        private fun releasePointers(pointers: List<ComPtr>) {
            val released = linkedSetOf<Long>()
            pointers.forEach { pointer ->
                if (!pointer.isNull && released.add(pointer.value.rawValue)) {
                    PlatformComInterop.release(pointer)
                }
            }
        }
    }
}
