package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.core.WinRtDelegateValueKind
import dev.winrt.winmd.plugin.WinMdMethod

internal sealed interface DelegateLambdaPlan {
    val lambdaType: LambdaTypeName
    val bridge: BridgeSpec

    data class PlannedBridge(
        override val lambdaType: LambdaTypeName,
        override val bridge: BridgeSpec,
    ) : DelegateLambdaPlan
}

internal data class BridgeSpec(
    val factoryMethod: String,
    val argumentKinds: List<WinRtDelegateValueKind>,
)

internal sealed interface ParameterCarrier {
    data object NoArgs : ParameterCarrier

    data class Direct(
        val kotlinType: TypeName,
    ) : ParameterCarrier

    data class ObjectWrapped(
        val callbackArgType: TypeName,
    ) : ParameterCarrier
}

internal data class DelegateReturnDescriptor(
    val factoryMethod: String,
    val lambdaReturnType: TypeName,
)

private data class ScalarBridgeSpec(
    val parameterType: TypeName,
    val argumentKind: WinRtDelegateValueKind,
)

internal data class DelegateSignatureShape(
    val argumentKinds: List<WinRtDelegateValueKind>,
    val lambdaParameterTypes: List<TypeName>,
    val returnDescriptor: DelegateReturnDescriptor,
)

internal class DelegateLambdaPlanResolver(
    private val typeNameMapper: TypeNameMapper,
) {
    private val scalarBridgeSpecs = mapOf(
        "Int32" to ScalarBridgeSpec(
            parameterType = Int::class.asTypeName(),
            argumentKind = WinRtDelegateValueKind.INT32,
        ),
        "String" to ScalarBridgeSpec(
            parameterType = String::class.asTypeName(),
            argumentKind = WinRtDelegateValueKind.STRING,
        ),
        "UInt32" to ScalarBridgeSpec(
            parameterType = UInt::class.asTypeName(),
            argumentKind = WinRtDelegateValueKind.UINT32,
        ),
        "Boolean" to ScalarBridgeSpec(
            parameterType = Boolean::class.asTypeName(),
            argumentKind = WinRtDelegateValueKind.BOOLEAN,
        ),
        "Int64" to ScalarBridgeSpec(
            parameterType = Long::class.asTypeName(),
            argumentKind = WinRtDelegateValueKind.INT64,
        ),
        "UInt64" to ScalarBridgeSpec(
            parameterType = ULong::class.asTypeName(),
            argumentKind = WinRtDelegateValueKind.UINT64,
        ),
        "Float32" to ScalarBridgeSpec(
            parameterType = Float::class.asTypeName(),
            argumentKind = WinRtDelegateValueKind.FLOAT32,
        ),
        "Float64" to ScalarBridgeSpec(
            parameterType = Double::class.asTypeName(),
            argumentKind = WinRtDelegateValueKind.FLOAT64,
        ),
    )

    private val returnDescriptors = mapOf(
        "Unit" to DelegateReturnDescriptor(
            factoryMethod = "createUnitDelegate",
            lambdaReturnType = Unit::class.asTypeName(),
        ),
        "Boolean" to DelegateReturnDescriptor(
            factoryMethod = "createBooleanDelegate",
            lambdaReturnType = Boolean::class.asTypeName(),
        ),
    )

    fun resolve(
        invokeMethod: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String> = emptySet(),
        supportsObjectType: (String) -> Boolean,
    ): DelegateLambdaPlan? {
        val signatureShape = resolveSignatureShape(
            invokeMethod = invokeMethod,
            currentNamespace = currentNamespace,
            genericParameters = genericParameters,
            supportsObjectType = supportsObjectType,
        ) ?: return null

        val lambdaType = when (signatureShape.lambdaParameterTypes.isEmpty()) {
            true -> LambdaTypeName.get(returnType = signatureShape.returnDescriptor.lambdaReturnType)
            false -> LambdaTypeName.get(
                parameters = signatureShape.lambdaParameterTypes.toTypedArray(),
                returnType = signatureShape.returnDescriptor.lambdaReturnType,
            )
        }
        return DelegateLambdaPlan.PlannedBridge(
            lambdaType = lambdaType,
            bridge = BridgeSpec(
                factoryMethod = signatureShape.returnDescriptor.factoryMethod,
                argumentKinds = signatureShape.argumentKinds,
            ),
        )
    }

    internal fun resolveSignatureShape(
        invokeMethod: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
        supportsObjectType: (String) -> Boolean,
    ): DelegateSignatureShape? {
        val returnDescriptor = returnDescriptors[invokeMethod.returnType] ?: return null
        val carriers = invokeMethod.parameters.map { parameter ->
            resolveParameterCarrier(
                typeName = parameter.type,
                currentNamespace = currentNamespace,
                genericParameters = genericParameters,
                supportsObjectType = supportsObjectType,
            ) ?: return null
        }
        val lambdaParameterTypes = carriers.mapNotNull {
            when (it) {
                ParameterCarrier.NoArgs -> null
                is ParameterCarrier.Direct -> it.kotlinType
                is ParameterCarrier.ObjectWrapped -> it.callbackArgType
            }
        }
        val normalizedCarriers = if (carriers.isEmpty()) listOf(ParameterCarrier.NoArgs) else carriers
        val argumentKinds = carriers.mapNotNull {
            when (it) {
                ParameterCarrier.NoArgs -> null
                is ParameterCarrier.Direct -> scalarBridgeSpecs.entries.firstOrNull { entry -> entry.value.parameterType == it.kotlinType }?.value?.argumentKind
                is ParameterCarrier.ObjectWrapped -> WinRtDelegateValueKind.OBJECT
            }
        }
        return DelegateSignatureShape(
            argumentKinds = argumentKinds,
            lambdaParameterTypes = lambdaParameterTypes,
            returnDescriptor = returnDescriptor,
        )
    }

    private fun resolveParameterCarrier(
        typeName: String,
        currentNamespace: String,
        genericParameters: Set<String>,
        supportsObjectType: (String) -> Boolean,
    ): ParameterCarrier? {
        val scalar = scalarBridgeSpecs[typeName]
        if (scalar != null) {
            return ParameterCarrier.Direct(scalar.parameterType)
        }
        if (supportsObjectType(typeName)) {
            return ParameterCarrier.ObjectWrapped(
                typeNameMapper.mapTypeName(typeName, currentNamespace, genericParameters),
            )
        }
        return null
    }
}
