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
    val parameterCarriers: List<ParameterCarrier>,
    val returnCarrier: ReturnCarrier,
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

internal enum class ReturnCarrier {
    UNIT,
    BOOLEAN,
}

private data class ScalarBridgeSpec(
    val parameterType: TypeName,
    val unitBridgeFactoryMethod: String? = null,
    val booleanBridgeFactoryMethod: String? = null,
)

private data class DelegateSignatureShape(
    val parameterCarriers: List<ParameterCarrier>,
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
            unitBridgeFactoryMethod = "createInt32ArgUnitDelegate",
            booleanBridgeFactoryMethod = "createInt32ArgBooleanDelegate",
        ),
        "String" to ScalarBridgeSpec(
            parameterType = String::class.asTypeName(),
            unitBridgeFactoryMethod = "createStringArgUnitDelegate",
            booleanBridgeFactoryMethod = "createStringArgBooleanDelegate",
        ),
        "UInt32" to ScalarBridgeSpec(
            parameterType = UInt::class.asTypeName(),
            unitBridgeFactoryMethod = "createUInt32ArgUnitDelegate",
            booleanBridgeFactoryMethod = "createUInt32ArgBooleanDelegate",
        ),
        "Boolean" to ScalarBridgeSpec(
            parameterType = Boolean::class.asTypeName(),
            unitBridgeFactoryMethod = "createBooleanArgUnitDelegate",
        ),
        "Int64" to ScalarBridgeSpec(
            parameterType = Long::class.asTypeName(),
            unitBridgeFactoryMethod = "createInt64ArgUnitDelegate",
            booleanBridgeFactoryMethod = "createInt64ArgBooleanDelegate",
        ),
        "UInt64" to ScalarBridgeSpec(
            parameterType = ULong::class.asTypeName(),
            unitBridgeFactoryMethod = "createUInt64ArgUnitDelegate",
        ),
        "Float32" to ScalarBridgeSpec(
            parameterType = Float::class.asTypeName(),
            unitBridgeFactoryMethod = "createFloat32ArgUnitDelegate",
        ),
        "Float64" to ScalarBridgeSpec(
            parameterType = Double::class.asTypeName(),
            unitBridgeFactoryMethod = "createFloat64ArgUnitDelegate",
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

        return when {
            signatureShape.parameterCarriers.singleOrNull() == ParameterCarrier.NoArgs &&
                signatureShape.returnCarrier == ReturnCarrier.UNIT -> {
                DelegateLambdaPlan.PlannedBridge(
                    lambdaType = LambdaTypeName.get(returnType = signatureShape.lambdaReturnType),
                    bridge = BridgeSpec(
                        factoryMethod = "createNoArgUnitDelegate",
                        parameterCarriers = signatureShape.parameterCarriers,
                        returnCarrier = signatureShape.returnCarrier,
                    ),
                )
            }
            signatureShape.parameterCarriers.singleOrNull() == ParameterCarrier.NoArgs &&
                signatureShape.returnCarrier == ReturnCarrier.BOOLEAN -> {
                DelegateLambdaPlan.PlannedBridge(
                    lambdaType = LambdaTypeName.get(returnType = signatureShape.lambdaReturnType),
                    bridge = BridgeSpec(
                        factoryMethod = "createNoArgBooleanDelegate",
                        parameterCarriers = signatureShape.parameterCarriers,
                        returnCarrier = signatureShape.returnCarrier,
                    ),
                )
            }
            signatureShape.parameterCarriers.singleOrNull() is ParameterCarrier.ObjectWrapped &&
                signatureShape.returnCarrier == ReturnCarrier.UNIT -> {
                DelegateLambdaPlan.PlannedBridge(
                    lambdaType = LambdaTypeName.get(
                        parameters = signatureShape.lambdaParameterTypes.toTypedArray(),
                        returnType = signatureShape.lambdaReturnType,
                    ),
                    bridge = BridgeSpec(
                        factoryMethod = "createObjectArgUnitDelegate",
                        parameterCarriers = signatureShape.parameterCarriers,
                        returnCarrier = signatureShape.returnCarrier,
                    ),
                )
            }
            signatureShape.parameterCarriers.singleOrNull() is ParameterCarrier.ObjectWrapped &&
                signatureShape.returnCarrier == ReturnCarrier.BOOLEAN -> {
                DelegateLambdaPlan.PlannedBridge(
                    lambdaType = LambdaTypeName.get(
                        parameters = signatureShape.lambdaParameterTypes.toTypedArray(),
                        returnType = signatureShape.lambdaReturnType,
                    ),
                    bridge = BridgeSpec(
                        factoryMethod = "createObjectArgBooleanDelegate",
                        parameterCarriers = signatureShape.parameterCarriers,
                        returnCarrier = signatureShape.returnCarrier,
                    ),
                )
            }
            signatureShape.parameterCarriers.size == 1 &&
                signatureShape.parameterCarriers.single() is ParameterCarrier.Direct -> {
                resolveScalarPlan(signatureShape)
            }
            else -> null
        }
    }

    private fun resolveSignatureShape(
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
        return DelegateSignatureShape(
            parameterCarriers = normalizedCarriers,
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

    private fun resolveScalarPlan(signatureShape: DelegateSignatureShape): DelegateLambdaPlan.PlannedBridge? {
        val parameterCarrier = signatureShape.parameterCarriers.single() as? ParameterCarrier.Direct ?: return null
        val parameterTypeName = scalarBridgeSpecs.entries.firstOrNull { it.value.parameterType == parameterCarrier.kotlinType }?.key ?: return null
        val spec = scalarBridgeSpecs[parameterTypeName] ?: return null
        return when (signatureShape.returnCarrier) {
            ReturnCarrier.UNIT -> spec.unitBridgeFactoryMethod?.let {
                directSingleArgPlan(spec.parameterType, signatureShape.lambdaReturnType, it)
            }
            ReturnCarrier.BOOLEAN -> spec.booleanBridgeFactoryMethod?.let {
                directSingleArgPlan(spec.parameterType, signatureShape.lambdaReturnType, it)
            }
        }
    }

    private fun directSingleArgPlan(parameterType: TypeName, returnType: TypeName, bridgeFactoryMethod: String): DelegateLambdaPlan.PlannedBridge {
        return DelegateLambdaPlan.PlannedBridge(
            lambdaType = LambdaTypeName.get(parameters = arrayOf(parameterType), returnType = returnType),
            bridge = BridgeSpec(
                factoryMethod = bridgeFactoryMethod,
                parameterCarriers = listOf(ParameterCarrier.Direct(parameterType)),
                returnCarrier = if (returnType == Boolean::class.asTypeName()) ReturnCarrier.BOOLEAN else ReturnCarrier.UNIT,
            ),
        )
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
