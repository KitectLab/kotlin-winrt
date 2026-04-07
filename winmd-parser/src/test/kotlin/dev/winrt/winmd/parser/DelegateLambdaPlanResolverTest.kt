package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.TypeName
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdParameter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DelegateLambdaPlanResolverTest {
    private val resolver = DelegateLambdaPlanResolver(TypeNameMapper())

    @Test
    fun normalizes_two_scalar_parameters_before_bridge_selection() {
        val shape = resolver.resolveSignatureShape(
            invokeMethod = WinMdMethod(
                name = "Invoke",
                returnType = "Boolean",
                parameters = listOf(
                    WinMdParameter(name = "index", type = "Int32"),
                    WinMdParameter(name = "label", type = "String"),
                ),
            ),
            currentNamespace = "Example.Callbacks",
            genericParameters = emptySet(),
            supportsObjectType = { it == "Object" || it.contains('.') },
        )

        requireNotNull(shape)
        assertEquals("createBooleanDelegate", shape.returnDescriptor.factoryMethod)
        assertEquals(listOf("kotlin.Int", "kotlin.String"), shape.lambdaParameterTypes.map(TypeName::toString))
    }

    @Test
    fun normalizes_mixed_object_and_scalar_parameters_before_bridge_selection() {
        val shape = resolver.resolveSignatureShape(
            invokeMethod = WinMdMethod(
                name = "Invoke",
                returnType = "Unit",
                parameters = listOf(
                    WinMdParameter(name = "payload", type = "Example.Callbacks.Payload"),
                    WinMdParameter(name = "count", type = "UInt32"),
                ),
            ),
            currentNamespace = "Example.Callbacks",
            genericParameters = emptySet(),
            supportsObjectType = { it == "Object" || it.contains('.') },
        )

        requireNotNull(shape)
        assertEquals("createUnitDelegate", shape.returnDescriptor.factoryMethod)
        assertEquals(
            listOf("example.callbacks.Payload", "kotlin.UInt"),
            shape.lambdaParameterTypes.map(TypeName::toString),
        )
    }

    @Test
    fun normalizes_three_parameter_mixed_signature_before_bridge_selection() {
        val shape = resolver.resolveSignatureShape(
            invokeMethod = WinMdMethod(
                name = "Invoke",
                returnType = "Boolean",
                parameters = listOf(
                    WinMdParameter(name = "payload", type = "Example.Callbacks.Payload"),
                    WinMdParameter(name = "count", type = "Int32"),
                    WinMdParameter(name = "enabled", type = "Boolean"),
                ),
            ),
            currentNamespace = "Example.Callbacks",
            genericParameters = emptySet(),
            supportsObjectType = { it == "Object" || it.contains('.') },
        )

        requireNotNull(shape)
        assertEquals("createBooleanDelegate", shape.returnDescriptor.factoryMethod)
        assertEquals(
            listOf("example.callbacks.Payload", "kotlin.Int", "kotlin.Boolean"),
            shape.lambdaParameterTypes.map(TypeName::toString),
        )
    }
}
