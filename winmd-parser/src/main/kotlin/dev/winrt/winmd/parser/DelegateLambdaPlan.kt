package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
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
    val argumentKinds: List<DelegateArgumentKind>,
    val returnCarrier: ReturnCarrier,
)

internal enum class DelegateArgumentKind {
    OBJECT,
    INT32,
    UINT32,
    BOOLEAN,
    INT64,
    UINT64,
    FLOAT32,
    FLOAT64,
    STRING,
}

internal sealed interface ParameterCarrier {
    data object NoArgs : ParameterCarrier

    data class Direct(
        val kotlinType: TypeName,
    ) : ParameterCarrier

    data class ObjectWrapped(
        val callbackArgType: TypeName,
    ) : ParameterCarrier
}

internal enum class ReturnCarrier {
    UNIT,
    BOOLEAN,
}

private data class ScalarBridgeSpec(
    val parameterType: TypeName,
    val argumentKind: DelegateArgumentKind,
)

internal data class DelegateSignatureShape(
    val parameterCarriers: List<ParameterCarrier>,
    val argumentKinds: List<DelegateArgumentKind>,
    val lambdaParameterTypes: List<TypeName>,
    val returnCarrier: ReturnCarrier,
    val lambdaReturnType: TypeName,
)

internal class DelegateLambdaPlanResolver(
    private val typeNameMapper: TypeNameMapper,
) {
    private val scalarBridgeSpecs = mapOf(
        "Int32" to ScalarBridgeSpec(
            parameterType = Int::class.asTypeName(),
            argumentKind = DelegateArgumentKind.INT32,
        ),
        "String" to ScalarBridgeSpec(
            parameterType = String::class.asTypeName(),
            argumentKind = DelegateArgumentKind.STRING,
        ),
        "UInt32" to ScalarBridgeSpec(
            parameterType = UInt::class.asTypeName(),
            argumentKind = DelegateArgumentKind.UINT32,
        ),
        "Boolean" to ScalarBridgeSpec(
            parameterType = Boolean::class.asTypeName(),
            argumentKind = DelegateArgumentKind.BOOLEAN,
        ),
        "Int64" to ScalarBridgeSpec(
            parameterType = Long::class.asTypeName(),
            argumentKind = DelegateArgumentKind.INT64,
        ),
        "UInt64" to ScalarBridgeSpec(
            parameterType = ULong::class.asTypeName(),
            argumentKind = DelegateArgumentKind.UINT64,
        ),
        "Float32" to ScalarBridgeSpec(
            parameterType = Float::class.asTypeName(),
            argumentKind = DelegateArgumentKind.FLOAT32,
        ),
        "Float64" to ScalarBridgeSpec(
            parameterType = Double::class.asTypeName(),
            argumentKind = DelegateArgumentKind.FLOAT64,
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
            true -> LambdaTypeName.get(returnType = signatureShape.lambdaReturnType)
            false -> LambdaTypeName.get(
                parameters = signatureShape.lambdaParameterTypes.toTypedArray(),
                returnType = signatureShape.lambdaReturnType,
            )
        }
        return DelegateLambdaPlan.PlannedBridge(
            lambdaType = lambdaType,
            bridge = BridgeSpec(
                factoryMethod = when (signatureShape.returnCarrier) {
                    ReturnCarrier.UNIT -> "createUnitDelegate"
                    ReturnCarrier.BOOLEAN -> "createBooleanDelegate"
                },
                argumentKinds = signatureShape.argumentKinds,
                returnCarrier = signatureShape.returnCarrier,
            ),
        )
    }

    internal fun resolveSignatureShape(
        invokeMethod: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
        supportsObjectType: (String) -> Boolean,
    ): DelegateSignatureShape? {
        val returnCarrier = when (invokeMethod.returnType) {
            "Unit" -> ReturnCarrier.UNIT
            "Boolean" -> ReturnCarrier.BOOLEAN
            else -> return null
        }
        val lambdaReturnType = when (returnCarrier) {
            ReturnCarrier.UNIT -> Unit::class.asTypeName()
            ReturnCarrier.BOOLEAN -> Boolean::class.asTypeName()
        }
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
                is ParameterCarrier.ObjectWrapped -> DelegateArgumentKind.OBJECT
            }
        }
        return DelegateSignatureShape(
            parameterCarriers = normalizedCarriers,
            argumentKinds = argumentKinds,
            lambdaParameterTypes = lambdaParameterTypes,
            returnCarrier = returnCarrier,
            lambdaReturnType = lambdaReturnType,
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

    private fun kotlinScalarType(typeName: String, currentNamespace: String, genericParameters: Set<String>): TypeName? {
        return when (typeName) {
            "Unit" -> Unit::class.asTypeName()
            "String" -> String::class.asTypeName()
            "Boolean" -> Boolean::class.asTypeName()
            "Int32" -> Int::class.asTypeName()
            "UInt32" -> UInt::class.asTypeName()
            "Int64" -> Long::class.asTypeName()
            "UInt64" -> ULong::class.asTypeName()
            "Float32" -> Float::class.asTypeName()
            "Float64" -> Double::class.asTypeName()
            else -> if (typeName == "Object" || typeName.contains('.')) {
                typeNameMapper.mapTypeName(typeName, currentNamespace, genericParameters)
            } else {
                null
            }
        }
    }
}
