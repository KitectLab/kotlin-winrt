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

internal data class DelegateReturnDescriptor(
    val factoryMethod: String,
    val lambdaReturnType: TypeName,
)

private data class DelegateParameterDescriptor(
    val lambdaParameterType: TypeName,
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
    private val scalarParameterDescriptors = mapOf(
        "Int32" to DelegateParameterDescriptor(
            lambdaParameterType = Int::class.asTypeName(),
            argumentKind = WinRtDelegateValueKind.INT32,
        ),
        "String" to DelegateParameterDescriptor(
            lambdaParameterType = String::class.asTypeName(),
            argumentKind = WinRtDelegateValueKind.STRING,
        ),
        "UInt32" to DelegateParameterDescriptor(
            lambdaParameterType = UInt::class.asTypeName(),
            argumentKind = WinRtDelegateValueKind.UINT32,
        ),
        "Boolean" to DelegateParameterDescriptor(
            lambdaParameterType = Boolean::class.asTypeName(),
            argumentKind = WinRtDelegateValueKind.BOOLEAN,
        ),
        "Int64" to DelegateParameterDescriptor(
            lambdaParameterType = Long::class.asTypeName(),
            argumentKind = WinRtDelegateValueKind.INT64,
        ),
        "UInt64" to DelegateParameterDescriptor(
            lambdaParameterType = ULong::class.asTypeName(),
            argumentKind = WinRtDelegateValueKind.UINT64,
        ),
        "Float32" to DelegateParameterDescriptor(
            lambdaParameterType = Float::class.asTypeName(),
            argumentKind = WinRtDelegateValueKind.FLOAT32,
        ),
        "Float64" to DelegateParameterDescriptor(
            lambdaParameterType = Double::class.asTypeName(),
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
        val parameterDescriptors = invokeMethod.parameters.map { parameter ->
            resolveParameterDescriptor(
                typeName = parameter.type,
                currentNamespace = currentNamespace,
                genericParameters = genericParameters,
                supportsObjectType = supportsObjectType,
            ) ?: return null
        }
        return DelegateSignatureShape(
            argumentKinds = parameterDescriptors.map(DelegateParameterDescriptor::argumentKind),
            lambdaParameterTypes = parameterDescriptors.map(DelegateParameterDescriptor::lambdaParameterType),
            returnDescriptor = returnDescriptor,
        )
    }

    private fun resolveParameterDescriptor(
        typeName: String,
        currentNamespace: String,
        genericParameters: Set<String>,
        supportsObjectType: (String) -> Boolean,
    ): DelegateParameterDescriptor? {
        scalarParameterDescriptors[typeName]?.let { return it }
        if (supportsObjectType(typeName)) {
            return DelegateParameterDescriptor(
                lambdaParameterType = typeNameMapper.mapTypeName(typeName, currentNamespace, genericParameters),
                argumentKind = WinRtDelegateValueKind.OBJECT,
            )
        }
        return null
    }
}
