package dev.winrt.sample.jvm

import dev.winrt.core.JvmWinRtObjectStub
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr
import dev.winrt.kom.HResult
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.PlatformComInterop
import microsoft.ui.xaml.controls.XamlControlsResources
import java.nio.file.Path
import microsoft.ui.xaml.Application
import microsoft.ui.xaml.ResourceDictionary

class SampleWinUiApp private constructor(
    val application: Application,
    val pointer: ComPtr,
    private val innerPointer: ComPtr,
    private val authoringStub: JvmWinRtObjectStub,
    private val metadataProviderPointer: ComPtr,
) : AutoCloseable {
    private var resourceManagerRegistration: WinUiResourceManagerSupport.Registration? = null

    fun ensureResourceManagerRegistered() {
        if (resourceManagerRegistration != null) {
            return
        }
        val currentApplication = runCatching { Application.current }.getOrNull()
        val targetApplication = currentApplication ?: application
        val runtimeRoot = System.getProperty("dev.winrt.windowsAppSdkRoot")
            ?.takeIf { it.isNotBlank() }
            ?.let(Path::of)
        if (runtimeRoot != null) {
            resourceManagerRegistration = WinUiResourceManagerSupport.register(targetApplication.pointer, runtimeRoot)
        }
    }

    fun attachControlResources() {
        ensureResourceManagerRegistered()
        val targetApplication = runCatching { Application.current }.getOrNull() ?: application

        val controlResources = runCatching { loadControlResourcesFromXaml() }
            .recoverCatching { XamlControlsResources() }
            .getOrNull()
            ?: return

        runCatching {
            targetApplication.resources = controlResources
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
        private val iidIApplicationOverrides = guidOf("a33e81ef-c665-503b-8827-d27ef1720a06")
        private val iidIXamlReaderStatics = guidOf("82a4cd9e-435e-5aeb-8c4f-300cece45cae")
        private val iidApplicationFactory = guidOf("9fd96657-5294-5a65-a1db-4fea143597da")
        private val iidApplicationDefault = guidOf("06a8f4e7-1146-55af-820d-ebd55643b021")
        private const val applicationFactoryClassId = "Microsoft.UI.Xaml.Application"
        private const val applicationRuntimeClassName = "Dev.WinRT.Sample.Jvm.App"
        private const val xamlReaderClassId = "Microsoft.UI.Xaml.Markup.XamlReader"

        fun create(onLaunched: () -> Unit = {}): SampleWinUiApp {
            val metadataProviderPointer = WinUiXamlMetadataProvider.create()
            var authoringStub: JvmWinRtObjectStub? = null
            var instancePointer = ComPtr.NULL
            var innerPointer = ComPtr.NULL
            var defaultPointer = ComPtr.NULL

            try {
                authoringStub = JvmWinRtObjectStub.createWithRuntimeClassName(
                    runtimeClassName = applicationRuntimeClassName,
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
                    JvmWinRtObjectStub.InterfaceSpec(
                        iid = iidIApplicationOverrides,
                        objectArgUnitMethods = mapOf(
                            6 to {
                                onLaunched()
                                HResult(0)
                            },
                        ),
                    ),
                )

                val factory = JvmWinRtRuntime.getActivationFactory(applicationFactoryClassId, iidApplicationFactory).getOrThrow()
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
                ).also { app ->
                    app.ensureResourceManagerRegistered()
                }
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

        private fun loadControlResourcesFromXaml(): ResourceDictionary {
            val xaml = """
                <controls:XamlControlsResources
                    xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
                    xmlns:controls="using:Microsoft.UI.Xaml.Controls" />
            """.trimIndent()
            val statics = JvmWinRtRuntime.getActivationFactory(xamlReaderClassId, iidIXamlReaderStatics).getOrThrow()
            return try {
                val loaded = PlatformComInterop.invokeObjectMethodWithStringArg(statics, 6, xaml).getOrThrow()
                ResourceDictionary(loaded)
            } finally {
                PlatformComInterop.release(statics)
            }
        }
    }
}
