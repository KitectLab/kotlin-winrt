package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdMethod

internal sealed interface DelegateLambdaPlan {
    val lambdaType: LambdaTypeName

    data class DirectBridge(
        override val lambdaType: LambdaTypeName,
        val bridgeFactoryMethod: String,
    ) : DelegateLambdaPlan

    data class ObjectBridge(
        override val lambdaType: LambdaTypeName,
        val bridgeFactoryMethod: String,
        val callbackArgType: TypeName,
    ) : DelegateLambdaPlan
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
                DelegateLambdaPlan.DirectBridge(
                    lambdaType = LambdaTypeName.get(returnType = Unit::class.asTypeName()),
                    bridgeFactoryMethod = "createNoArgUnitDelegate",
                )
            }
            invokeMethod.parameters.isEmpty() && invokeMethod.returnType == "Boolean" -> {
                DelegateLambdaPlan.DirectBridge(
                    lambdaType = LambdaTypeName.get(returnType = Boolean::class.asTypeName()),
                    bridgeFactoryMethod = "createNoArgBooleanDelegate",
                )
            }
            invokeMethod.parameters.size == 1 -> resolveScalarPlan(invokeMethod.parameters.single().type, invokeMethod.returnType)
            invokeMethod.parameters.size == 1 &&
                supportsObjectType(invokeMethod.parameters.single().type) &&
                invokeMethod.returnType == "Unit" -> {
                val callbackArgType = typeNameMapper.mapTypeName(invokeMethod.parameters.single().type, currentNamespace, genericParameters)
                DelegateLambdaPlan.ObjectBridge(
                    lambdaType = LambdaTypeName.get(parameters = arrayOf(callbackArgType), returnType = Unit::class.asTypeName()),
                    bridgeFactoryMethod = "createObjectArgUnitDelegate",
                    callbackArgType = callbackArgType,
                )
            }
            invokeMethod.parameters.size == 1 &&
                supportsObjectType(invokeMethod.parameters.single().type) &&
                invokeMethod.returnType == "Boolean" -> {
                val callbackArgType = typeNameMapper.mapTypeName(invokeMethod.parameters.single().type, currentNamespace, genericParameters)
                DelegateLambdaPlan.ObjectBridge(
                    lambdaType = LambdaTypeName.get(parameters = arrayOf(callbackArgType), returnType = Boolean::class.asTypeName()),
                    bridgeFactoryMethod = "createObjectArgBooleanDelegate",
                    callbackArgType = callbackArgType,
                )
            }
            else -> null
        }
    }

    private fun resolveScalarPlan(parameterTypeName: String, returnTypeName: String): DelegateLambdaPlan.DirectBridge? {
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

    private fun directSingleArgPlan(parameterType: TypeName, returnType: TypeName, bridgeFactoryMethod: String): DelegateLambdaPlan.DirectBridge {
        return DelegateLambdaPlan.DirectBridge(
            lambdaType = LambdaTypeName.get(parameters = arrayOf(parameterType), returnType = returnType),
            bridgeFactoryMethod = bridgeFactoryMethod,
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
