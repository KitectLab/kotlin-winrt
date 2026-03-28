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
        val returnType = kotlinScalarType(invokeMethod.returnType, currentNamespace, genericParameters)
        if (returnType == null && invokeMethod.returnType !in setOf("Unit", "Boolean")) {
            return null
        }

        return when {
            invokeMethod.parameters.isEmpty() && invokeMethod.returnType == "Unit" -> {
                DelegateLambdaPlan.PlannedBridge(
                    lambdaType = LambdaTypeName.get(returnType = Unit::class.asTypeName()),
                    bridge = BridgeSpec(
                        factoryMethod = "createNoArgUnitDelegate",
                        parameterCarriers = listOf(ParameterCarrier.NoArgs),
                        returnCarrier = ReturnCarrier.UNIT,
                    ),
                )
            }
            invokeMethod.parameters.isEmpty() && invokeMethod.returnType == "Boolean" -> {
                DelegateLambdaPlan.PlannedBridge(
                    lambdaType = LambdaTypeName.get(returnType = Boolean::class.asTypeName()),
                    bridge = BridgeSpec(
                        factoryMethod = "createNoArgBooleanDelegate",
                        parameterCarriers = listOf(ParameterCarrier.NoArgs),
                        returnCarrier = ReturnCarrier.BOOLEAN,
                    ),
                )
            }
            invokeMethod.parameters.size == 1 &&
                supportsObjectType(invokeMethod.parameters.single().type) &&
                invokeMethod.returnType == "Unit" -> {
                val callbackArgType = typeNameMapper.mapTypeName(invokeMethod.parameters.single().type, currentNamespace, genericParameters)
                DelegateLambdaPlan.PlannedBridge(
                    lambdaType = LambdaTypeName.get(parameters = arrayOf(callbackArgType), returnType = Unit::class.asTypeName()),
                    bridge = BridgeSpec(
                        factoryMethod = "createObjectArgUnitDelegate",
                        parameterCarriers = listOf(ParameterCarrier.ObjectWrapped(callbackArgType)),
                        returnCarrier = ReturnCarrier.UNIT,
                    ),
                )
            }
            invokeMethod.parameters.size == 1 &&
                supportsObjectType(invokeMethod.parameters.single().type) &&
                invokeMethod.returnType == "Boolean" -> {
                val callbackArgType = typeNameMapper.mapTypeName(invokeMethod.parameters.single().type, currentNamespace, genericParameters)
                DelegateLambdaPlan.PlannedBridge(
                    lambdaType = LambdaTypeName.get(parameters = arrayOf(callbackArgType), returnType = Boolean::class.asTypeName()),
                    bridge = BridgeSpec(
                        factoryMethod = "createObjectArgBooleanDelegate",
                        parameterCarriers = listOf(ParameterCarrier.ObjectWrapped(callbackArgType)),
                        returnCarrier = ReturnCarrier.BOOLEAN,
                    ),
                )
            }
            invokeMethod.parameters.size == 1 -> resolveScalarPlan(invokeMethod.parameters.single().type, invokeMethod.returnType)
            else -> null
        }
    }

    private fun resolveScalarPlan(parameterTypeName: String, returnTypeName: String): DelegateLambdaPlan.PlannedBridge? {
        val spec = scalarBridgeSpecs[parameterTypeName] ?: return null
        return when (returnTypeName) {
            "Unit" -> spec.unitBridgeFactoryMethod?.let {
                directSingleArgPlan(spec.parameterType, Unit::class.asTypeName(), it)
            }
            "Boolean" -> spec.booleanBridgeFactoryMethod?.let {
                directSingleArgPlan(spec.parameterType, Boolean::class.asTypeName(), it)
            }
            else -> null
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
