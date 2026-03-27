package dev.winrt.sample.jvm

import dev.winrt.core.JvmWinRtObjectStub
import dev.winrt.core.guidOf
import dev.winrt.kom.ComPtr
import dev.winrt.kom.HResult
import microsoft.ui.xaml.IApplicationOverrides
import microsoft.ui.xaml.LaunchActivatedEventArgs
import org.junit.Assert.assertEquals
import org.junit.Test

class WinUiApplicationOverridesProbeTest {
    @Test
    fun application_overrides_stub_receives_on_launched() {
        val overridesIid = IApplicationOverrides.iid
        val argsIid = guidOf("aaaaaaaa-1111-2222-3333-444444444444")
        var seenArg = ComPtr.NULL

        JvmWinRtObjectStub.create(
            JvmWinRtObjectStub.InterfaceSpec(
                iid = overridesIid,
                objectArgUnitMethods = mapOf(
                    6 to { arg ->
                        seenArg = arg
                        HResult(0)
                    },
                ),
            ),
        ).use { overridesStub ->
            JvmWinRtObjectStub.create(
                JvmWinRtObjectStub.InterfaceSpec(iid = argsIid),
            ).use { argsStub ->
                val overrides = IApplicationOverrides(overridesStub.primaryPointer)
                val args = LaunchActivatedEventArgs(argsStub.primaryPointer)
                overrides.onLaunched(args)
                assertEquals(argsStub.primaryPointer.value.rawValue, seenArg.value.rawValue)
            }
        }
    }
}
