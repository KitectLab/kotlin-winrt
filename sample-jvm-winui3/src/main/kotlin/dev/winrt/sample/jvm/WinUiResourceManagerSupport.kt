package dev.winrt.sample.jvm

import dev.winrt.core.ParameterizedInterfaceId
import dev.winrt.core.WinRtDelegateBridge
import dev.winrt.core.WinRtDelegateHandle
import dev.winrt.core.WinRtDelegateValueKind
import dev.winrt.core.WinRtTypeSignature
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr
import dev.winrt.kom.Guid
import dev.winrt.kom.JvmWinRtRuntime
import dev.winrt.kom.PlatformComInterop
import java.nio.file.Files
import java.nio.file.Path
import microsoft.ui.xaml.Application
import windows.foundation.EventRegistrationToken

object WinUiResourceManagerSupport {
    private val iidIApplication2: Guid = guidOf("469e6d36-2e11-5b06-9e0a-c5eef0cf8f12")
    private val iidIResourceManagerRequestedEventArgs: Guid = guidOf("c35f4cf1-fcd6-5c6b-9be2-4cfaefb68b2a")
    private val iidIResourceManagerFactory: Guid = guidOf("d6acf18f-458a-535b-a5c4-ac2dc4e49099")
    private val iidIResourceManager: Guid = guidOf("ac2291ef-81be-5c99-a0ae-bcee0180b8a8")
    private const val resourceManagerClassId = "Microsoft.Windows.ApplicationModel.Resources.ResourceManager"
    private const val typedEventHandlerGuid = "9de1c534-6ae1-11e0-84e1-18a905bcc53f"
    private const val applicationClassId = "Microsoft.UI.Xaml.Application"
    private const val applicationDefaultInterfaceIid = "06a8f4e7-1146-55af-820d-ebd55643b021"
    private const val resourceManagerRequestedEventArgsClassId = "Microsoft.UI.Xaml.ResourceManagerRequestedEventArgs"
    private const val resourceManagerRequestedEventArgsDefaultInterfaceIid = "c35f4cf1-fcd6-5c6b-9be2-4cfaefb68b2a"
    private const val addResourceManagerRequestedSlot = 6
    private const val removeResourceManagerRequestedSlot = 7
    private const val setCustomResourceManagerSlot = 7

    class Registration internal constructor(
        private val applicationPointer: ComPtr,
        private val token: EventRegistrationToken,
        private val delegateHandle: WinRtDelegateHandle,
        private val resourceManagerPointer: ComPtr,
        val priPath: Path,
    ) : AutoCloseable {
        override fun close() {
            runCatching {
                val application2 = PlatformComInterop.queryInterface(applicationPointer, iidIApplication2).getOrThrow()
                try {
                    PlatformComInterop.invokeInt64Setter(
                        application2,
                        removeResourceManagerRequestedSlot,
                        token.value,
                    ).getOrThrow()
                } finally {
                    PlatformComInterop.release(application2)
                }
            }
            delegateHandle.close()
            PlatformComInterop.release(resourceManagerPointer)
        }
    }

    fun register(application: Application, runtimeRoot: Path): Registration? {
        val priPath = runtimeRoot.resolve("Microsoft.UI.Xaml.Controls.pri")
        if (!Files.isRegularFile(priPath)) {
            println("winui: custom resource manager skipped: missing ${priPath.fileName}")
            return null
        }

        val resourceManagerPointer = createResourceManager(priPath)
        return runCatching {
            val delegateHandle = WinRtDelegateBridge.createUnitDelegate(
                iid = resourceManagerRequestedHandlerIid(),
                parameterKinds = listOf(
                    WinRtDelegateValueKind.OBJECT,
                    WinRtDelegateValueKind.OBJECT,
                ),
            ) { args ->
                val eventArgs = args[1] as ComPtr
                val requestedArgs = PlatformComInterop.queryInterface(eventArgs, iidIResourceManagerRequestedEventArgs)
                    .getOrThrow()
                try {
                    PlatformComInterop.invokeObjectSetter(
                        requestedArgs,
                        setCustomResourceManagerSlot,
                        resourceManagerPointer,
                    ).getOrThrow()
                    println("winui: custom resource manager attached=${priPath.fileName}")
                } finally {
                    PlatformComInterop.release(requestedArgs)
                }
            }

            val application2 = PlatformComInterop.queryInterface(application.pointer, iidIApplication2).getOrThrow()
            val token = try {
                EventRegistrationToken(
                    PlatformComInterop.invokeInt64MethodWithObjectArg(
                        application2,
                        addResourceManagerRequestedSlot,
                        delegateHandle.pointer,
                    ).getOrThrow(),
                )
            } finally {
                PlatformComInterop.release(application2)
            }

            println("winui: custom resource manager registered=${priPath.fileName}")
            Registration(
                applicationPointer = application.pointer,
                token = token,
                delegateHandle = delegateHandle,
                resourceManagerPointer = resourceManagerPointer,
                priPath = priPath,
            )
        }.onFailure {
            PlatformComInterop.release(resourceManagerPointer)
        }.getOrElse { error ->
            println("winui: custom resource manager registration failed: ${error::class.simpleName}: ${error.message}")
            null
        }
    }

    private fun createResourceManager(priPath: Path): ComPtr {
        val factory = JvmWinRtRuntime.getActivationFactory(resourceManagerClassId, iidIResourceManagerFactory).getOrThrow()
        return try {
            val instance = PlatformComInterop.invokeObjectMethodWithStringArg(
                factory,
                6,
                priPath.toAbsolutePath().toString(),
            ).getOrThrow()
            try {
                PlatformComInterop.queryInterface(instance, iidIResourceManager).getOrThrow()
            } finally {
                PlatformComInterop.release(instance)
            }
        } finally {
            PlatformComInterop.release(factory)
        }
    }

    private fun resourceManagerRequestedHandlerIid(): Guid {
        val senderSignature = WinRtTypeSignature.runtimeClass(
            applicationClassId,
            WinRtTypeSignature.guid(applicationDefaultInterfaceIid),
        )
        val argsSignature = WinRtTypeSignature.runtimeClass(
            resourceManagerRequestedEventArgsClassId,
            WinRtTypeSignature.guid(resourceManagerRequestedEventArgsDefaultInterfaceIid),
        )
        val signature = WinRtTypeSignature.parameterizedInterface(
            typedEventHandlerGuid,
            senderSignature,
            argsSignature,
        )
        return ParameterizedInterfaceId.createFromSignature(signature)
    }
}
